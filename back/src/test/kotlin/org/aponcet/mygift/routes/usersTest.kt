package org.aponcet.mygift.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import com.google.gson.Gson
import io.kotlintest.Description
import io.kotlintest.Spec
import io.kotlintest.specs.StringSpec
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.routing.*
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


class usersTest : StringSpec() {

    private lateinit var tokenResponse: TokenResponse
    private lateinit var jwtVerifier: JWTVerifier

    companion object {
        val USER_MANAGER = mockk<UserManager>()
        val USER_AND_PICTURE_JSON = UserAndPictureJson("test", "test", "picture")
        val USER_JSON = UserJson("test", "test")
        val USER_ANSWER = User("token", "test", "picture")
        val USER_MODIFICATION = UserModification("test2", "other picture")
        val MODIFIED_USER_ANSWER = User("token", "test2", "other picture")

        val DECODED_JWT = mockk<DecodedJWT>()
        val JWT_VERIFIER = mockk<JWTVerifier>()
    }

    override fun isInstancePerTest() = true

    override fun beforeSpec(description: Description, spec: Spec) {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val generateKeyPair = keyPairGenerator.generateKeyPair()
        val simpleJwt = SimpleJWT(
            generateKeyPair.public as RSAPublicKey,
            generateKeyPair.private as RSAPrivateKey
        )

        tokenResponse = TokenResponse(simpleJwt.sign(1, "test"))

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

            withTestApplication({ userModule() }) {
                with(handleRequest(HttpMethod.Put, "users") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_AND_PICTURE_JSON))
                }) {
                    assertEquals(HttpStatusCode.Created, response.status())
                    assertEquals(USER_ANSWER, Gson().fromJson(response.content, User::class.java))
                }
            }
        }

        "test create user throw" {
            coEvery { USER_MANAGER.addUser(USER_AND_PICTURE_JSON) } throws CreateUserException("Could not create")

            withTestApplication({ userModule() }) {
                with(handleRequest(HttpMethod.Put, "users") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_AND_PICTURE_JSON))
                }) {
                    assertEquals(HttpStatusCode.Conflict, response.status())
                    assertEquals(
                        ErrorAnswer("Unable to create user. Cause: Could not create"),
                        Gson().fromJson(response.content, ErrorAnswer::class.java)
                    )
                }
            }
        }

        "test connect with valid" {
            coEvery { USER_MANAGER.connect(USER_JSON) } returns USER_ANSWER

            withTestApplication({ userModule() }) {
                with(handleRequest(HttpMethod.Post, "user/connect") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_JSON))
                }) {
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertEquals(USER_ANSWER, Gson().fromJson(response.content, User::class.java))
                }
            }
        }

        "test connect throw bad parameter" {
            coEvery { USER_MANAGER.connect(USER_JSON) } throws BadParamException("Invalid ...")

            withTestApplication({ userModule() }) {
                with(handleRequest(HttpMethod.Post, "user/connect") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_JSON))
                }) {
                    assertEquals(HttpStatusCode.BadRequest, response.status())
                    assertEquals(
                        ErrorAnswer("Invalid ..."),
                        Gson().fromJson(response.content, ErrorAnswer::class.java)
                    )
                }
            }
        }

        "test connect throw unauthorized" {
            coEvery { USER_MANAGER.connect(USER_JSON) } throws ConnectionException("Invalid credentials")

            withTestApplication({ userModule() }) {
                with(handleRequest(HttpMethod.Post, "user/connect") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_JSON))
                }) {
                    assertEquals(HttpStatusCode.Unauthorized, response.status())
                    assertEquals(
                        ErrorAnswer("Invalid credentials"),
                        Gson().fromJson(response.content, ErrorAnswer::class.java)
                    )
                }
            }
        }

        "test update user valid request" {
            every { JWT_VERIFIER.verify("mytoken") } returns DECODED_JWT

            coEvery { USER_MANAGER.modifyUser(1, USER_MODIFICATION) } returns MODIFIED_USER_ANSWER

            withTestApplication({ userModule() }) {
                with(handleRequest(HttpMethod.Patch, "users") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${tokenResponse.token}")
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_MODIFICATION))
                }) {
                    assertEquals(HttpStatusCode.Accepted, response.status())
                    assertEquals(MODIFIED_USER_ANSWER, Gson().fromJson(response.content, User::class.java))
                }
            }
        }

        "test update user invalid request" {
            every { JWT_VERIFIER.verify("mytoken") } returns DECODED_JWT

            coEvery { USER_MANAGER.modifyUser(1, USER_MODIFICATION) } throws CreateUserException("User does not exists")

            withTestApplication({ userModule() }) {
                with(handleRequest(HttpMethod.Patch, "users") {
                    addHeader(HttpHeaders.Authorization, "Bearer ${tokenResponse.token}")
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(Gson().toJson(USER_MODIFICATION))
                }) {
                    assertEquals(HttpStatusCode.Conflict, response.status())
                    assertEquals(
                        ErrorAnswer("User does not exists"),
                        Gson().fromJson(response.content, ErrorAnswer::class.java)
                    )
                }
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

        routing { users(USER_MANAGER) }
    }
}
