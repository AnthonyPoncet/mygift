package org.aponcet.mygift.routes

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.aponcet.mygift.*
import org.aponcet.mygift.dbmanager.Status
import java.util.*

fun Route.gifts(userManager: UserManager) {
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
        }
    }
}

private fun getGiftId(parameters: Parameters): Long {
    return parameters["gid"]!!.toLongOrNull() ?: throw NotANumberException("Gift id")
}