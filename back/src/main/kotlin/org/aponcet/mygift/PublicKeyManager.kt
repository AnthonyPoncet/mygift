package org.aponcet.mygift

import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.ResponseException
import io.ktor.client.request.get
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.aponcet.authserver.KeyResponse
import java.net.ConnectException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PublicKeyManager(private val port: Int) {

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val client = HttpClient(Apache)

    var publicKey : ByteArray? = null

    companion object {
        const val DELAY_NO_TOKEN = 10L
        const val DELAY_TOKEN = 720L //each 12h
    }

    fun start() {
        val r = Runnable { loop() }
        executor.schedule(r, 0, TimeUnit.SECONDS)
    }

    fun loop() {
        GlobalScope.launch {
            val r = Runnable { loop() }
            try {
                val body = client.get<String>("http://127.0.0.1:$port/public-key")
                executor.schedule(r, DELAY_TOKEN, TimeUnit.SECONDS)
                publicKey = Gson().fromJson(body, KeyResponse::class.java).key
            } catch (e: ResponseException) {
                System.err.println("Unable to retrieve pubic key: $e")
                executor.schedule(r, DELAY_NO_TOKEN, TimeUnit.SECONDS)
            } catch (e: ConnectException) {
                System.err.println("Unable to retrieve pubic key, server may be down")
                executor.schedule(r, DELAY_NO_TOKEN, TimeUnit.SECONDS)
            } catch (e: Exception) {
                System.err.println("Unable to retrieve pubic key, unknown exception: $e")
                executor.schedule(r, DELAY_NO_TOKEN, TimeUnit.SECONDS)
            }
        }
    }
}