package org.aponcet.mygift.routes

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.aponcet.mygift.NotANumberException
import org.aponcet.mygift.UserManager

fun Route.buyList(userManager: UserManager) {
    authenticate {
        get("/buy-list") {
            handle(call) { id ->
                val buyList = userManager.getBuyList(id)
                call.respond(HttpStatusCode.OK, buyList)
            }
        }
        delete("/buy-list/deleted-gifts/{gid}") {
            val gid = call.parameters["gid"]!!.toLongOrNull() ?: throw NotANumberException("Gift id")
            handle(call) { id ->
                userManager.deleteDeletedGift(gid, id)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}