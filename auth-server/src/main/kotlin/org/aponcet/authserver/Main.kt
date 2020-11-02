package org.aponcet.authserver

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.routing.post
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.aponcet.mygift.dbmanager.DbException
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class InvalidCredentialsException(message: String) : RuntimeException(message)

data class UserJson(val name: String?, val password: String?)
data class UserAndPictureJson(val name: String?, val password: String?, val picture: String?)
data class ErrorResponse(val error: String)
data class TokenResponse(val token: String)
data class KeyResponse(val key: ByteArray)

open class SimpleJWT(val publicKey: RSAPublicKey, secretKey: RSAPrivateKey) {
    private val algorithm = Algorithm.RSA256(publicKey, secretKey)
    fun sign(id: Long, name: String): String = JWT.create()
        .withClaim("id", id)
        .withClaim("name", name)
        .sign(algorithm)
}

fun main(args: Array<String>) {
    mainBody {
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
        put("/create") {
            val userAndPictureJson = Gson().fromJson(call.receiveText(), UserAndPictureJson::class.java)
                ?: throw IllegalArgumentException("/create need a json as input")

            if (userAndPictureJson.name == null) throw IllegalArgumentException("/create json need name node")
            if (userAndPictureJson.password == null) throw IllegalArgumentException("/create json need password node")
            if (userProvider.getUser(userAndPictureJson.name) != null) {
                call.respond(HttpStatusCode.Conflict, Error("User already exists"))
                return@put
            }

            try {
                val encodedPasswordAndSalt = PasswordManager.generateEncodedPassword(userAndPictureJson.password)
                userProvider.addUser(userAndPictureJson.name, encodedPasswordAndSalt.encodedPassword, encodedPasswordAndSalt.salt, userAndPictureJson.picture?:"")
                call.respond(HttpStatusCode.Created)
            } catch (e: DbException) {
                call.respond(HttpStatusCode.BadRequest, Error(e.message))
            } catch (e: Exception) {
                System.err.println(e)
                call.respond(HttpStatusCode.InternalServerError, Error(e.message ?: "Unknown error"))
            }
        }

        post("/login") {
            val jsonUser = Gson().fromJson(call.receiveText(), UserJson::class.java)
                ?: throw IllegalArgumentException("/login need a json as input")

            if (jsonUser.name == null) throw IllegalArgumentException("/login json need name node")
            if (jsonUser.password == null) throw IllegalArgumentException("/login json need password node")

            val user = userProvider.getUser(jsonUser.name) ?: throw InvalidCredentialsException("Unknown user '${jsonUser.name}' or password mismatch")
            if (!PasswordManager.isPasswordOk(jsonUser.password, user.encodedPasswordAndSalt.salt, user.encodedPasswordAndSalt.encodedPassword)) {
                throw InvalidCredentialsException("Wrong password")
            }

            call.respond(TokenResponse(simpleJwt.sign(user.id, user.name)))
        }

        get("/public-key") {
            call.respond(KeyResponse(simpleJwt.publicKey.encoded))
        }
    }
}

