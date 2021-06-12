package org.aponcet.mygift.routes

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.aponcet.authserver.UserAndPictureJson
import org.aponcet.authserver.UserJson
import org.aponcet.mygift.*

fun Route.users(userManager: UserManager) {
    /** Connect **/
    route("/user") {
        post("/connect") {
            val userJson = Gson().fromJson(decode(call.receiveText()), UserJson::class.java)

            try {
                val user = userManager.connect(userJson)
                call.respond(HttpStatusCode.OK, user)
            } catch (e: BadParamException) {
                call.respond(HttpStatusCode.BadRequest, ErrorAnswer(e.error))
            } catch (e: ConnectionException) {
                call.respond(HttpStatusCode.Unauthorized, ErrorAnswer(e.error))
            } catch (e: Exception) {
                call.application.environment.log.error("Error while connecting user", e)
                call.respond(HttpStatusCode.InternalServerError, ErrorAnswer(e.message ?: "Unknown error"))
            }
        }
    }

    /** Create user **/
    route("/users") {
        put {
            val basicUserInformation = Gson().fromJson(decode(call.receiveText()), UserAndPictureJson::class.java)
            try {
                val user = userManager.addUser(basicUserInformation)
                call.respond(HttpStatusCode.Created, user)
            } catch (e: BadParamException) {
                call.respond(HttpStatusCode.BadRequest, ErrorAnswer(e.message!!))
            } catch (e: CreateUserException) {
                call.respond(HttpStatusCode.Conflict, ErrorAnswer(e.message!!))
            } catch (e: Exception) {
                call.application.environment.log.error("Error while creating user", e)
                call.respond(HttpStatusCode.InternalServerError, ErrorAnswer(e.message ?: "Unknown error"))
            }
        }
    }

    /** Update user **/
    authenticate {
        route("/users") {
            patch {
                handle(call) { id ->
                    val info = Gson().fromJson(decode(call.receiveText()), UserModification::class.java)
                    try {
                        val user = userManager.modifyUser(id, info)
                        call.respond(HttpStatusCode.Accepted, user)
                    } catch (e: BadParamException) {
                        call.respond(HttpStatusCode.BadRequest, ErrorAnswer(e.error))
                    } catch (e: CreateUserException) {
                        call.respond(HttpStatusCode.Conflict, ErrorAnswer(e.error))
                    }
                }
            }
        }
    }
}