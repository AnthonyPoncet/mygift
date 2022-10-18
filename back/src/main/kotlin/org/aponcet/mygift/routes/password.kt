package org.aponcet.mygift.routes

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.aponcet.authserver.UserAndResetPassword
import org.aponcet.mygift.ErrorAnswer
import org.aponcet.mygift.UserManager

fun Route.password(userManager: UserManager) {
    route("passwords/reset/{uuid}") {
        get {
            val uuid = call.parameters["uuid"]!!
            try {
                userManager.getEntry(uuid)
                call.respond(HttpStatusCode.Found)
            } catch (e: Exception) {
                call.application.environment.log.error("Error while getting password reset uuid", e)
                call.respond(HttpStatusCode.InternalServerError, ErrorAnswer(e.message ?: "Unknown error"))
            }
        }
        post {
            try {
                val uuid = call.parameters["uuid"]!!
                val userAndResetPassword =
                    Gson().fromJson(call.receiveText(), UserAndResetPassword::class.java)
                userManager.modifyPassword(userAndResetPassword, uuid)
                call.respond(HttpStatusCode.Accepted)
            } catch (e: Exception) {
                call.application.environment.log.error("Error while resetting password", e)
                call.respond(HttpStatusCode.InternalServerError, ErrorAnswer(e.message ?: "Unknown error"))
            }
        }
    }
}