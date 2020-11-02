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
                val keyResponse = Gson().fromJson(response.content, KeyResponse::class.java) //verify could org.aponcet.authserver.decode
                val keyFactory = KeyFactory.getInstance("RSA")
                currentKey = keyFactory.generatePublic(X509EncodedKeySpec(keyResponse.key)) as RSAPublicKey
            }

            //connect with valid credential and verify token
            with(handleRequest(HttpMethod.Post, "login") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserJson("test", "test")))
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                val tokenResponse = Gson().fromJson(response.content, TokenResponse::class.java)
                val token = tokenResponse.token

                val algorithm = Algorithm.RSA256(currentKey, null)
                val verifier = JWT.require(algorithm).build()
                val verify = verifier.verify(token)

                assertEquals("RS256", verify.algorithm)
                val claims = verify.claims
                assertEquals(2, claims.size)
                val name = claims["name"]
                assertNotNull(name)
                assertEquals("test", name.asString())
                val id = claims["id"]
                assertNotNull(id)
                assertEquals(1, id.asLong())
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
                assertEquals(ErrorResponse("Unknown user 'toto' or password mismatch"), Gson().fromJson(response.content, ErrorResponse::class.java))
            }
        }
    }

    "test end point /login missing body" {
        withTestApplication({authModule(TestUserProvider())}) {
            with(handleRequest(HttpMethod.Post, "login")) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(ErrorResponse("/login need a json as input"), Gson().fromJson(response.content, ErrorResponse::class.java))
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
                assertEquals(ErrorResponse("/login json need name node"), Gson().fromJson(response.content, ErrorResponse::class.java))
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
                assertEquals(ErrorResponse("/login json need password node"), Gson().fromJson(response.content, ErrorResponse::class.java))
            }
        }
    }

    "test end point /create valid" {
        withTestApplication({authModule(TestUserProvider())}) {
            with(handleRequest(HttpMethod.Put, "create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserAndPictureJson("toto", "pwd", "picture")))
            }) {
                assertEquals(HttpStatusCode.Created, response.status())
            }
        }
    }

    "test end point /create call two times same user" {
        withTestApplication({authModule(TestUserProvider())}) {
            with(handleRequest(HttpMethod.Put, "create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserAndPictureJson("test", "pwd", "picture")))
            }) {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
        }
    }

    "test end point /create call missing name" {
        withTestApplication({authModule(TestUserProvider())}) {
            with(handleRequest(HttpMethod.Put, "create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserAndPictureJson(null, "pwd", "picture")))
            }) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    "test end point /create call missing password" {
        withTestApplication({authModule(TestUserProvider())}) {
            with(handleRequest(HttpMethod.Put, "create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserAndPictureJson("toto", null, "picture")))
            }) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }
})

class TestUserProvider : UserProvider {
    private val users = mapOf("test" to User(1, "test", EncodedPasswordAndSalt(PasswordManager.hash("test", "salt".toByteArray()), "salt".toByteArray())))

    override fun addUser(name: String, password: ByteArray, salt: ByteArray, picture: String) {
    }

    override fun getUser(name: String): User? {
        return users[name]
    }
}