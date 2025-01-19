package org.aponcet.mygift.routes

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.aponcet.mygift.*
import java.util.*

fun Route.categories(userManager: UserManager) {
    authenticate {
        route("/categories") {
            put {
                handle(call) { id ->
                    val category = Gson().fromJson(call.receiveText(), RestCategory::class.java)
                    if (category.name == null || category.share == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorAnswer("Invalid category json"))
                    } else {
                        userManager.addCategory(id, category.name, category.share)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
            patch("/{cid}") {
                val cid = call.parameters["cid"]!!.toLongOrNull() ?: throw NotANumberException("Category id")
                handle(call) { id ->
                    val category = Gson().fromJson(call.receiveText(), RestCategory::class.java)
                    if (category.name == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorAnswer("Invalid category json"))
                    } else {
                        userManager.modifyCategory(id, cid, category.name)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
            delete("/{cid}") {
                val cid = call.parameters["cid"]!!.toLongOrNull() ?: throw NotANumberException("Category id")
                handle(call) { id ->
                    userManager.removeCategory(id, cid)
                    call.respond(HttpStatusCode.Accepted)
                }
            }
            post("/{cid}/rank-actions/{action}") {
                val cid = call.parameters["cid"]!!.toLongOrNull() ?: throw NotANumberException("Category id")
                handle(call) { id ->
                    val action = call.parameters["action"] ?: throw IllegalStateException("Missing action")
                    if (action != "down" && action != "up") {
                        throw IllegalStateException("Only allowed action are up or down")
                    }
                    userManager.changeCategoryRank(id, cid, RankAction.valueOf(action.uppercase(Locale.getDefault())))
                    call.respond(HttpStatusCode.Accepted)
                }
            }
        }
    }
}