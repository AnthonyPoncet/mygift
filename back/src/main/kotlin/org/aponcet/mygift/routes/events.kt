package org.aponcet.mygift.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.aponcet.mygift.UserManager

fun Route.events(userManager: UserManager) {
    authenticate {
        get("/events") {
            handle(call) { userId ->
                val events = userManager.getEvents(userId)
                call.respond(HttpStatusCode.OK, events)
            }
        }
    }
}