package org.aponcet.mygift.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.aponcet.mygift.ErrorAnswer

class MalformedPayloadException : Exception()

suspend fun handle(call: ApplicationCall, function: suspend (Long) -> Unit) {
    val payload = (call.authentication.principal as JWTPrincipal).payload
    val id = payload.getClaim("id").asLong() ?: throw MalformedPayloadException()
    try {
        function(id)
    } catch (e: Exception) {
        call.application.environment.log.error("Error while handling request parameter parsing", e)
        call.respond(HttpStatusCode.InternalServerError, ErrorAnswer(e.message ?: "Unknown error"))
    }
}