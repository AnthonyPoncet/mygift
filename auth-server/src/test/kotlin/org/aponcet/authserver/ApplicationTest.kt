package org.aponcet.authserver

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import io.kotlintest.specs.StringSpec
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class ApplicationTest : StringSpec({

    "test could contact end point /public-key" {
        withTestApplication({authModule(TestUserProvider())}) {
            with(handleRequest(HttpMethod.Get, "public-key")) {
                assertEquals(HttpStatusCode.OK, response.status())
                val fromJson = Gson().fromJson(response.content, Map::class.java)//verify could org.aponcet.authserver.decode
                assertTrue(fromJson["key"] != null)
            }
        }
    }

    "test end point /login valid credentials" {
        withTestApplication({authModule(TestUserProvider())}) {
            //Get current public key
            var currentKey: RSAPublicKey
            with(handleRequest(HttpMethod.Get, "public-key")) {
                assertEquals(HttpStatusCode.OK, response.status())
                val fromJson = Gson().fromJson(response.content, Map::class.java) //verify could org.aponcet.authserver.decode
                val keyFactory = KeyFactory.getInstance("RSA")
                val publicKeyString: ArrayList<Byte> = fromJson["key"] as ArrayList<Byte>
                currentKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyString.toByteArray())) as RSAPublicKey
            }

            //connect with valid credential and verify token
            with(handleRequest(HttpMethod.Post, "login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserJson("test", "test")))
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                val fromJson = Gson().fromJson(response.content, Map::class.java)
                assertTrue(fromJson["token"] != null)
                val token = fromJson["token"] as String

                val algorithm = Algorithm.RSA256(currentKey, null)
                val verifier = JWT.require(algorithm).build()
                val verify = verifier.verify(token)

                assertEquals("RS256", verify.algorithm)
                val claims = verify.claims
                assertEquals(1, claims.size)
                val name = claims["name"]
                assertNotNull(name)
                assertEquals("test", name.asString())
            }
        }
    }

    "test end point /login invalid credentials" {
        withTestApplication({authModule(TestUserProvider())}) {
            with(handleRequest(HttpMethod.Post, "login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserJson("toto", "test")))
            }) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                assertEquals(mapOf("OK" to false, "error" to "Unknown user toto"), Gson().fromJson(response.content, Map::class.java))
            }
        }
    }

    "test end point /login missing body" {
        withTestApplication({authModule(TestUserProvider())}) {
            with(handleRequest(HttpMethod.Post, "login")) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(mapOf("OK" to false, "error" to "/login need a json as input"), Gson().fromJson(response.content, Map::class.java))
            }
        }
    }

    "test end point /login missing name in body" {
        withTestApplication({authModule(TestUserProvider())}) {
            with(handleRequest(HttpMethod.Post, "login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserJson(null, "test")))
            }) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(mapOf("OK" to false, "error" to "/login json need name node"), Gson().fromJson(response.content, Map::class.java))
            }
        }
    }

    "test end point /login missing password in body" {
        withTestApplication({authModule(TestUserProvider())}) {
            with(handleRequest(HttpMethod.Post, "login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserJson("toto", null)))
            }) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(mapOf("OK" to false, "error" to "/login json need password node"), Gson().fromJson(response.content, Map::class.java))
            }
        }
    }
})

class TestUserProvider : UserProvider {
    private val users = mapOf("test" to User("test", "test"))

    override fun getUser(name: String): User? {
        return users[name]
    }
}