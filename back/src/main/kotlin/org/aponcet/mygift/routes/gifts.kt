package org.aponcet.mygift.routes

import amazon
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.aponcet.mygift.*
import org.aponcet.mygift.dbmanager.Status
import org.aponcet.mygift.model.Data
import java.io.File
import java.net.URL
import java.util.*

enum class Source { Amazon }
data class RestScrap(val source: Source?, val url: String?)

fun Route.gifts(userManager: UserManager, data: Data) {
    authenticate {
        route("gifts") {
            get {
                handle(call) { id ->
                    val userGifts = userManager.getUserGifts(id)
                    call.respond(HttpStatusCode.OK, userGifts)
                }
            }
            put {
                handle(call) { id ->
                    val gift = Gson().fromJson(call.receiveText(), RestGift::class.java)
                    if (gift.name == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorAnswer("missing name node in json"))
                    } else {
                        val forUser =
                            call.request.queryParameters["forUser"] /* being the name of the user --> add a secret gift*/
                        if (forUser == null) {
                            userManager.addGift(id, gift)
                        } else {
                            userManager.addSecretGift(id, forUser, gift)
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
            patch("/{gid}") {
                val gid = getGiftId(call.parameters)
                handle(call) { id ->
                    val gift = Gson().fromJson(call.receiveText(), RestGift::class.java)
                    if (gift.name == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorAnswer("missing name node in json"))
                    } else {
                        userManager.modifyGift(id, gid, gift)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
            delete("/{gid}") {
                val gid = getGiftId(call.parameters)
                handle(call) { id ->
                    val status = Status.valueOf(
                        call.request.queryParameters["status"]
                            ?: throw Exception("status query parameter is mandatory")
                    )
                    userManager.removeGift(id, gid, status)
                    call.respond(HttpStatusCode.Accepted)
                }
            }

            post("/{gid}/rank-actions/{action}") {
                val gid = getGiftId(call.parameters)
                val action = call.parameters["action"] ?: throw IllegalStateException("Missing action")
                if (action != "down" && action != "up") {
                    throw IllegalStateException("Only allowed action are up or down")
                }
                handle(call) { id ->
                    userManager.changeGiftRank(id, gid, RankAction.valueOf(action.uppercase(Locale.getDefault())))
                    call.respond(HttpStatusCode.Accepted)
                }
            }

            post("/{gid}/heart/{action}") {
                val gid = getGiftId(call.parameters)
                val action = call.parameters["action"] ?: throw IllegalStateException("Missing action")
                if (action != "like" && action != "unlike") {
                    throw IllegalStateException("Only allowed action are like or unlike")
                }
                handle(call) { id ->
                    userManager.changeGiftHeart(id, gid, HeartAction.valueOf(action.uppercase(Locale.getDefault())))
                    call.respond(HttpStatusCode.Accepted)
                }
            }

            get("/{friendName}") {
                handle(call) { id ->
                    val friendName = call.parameters["friendName"]!!
                    val userGifts = userManager.getFriendGifts(id, friendName)
                    call.respond(HttpStatusCode.OK, userGifts)
                }
            }

            post("/{gid}/reserve") {
                val gid = getGiftId(call.parameters)
                handle(call) { id ->
                    userManager.changeReserve(gid, id, true)
                    call.respond(HttpStatusCode.Accepted)
                }
            }
            delete("/{gid}/reserve") {
                val gid = getGiftId(call.parameters)
                handle(call) { id ->
                    userManager.changeReserve(gid, id, false)
                    call.respond(HttpStatusCode.Accepted)
                }
            }

            post("/scrap") {
                val json = call.receiveText()

                val scrap = Gson().fromJson(json, RestScrap::class.java)
                if (scrap.url == null || scrap.source == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }

                if (!scrap.url.contains("amazon")) {
                    call.respond(HttpStatusCode.NotImplemented)
                    return@post
                }
                
                val gift = amazon(scrap.url)

                val url = URL(gift.image)
                val ext = gift.image.split(".").last()
                val fileName = "upload-${System.currentTimeMillis()}-${gift.image.hashCode()}.$ext"
                val file = File(data.uploads, fileName)
                url.openStream().use { input -> file.outputStream().buffered().use { output -> input.copyTo(output) } }

                gift.image = fileName

                call.respond(HttpStatusCode.OK, gift)
            }
        }
    }
}

private fun getGiftId(parameters: Parameters): Long {
    return parameters["gid"]!!.toLongOrNull() ?: throw NotANumberException("Gift id")
}