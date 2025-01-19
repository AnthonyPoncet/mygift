package org.aponcet.mygift.routes

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.aponcet.authserver.UserAndPictureJson
import org.aponcet.authserver.UserJson
import org.aponcet.mygift.*

fun Route.users(userManager: UserManager, debug: Boolean) {
    /** Connect **/
    route("/user") {
        post("/connect") {
            val userJson = Gson().fromJson(call.receiveText(), UserJson::class.java)

            try {
                val user = userManager.connect(userJson)
                call.sessions.set(Session(user.session))
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
        authenticate {
            get("/logout") {
                handle(call) { id ->
                    val session = call.sessions.get<Session>()
                    if (session == null) {
                        call.respond(HttpStatusCode.Accepted)
                    } else {
                        try {
                            userManager.deleteSession(session.session, id)
                        } catch (e: BadParamException) {
                            call.application.environment.log.error("Got an error while logging out", e)
                        }
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
            }

            post("/change-account") {
                var userJson = Gson().fromJson(call.receiveText(), UserJson::class.java)

                if (!debug) {
                    userJson = userJson.copy(session = call.sessions.get<Session>()!!.session)
                }

                try {
                    val user = userManager.connect(userJson)
                    call.sessions.set(Session(user.session))
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
    }

    /** Create user **/
    route("/users") {
        put {
            val basicUserInformation = Gson().fromJson(call.receiveText(), UserAndPictureJson::class.java)
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
                    val userModification = Gson().fromJson(call.receiveText(), UserModification::class.java)
                    try {
                        val user = userManager.modifyUser(id, userModification)
                        call.respond(HttpStatusCode.Accepted, user)
                    } catch (e: BadParamException) {
                        call.respond(HttpStatusCode.BadRequest, ErrorAnswer(e.error))
                    } catch (e: UpdateUserException) {
                        call.respond(HttpStatusCode.Conflict, ErrorAnswer(e.error))
                    }
                }
            }

            get("session") {
                handle(call) { id ->
                    try {
                        val session = call.sessions.get<Session>()!!.session
                        val user = userManager.getUsersOfSession(id, session)
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