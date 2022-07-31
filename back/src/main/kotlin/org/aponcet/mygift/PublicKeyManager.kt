package org.aponcet.mygift

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.aponcet.authserver.KeyResponse
import org.aponcet.mygift.model.AuthServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PublicKeyManager(private val authServer: AuthServer) {

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val client = HttpClient(Apache)

    var publicKey: ByteArray? = null

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(PublicKeyManager::class.java)

        const val DELAY_NO_TOKEN = 10L
        const val DELAY_TOKEN = 43200L //each 12h
    }

    fun start() {
        val r = Runnable { loop() }
        executor.schedule(r, 0, TimeUnit.SECONDS)
    }

    private fun loop() {
        GlobalScope.launch {
            val r = Runnable { loop() }
            try {
                val httpResponse = client.get("http://${authServer.host}:${authServer.port}/public-key")
                executor.schedule(r, DELAY_TOKEN, TimeUnit.SECONDS)
                publicKey = Gson().fromJson(httpResponse.bodyAsText(), KeyResponse::class.java).key
                LOGGER.info(
                    "${
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC))
                            .format(Instant.now())
                    } - Pubic key retrieved!"
                )
            } catch (e: ResponseException) {
                LOGGER.warn("Unable to retrieve pubic key", e)
                executor.schedule(r, DELAY_NO_TOKEN, TimeUnit.SECONDS)
            } catch (e: ConnectException) {
                LOGGER.error("Unable to retrieve pubic key, server may be down")
                executor.schedule(r, DELAY_NO_TOKEN, TimeUnit.SECONDS)
            } catch (e: Exception) {
                LOGGER.error("Unable to retrieve pubic key, unknown exception", e)
                executor.schedule(r, DELAY_NO_TOKEN, TimeUnit.SECONDS)
            }
        }
    }
}