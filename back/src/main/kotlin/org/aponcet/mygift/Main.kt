package org.aponcet.mygift

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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
        val file = File(jks.path)
        keystore.load(FileInputStream(file), jks.jksPassword.toCharArray())
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
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Delete)
        method(HttpMethod.Put)
        method(HttpMethod.Patch)
        header("Authorization")
        anyHost()
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }
    install(Compression)
    install(ContentNegotiation) {
        gson { setPrettyPrinting() }
    }

    install(StatusPages) {
        exception<NotANumberException> { e ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorAnswer("Provided ${e.target} must be a number.")
            )
        }
    }

    install(Authentication) {
        jwt {
            verifier { makeVerifier(publicKeyManager) }
            validate { JWTPrincipal(it.payload) }
        }
    }
    if (!debug) install(HttpsRedirect)

    routing {
        users(userManager)
        gifts(userManager)
        buyList(userManager)
        categories(userManager)
        friendRequest(userManager)
        files(data)
        password(userManager)
        static()
    }
}