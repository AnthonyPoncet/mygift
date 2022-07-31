package org.aponcet.authserver

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.aponcet.mygift.dbmanager.DbException
import org.aponcet.mygift.model.ConfigurationLoader
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class InvalidCredentialsException(message: String) : RuntimeException(message)

data class UserJson(val name: String?, val password: String?)
data class UserAndPictureJson(val name: String?, val password: String?, val picture: String?)
data class UserAndResetPassword(val name: String?, val password: String?)
data class ErrorResponse(val error: String)
data class TokenResponse(val token: String)
data class KeyResponse(val key: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyResponse) return false

        if (!key.contentEquals(other.key)) return false

        return true
    }

    override fun hashCode(): Int {
        return key.contentHashCode()
    }
}

open class SimpleJWT(val publicKey: RSAPublicKey, secretKey: RSAPrivateKey) {
    private val algorithm = Algorithm.RSA256(publicKey, secretKey)
    fun sign(id: Long, name: String): String = JWT.create()
        .withClaim("id", id)
        .withClaim("name", name)
        .sign(algorithm)
}

fun main(args: Array<String>) {
    val arguments = ArgumentParser.parse(args)
    val configuration = ConfigurationLoader.load(arguments.configurationFile)

    val env = applicationEngineEnvironment {
        module {
            authModule(DbUserProvider(configuration.data.database))
        }
        connector {
            host = configuration.authServer.host
            port = configuration.authServer.port
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

    install(ContentNegotiation) {
        gson { setPrettyPrinting() }
    }
    install(StatusPages) {
        exception<InvalidCredentialsException> { call, exception ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(exception.message ?: ""))
        }
        exception<IllegalArgumentException> { call, exception ->
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
                call.application.environment.log.error("A user named ${userAndPictureJson.name} already exists.")
                call.respond(HttpStatusCode.Conflict, ErrorResponse("User already exists"))
                return@put
            }

            try {
                val encodedPasswordAndSalt = PasswordManager.generateEncodedPassword(userAndPictureJson.password)
                userProvider.addUser(
                    userAndPictureJson.name,
                    encodedPasswordAndSalt.encodedPassword,
                    encodedPasswordAndSalt.salt,
                    userAndPictureJson.picture ?: ""
                )
                call.respond(HttpStatusCode.Created)
            } catch (e: DbException) {
                call.application.environment.log.error("database exception while creating user", e)
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: ""))
            } catch (e: Exception) {
                call.application.environment.log.error("Unknown exception while creating user", e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unknown error"))
            }
        }

        post("/update") {
            val userAndResetPassword = Gson().fromJson(call.receiveText(), UserAndResetPassword::class.java)
                ?: throw IllegalArgumentException("/update need a json as input")

            if (userAndResetPassword.name == null) throw IllegalArgumentException("/update json need name node")
            if (userAndResetPassword.password == null) throw IllegalArgumentException("/update json need password node")

            try {
                val encodedPasswordAndSalt = PasswordManager.generateEncodedPassword(userAndResetPassword.password)
                userProvider.modifyUser(
                    userAndResetPassword.name,
                    encodedPasswordAndSalt.encodedPassword,
                    encodedPasswordAndSalt.salt
                )
                call.respond(HttpStatusCode.OK)
            } catch (e: DbException) {
                call.application.environment.log.error("database exception while updating user", e)
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: ""))
            } catch (e: Exception) {
                call.application.environment.log.error("Unknown exception while updating user", e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unknown error"))
            }
        }

        post("/login") {
            val jsonUser = Gson().fromJson(call.receiveText(), UserJson::class.java)
                ?: throw IllegalArgumentException("/login need a json as input")

            if (jsonUser.name == null) throw IllegalArgumentException("/login json need name node")
            if (jsonUser.password == null) throw IllegalArgumentException("/login json need password node")

            val user = userProvider.getUser(jsonUser.name)
                ?: throw InvalidCredentialsException("Unknown user '${jsonUser.name}' or password mismatch")
            if (!PasswordManager.isPasswordOk(
                    jsonUser.password,
                    user.encodedPasswordAndSalt.salt,
                    user.encodedPasswordAndSalt.encodedPassword
                )
            ) {
                throw InvalidCredentialsException("Wrong password")
            }

            val message = TokenResponse(simpleJwt.sign(user.id, user.name))
            call.respond(HttpStatusCode.OK, message)
        }

        get("/public-key") {
            call.respond(HttpStatusCode.OK, KeyResponse(simpleJwt.publicKey.encoded))
        }
    }
}

