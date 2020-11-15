package org.aponcet.mygift.model

import java.io.File
import com.google.gson.Gson

data class Configuration(val data: Data, val authServer: AuthServer, val mainServer: MainServer)
data class Data(val database: String, val uploads: String, val tmp: String)
data class AuthServer(val host: String, val port: Int)
data class MainServer(val host: String, val httpPort: Int, val httpsPort: Int, val debug: Boolean, val resetDB: Boolean, val jks: Jks)
data class Jks(val path: String, val keyAlias: String, val jksPassword: String, val aliasKey: String)

class ConfigurationLoader {
    companion object {
        private val gson = Gson()

        fun load(configurationPath: String) : Configuration {
            val configurationFile = File(configurationPath)
            if (!configurationFile.exists()) {
                throw IllegalArgumentException("File $configurationPath does not exist")
            }

            val content = configurationFile.readText()

            return gson.fromJson(content, Configuration::class.java)
        }
    }
}