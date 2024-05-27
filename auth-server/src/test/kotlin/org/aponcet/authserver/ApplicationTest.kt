package org.aponcet.authserver

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import io.kotlintest.specs.StringSpec
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class ApplicationTest : StringSpec({

    "test could contact end point /public-key" {
        testApplication {
            this.application { authModule(TestUserProvider()) }
            val response = client.get("/public-key")
            assertEquals(HttpStatusCode.OK, response.status)
            val fromJson =
                Gson().fromJson(response.bodyAsText(), Map::class.java) //verify could org.aponcet.authserver.decode
            assertTrue(fromJson["key"] != null)
        }
    }

    "test end point /login valid credentials" {
        testApplication {
            this.application { authModule(TestUserProvider()) }
            //Get current public key
            val responseGet = client.get("/public-key")
            assertEquals(HttpStatusCode.OK, responseGet.status)
            val keyResponse = Gson().fromJson(
                responseGet.bodyAsText(),
                KeyResponse::class.java
            ) //verify could org.aponcet.authserver.decode
            val keyFactory = KeyFactory.getInstance("RSA")
            val currentKey = keyFactory.generatePublic(X509EncodedKeySpec(keyResponse.key)) as RSAPublicKey


            //connect with valid credential and verify token
            val responsePost = client.post("/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserJson("test", "test", null)))
            }
            assertEquals(HttpStatusCode.OK, responsePost.status)
            val tokenResponse = Gson().fromJson(responsePost.bodyAsText(), TokenResponse::class.java)
            val token = tokenResponse.jwt

            val algorithm = Algorithm.RSA256(currentKey, null)
            val verifier = JWT.require(algorithm).build()
            val verify = verifier.verify(token)

            assertEquals("RS256", verify.algorithm)
            val claims = verify.claims
            assertEquals(2, claims.size)
            val id = claims["id"]
            assertNotNull(id)
            assertEquals(1, id.asLong())
            val session = claims["session"]
            assertNotNull(session)
        }
    }

    "test end point /login invalid credentials" {
        testApplication {
            this.application { authModule(TestUserProvider()) }
            val response = client.post("/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserJson("toto", "test", null)))
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals(
                ErrorResponse("Unknown user 'toto' or password mismatch"),
                Gson().fromJson(response.bodyAsText(), ErrorResponse::class.java)
            )
        }
    }

    "test end point /login missing body" {
        testApplication {
            this.application { authModule(TestUserProvider()) }
            val response = client.post("/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(
                ErrorResponse("/login need a json as input"),
                Gson().fromJson(response.bodyAsText(), ErrorResponse::class.java)
            )
        }
    }

    "test end point /login missing name in body" {
        testApplication {
            this.application { authModule(TestUserProvider()) }
            val response = client.post("/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserJson(null, "test", null)))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(
                ErrorResponse("/login json need name node"),
                Gson().fromJson(response.bodyAsText(), ErrorResponse::class.java)
            )
        }
    }

    "test end point /login missing password in body" {
        testApplication {
            this.application { authModule(TestUserProvider()) }
            val response = client.post("/login") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserJson("toto", null, null)))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(
                ErrorResponse("/login json need password node"),
                Gson().fromJson(response.bodyAsText(), ErrorResponse::class.java)
            )
        }
    }

    "test end point /create valid" {
        testApplication {
            this.application { authModule(TestUserProvider()) }
            val response = client.put("/create") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserAndPictureJson("toto", "pwd", "picture", 50)))
            }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    "test end point /create call two times same user" {
        testApplication {
            this.application { authModule(TestUserProvider()) }
            val response = client.put("/create") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserAndPictureJson("test", "pwd", "picture", 50)))
            }
            assertEquals(HttpStatusCode.Conflict, response.status)
        }
    }

    "test end point /create call missing name" {
        testApplication {
            this.application { authModule(TestUserProvider()) }
            val response = client.put("/create") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserAndPictureJson(null, "pwd", "picture", 50)))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    "test end point /create call missing password" {
        testApplication {
            this.application { authModule(TestUserProvider()) }
            val response = client.put("/create") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Gson().toJson(UserAndPictureJson("toto", null, "picture", 50)))
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }
})

class TestUserProvider : UserProvider {
    private val users = mapOf(
        "test" to User(
            1,
            "test",
            EncodedPasswordAndSalt(PasswordManager.hash("test", "salt".toByteArray()), "salt".toByteArray())
        )
    )

    override fun addUser(name: String, password: ByteArray, salt: ByteArray, picture: String, dateOfBirth: Long?) {
    }

    override fun modifyUser(name: String, password: ByteArray, salt: ByteArray) {
    }

    override fun getUser(name: String): User? {
        return users[name]
    }

    override fun addSession(session: String, userId: Long) {
    }
}