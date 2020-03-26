package org.aponcet.authserver

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import com.xenomachina.argparser.ArgParser
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
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class InvalidCredentialsException(message: String) : RuntimeException(message)
data class UserJson(val name: String?, val password: String?)
data class ErrorResponse(val error: String)
data class TokenResponse(val token: String)
data class KeyResponse(val key: ByteArray)

open class SimpleJWT(val publicKey: RSAPublicKey, secretKey: RSAPrivateKey) {
    private val algorithm = Algorithm.RSA256(publicKey, secretKey)
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}

fun main(args: Array<String>) {
    val arguments = ArgParser(args).parseInto(::ArgumentParser)

    val env = applicationEngineEnvironment {
        module {
            authModule(DbUserProvider(arguments.db))
        }
        connector {
            host = "127.0.0.1"
            port = arguments.port
        }
    }

    embeddedServer(Netty, env).start(true)
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
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(exception.message ?: ""))
        }
        exception<IllegalArgumentException> { exception ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(exception.message ?: ""))
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

            call.respond(TokenResponse(simpleJwt.sign(user.name)))
        }

        get("/public-key") {
            call.respond(KeyResponse(simpleJwt.publicKey.encoded))
        }
    }
}


fun decode(input: String) : String {
    return input.toByteArray(StandardCharsets.ISO_8859_1).toString(StandardCharsets.UTF_8)
}
