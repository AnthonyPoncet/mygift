package org.aponcet.mygift.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import com.google.gson.Gson
import io.kotlintest.IsolationMode
import io.kotlintest.Spec
import io.kotlintest.specs.StringSpec
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.aponcet.authserver.SimpleJWT
import org.aponcet.authserver.TokenResponse
import org.aponcet.authserver.UserAndPictureJson
import org.aponcet.authserver.UserJson
import org.aponcet.mygift.*
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import kotlin.test.assertEquals

class UsersTest : StringSpec() {

    private lateinit var tokenResponse: TokenResponse
    private lateinit var jwtVerifier: JWTVerifier

    companion object {
        val USER_MANAGER = mockk<UserManager>()
        val USER_AND_PICTURE_JSON = UserAndPictureJson("test", "test", "picture", 50)
        val USER_JSON = UserJson("test", "test", null)
        val USER_ANSWER = User("token", "session", "test", "picture", 50)
        val USER_MODIFICATION = UserModification("test2", "other picture", 42)
        val MODIFIED_USER_ANSWER = User("token", "session", "test2", "other picture", 50)

        val DECODED_JWT = mockk<DecodedJWT>()
        val JWT_VERIFIER = mockk<JWTVerifier>()
    }

    override fun isolationMode() = IsolationMode.InstancePerTest

    override fun beforeSpec(spec: Spec) {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val generateKeyPair = keyPairGenerator.generateKeyPair()
        val simpleJwt = SimpleJWT(
            generateKeyPair.public as RSAPublicKey,
            generateKeyPair.private as RSAPrivateKey
        )

        tokenResponse = TokenResponse(simpleJwt.sign(1, "session"), "session")

        val publicKey = simpleJwt.publicKey.encoded

        val keyFactory = KeyFactory.getInstance("RSA")
        val key = keyFactory.generatePublic(X509EncodedKeySpec(publicKey)) as RSAPublicKey
        jwtVerifier = JWT
            .require(Algorithm.RSA256(key, null))
            .build()
    }

    init {
        "test create valid user" {
            coEvery { USER_MANAGER.addUser(USER_AND_PICTURE_JSON) } returns USER_ANSWER
            testApplication {
                this.application { userModule() }
                val response = client.put("/users") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_AND_PICTURE_JSON))
                }
                assertEquals(HttpStatusCode.Created, response.status)
                assertEquals(USER_ANSWER, Gson().fromJson(response.bodyAsText(), User::class.java))
            }
        }

        "test create user throw" {
            coEvery { USER_MANAGER.addUser(USER_AND_PICTURE_JSON) } throws CreateUserException("Could not create")

            testApplication {
                this.application { userModule() }
                val response = client.put("/users") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_AND_PICTURE_JSON))
                }
                assertEquals(HttpStatusCode.Conflict, response.status)
                assertEquals(
                    ErrorAnswer("Unable to create user. Cause: Could not create"),
                    Gson().fromJson(response.bodyAsText(), ErrorAnswer::class.java)
                )
            }
        }

        "test connect with valid" {
            coEvery { USER_MANAGER.connect(USER_JSON) } returns USER_ANSWER

            testApplication {
                this.application { userModule() }
                val response = client.post("/user/connect") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_JSON))
                }
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(USER_ANSWER, Gson().fromJson(response.bodyAsText(), User::class.java))
            }
        }

        "test connect throw bad parameter" {
            coEvery { USER_MANAGER.connect(USER_JSON) } throws BadParamException("Invalid ...")

            testApplication {
                this.application { userModule() }
                val response = client.post("/user/connect") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_JSON))
                }
                assertEquals(HttpStatusCode.BadRequest, response.status)
                assertEquals(
                    ErrorAnswer("Invalid ..."),
                    Gson().fromJson(response.bodyAsText(), ErrorAnswer::class.java)
                )
            }
        }

        "test connect throw unauthorized" {
            coEvery { USER_MANAGER.connect(USER_JSON) } throws ConnectionException("Invalid credentials")

            testApplication {
                this.application { userModule() }
                val response = client.post("/user/connect") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_JSON))
                }
                assertEquals(HttpStatusCode.Unauthorized, response.status)
                assertEquals(
                    ErrorAnswer("Invalid credentials"),
                    Gson().fromJson(response.bodyAsText(), ErrorAnswer::class.java)
                )
            }
        }

        "test update user valid request" {
            every { JWT_VERIFIER.verify("mytoken") } returns DECODED_JWT

            coEvery { USER_MANAGER.modifyUser(1, USER_MODIFICATION) } returns MODIFIED_USER_ANSWER

            testApplication {
                this.application { userModule() }
                val response = client.patch("/users") {
                    header(HttpHeaders.Authorization, "Bearer ${tokenResponse.jwt}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_MODIFICATION))
                }
                assertEquals(HttpStatusCode.Accepted, response.status)
                assertEquals(MODIFIED_USER_ANSWER, Gson().fromJson(response.bodyAsText(), User::class.java))
            }
        }

        "test update user invalid request" {
            every { JWT_VERIFIER.verify("mytoken") } returns DECODED_JWT

            coEvery { USER_MANAGER.modifyUser(1, USER_MODIFICATION) } throws UpdateUserException("User does not exists")

            testApplication {
                this.application { userModule() }
                val response = client.patch("/users") {
                    header(HttpHeaders.Authorization, "Bearer ${tokenResponse.jwt}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_MODIFICATION))
                }
                assertEquals(HttpStatusCode.Conflict, response.status)
                assertEquals(
                    ErrorAnswer("User does not exists"),
                    Gson().fromJson(response.bodyAsText(), ErrorAnswer::class.java)
                )
            }
        }
    }

    private fun Application.userModule() {
        install(ContentNegotiation) {
            gson { setPrettyPrinting() }
        }

        install(Authentication) {
            jwt {
                verifier { jwtVerifier }
                validate { JWTPrincipal(it.payload) }
            }
        }

        install(Sessions) {
            cookie<Session>("token_session") {
            }
        }

        routing { users(USER_MANAGER) }
    }
}
