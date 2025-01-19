package org.aponcet.mygift

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.aponcet.mygift.dbmanager.DatabaseManager
import org.aponcet.mygift.dbmanager.maintenance.AdaptTable
import org.aponcet.mygift.dbmanager.maintenance.CleanDataNotUsed
import org.aponcet.mygift.model.ConfigurationLoader
import org.aponcet.mygift.model.Data
import org.aponcet.mygift.routes.*
import java.io.File
import java.io.FileInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec


data class ErrorAnswer(val error: String)
data class NotANumberException(val target: String) : Exception()

fun makeVerifier(publicKeyManager: PublicKeyManager): JWTVerifier {
    val publicKey = publicKeyManager.publicKey
        ?: throw IllegalStateException("Authentication server may be down, please contact admin")

    val keyFactory = KeyFactory.getInstance("RSA")
    val key = keyFactory.generatePublic(X509EncodedKeySpec(publicKey)) as RSAPublicKey
    return JWT
        .require(Algorithm.RSA256(key, null))
        .build()
}

data class Session(val session: String)

fun main(args: Array<String>) {
    val parse = ArgumentParser.parse(args)
    val configuration = ConfigurationLoader.load(parse.configurationFile)

    if (parse.adaptTable != null) {
        val adaptTable = AdaptTable(configuration.data.database)
        adaptTable.execute(parse.adaptTable)
        return
    }

    if (parse.cleanData != null) {
        val cleanDataNotUsed = CleanDataNotUsed(configuration.data.database, configuration.data.uploads)
        cleanDataNotUsed.execute(parse.cleanData)
        return
    }

    val databaseManager = DatabaseManager(configuration.data.database)
    val userManager = UserManager(databaseManager, configuration.authServer)
    val publicKeyManager = PublicKeyManager(configuration.authServer)

    val uploadsDir = File(configuration.data.uploads)
    if (!uploadsDir.exists()) {
        uploadsDir.mkdir()
    }
    val tmpDir = File(configuration.data.tmp)
    if (!tmpDir.exists()) {
        tmpDir.mkdir()
    }

    publicKeyManager.start()

    if (configuration.mainServer.debug) {
        val env = applicationEngineEnvironment {
            module {
                mygift(userManager, publicKeyManager, configuration.mainServer.debug, configuration.data)
            }
            connector {
                port = configuration.mainServer.httpPort
            }
        }
        embeddedServer(Netty, env).start(true)

    } else {
        val keystore = KeyStore.getInstance("jks")
        val jks = configuration.mainServer.jks
        keystore.load(FileInputStream(File(jks.path)), jks.jksPassword.toCharArray())
        val env = applicationEngineEnvironment {
            module {
                mygift(userManager, publicKeyManager, configuration.mainServer.debug, configuration.data)
            }
            connector {
                port = configuration.mainServer.httpPort
            }
            sslConnector(
                keyStore = keystore,
                keyAlias = jks.keyAlias,
                keyStorePassword = { jks.jksPassword.toCharArray() },
                privateKeyPassword = { jks.aliasKey.toCharArray() }) {
                port = configuration.mainServer.httpsPort
                keyAlias = jks.keyAlias
            }
        }
        embeddedServer(Netty, env).start(true)
    }

}

fun Application.mygift(userManager: UserManager, publicKeyManager: PublicKeyManager, debug: Boolean, data: Data) {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        if (!debug) {
            allowHeader(HttpHeaders.AccessControlAllowOrigin)
            allowHost("www.druponps.fr", listOf("https"))
        } else {
            allowHost("0.0.0.0:5173", listOf("http"))
            allowHost("localhost:5173", listOf("http"))
            allowHost("localhost:8080", listOf("http"))
        }
    }
    install(Compression)
    install(ContentNegotiation) {
        gson { setPrettyPrinting() }
    }

    install(StatusPages) {
        exception<NotANumberException> { call, e ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorAnswer("Provided ${e.target} must be a number.")
            )
        }
    }

    install(Sessions) {
        cookie<Session>("token_session") {
            cookie.httpOnly = true
            if (!debug) {
                cookie.extensions["SameSite"] = "Strict"
            }
        }
    }

    install(Authentication) {
        jwt {
            verifier { makeVerifier(publicKeyManager) }
            validate {
                if (!debug) {
                    val session = this.sessions.get<Session>() ?: return@validate null

                    if (session.session != it.payload.getClaim("session").asString()) {
                        this.application.environment.log.error(
                            "Session does not match. Got in cookie ${session.session} against ${
                                it.payload.getClaim(
                                    "session"
                                )
                            }"
                        )
                        return@validate null
                    }
                }

                JWTPrincipal(it.payload)
            }
        }
    }

    if (!debug) install(HttpsRedirect)

    install(CallLogging)

    routing {
        buyList(userManager)
        categories(userManager)
        events(userManager)
        files(data)
        friendRequest(userManager)
        gifts(userManager, data)
        password(userManager)
        users(userManager, debug)
        static()
    }
}