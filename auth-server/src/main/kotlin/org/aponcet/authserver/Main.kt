package org.aponcet.authserver

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class InvalidCredentialsException(message: String) : RuntimeException(message)
data class UserJson(val name: String?, val password: String?)

open class SimpleJWT(val publicKey: RSAPublicKey, secretKey: RSAPrivateKey) {
    private val algorithm = Algorithm.RSA256(publicKey, secretKey)
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}

fun Application.authModule(userProvider: UserProvider) {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    val generateKeyPair = keyPairGenerator.generateKeyPair()
    val simpleJwt = SimpleJWT(
        generateKeyPair.public as RSAPublicKey,
        generateKeyPair.private as RSAPrivateKey
    )

    install(ContentNegotiation){
        gson { setPrettyPrinting() }
    }
    install(StatusPages) {
        exception<InvalidCredentialsException> { exception ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }
        exception<IllegalArgumentException> { exception ->
            call.respond(HttpStatusCode.BadRequest, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }
    }

    routing {
        post("/login") {
            val jsonUser = Gson().fromJson(decode(call.receiveText()), UserJson::class.java)
                ?: throw IllegalArgumentException("/login need a json as input")

            if (jsonUser.name == null) throw IllegalArgumentException("/login json need name node")
            if (jsonUser.password == null) throw IllegalArgumentException("/login json need password node")

            val user = userProvider.getUser(jsonUser.name) ?: throw InvalidCredentialsException(
                "Unknown user ${jsonUser.name}"
            )
            if (user.password != jsonUser.password) {
                throw InvalidCredentialsException("Wrong password")
            }

            call.respond(mapOf("token" to simpleJwt.sign(user.name)))
        }

        get("/public-key") {
            call.respond(mapOf("key" to simpleJwt.publicKey.encoded))
        }
    }
}


fun decode(input: String) : String {
    return input.toByteArray(StandardCharsets.ISO_8859_1).toString(StandardCharsets.UTF_8)
}
