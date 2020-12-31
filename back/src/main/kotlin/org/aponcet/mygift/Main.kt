package org.aponcet.mygift

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.content.*
import io.ktor.request.receiveMultipart
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import org.aponcet.authserver.UserAndPictureJson
import org.aponcet.authserver.UserAndResetPassword
import org.aponcet.authserver.UserJson
import org.aponcet.mygift.dbmanager.*
import org.aponcet.mygift.dbmanager.maintenance.AdaptTable
import org.aponcet.mygift.dbmanager.maintenance.CleanDataNotUsed
import org.aponcet.mygift.model.ConfigurationLoader
import org.aponcet.mygift.model.Data
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyStore
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import javax.imageio.ImageIO


data class FileAnswer(val name: String)
data class ErrorAnswer(val error: String)
data class FriendRequestConflict(val ownRequest: Boolean, val status: RequestStatus, val message: String)
data class NotANumberException(val target: String) : Exception()
class MalformedPayloadException : Exception()

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

    if (configuration.mainServer.resetDB) {
        DbInitializerForTest(databaseManager)
    }

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
        exception<NotANumberException> { e -> call.respond(HttpStatusCode.BadRequest, ErrorAnswer("Provided ${e.target} must be a number.")) }
    }

    install(Authentication) {
        jwt {
            verifier { makeVerifier(publicKeyManager) }
            validate { JWTPrincipal(it.payload) }
        }
    }
    if (!debug) install(HttpsRedirect)

    routing {
        /** CONNECT **/
        route("/user") {
            post("/connect") {
                val userJson = Gson().fromJson(decode(call.receiveText()), UserJson::class.java)

                try {
                    val user = userManager.connect(userJson)
                    call.respond(HttpStatusCode.OK, user)
                } catch (e: BadParamException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorAnswer(e.error))
                } catch (e: ConnectionException) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorAnswer(e.error))
                } catch (e: Exception) {
                    call.application.environment.log.error("Error while connecting user", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorAnswer(e.message ?: "Unknown error"))
                }
            }
        }
        /** Create user **/
        route("/users") {
            put {
                val basicUserInformation = Gson().fromJson(decode(call.receiveText()), UserAndPictureJson::class.java)
                try {
                    val user = userManager.addUser(basicUserInformation)
                    call.respond(HttpStatusCode.Created, user)
                } catch (e: BadParamException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorAnswer(e.message!!))
                } catch (e: CreateUserException) {
                    call.respond(HttpStatusCode.Conflict, ErrorAnswer(e.message!!))
                } catch (e: Exception) {
                    call.application.environment.log.error("Error while creating user", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorAnswer(e.message ?: "Unknown error"))
                }
            }
        }

        /** USERS **/
        authenticate {
            route("/users") {
                patch {
                    handle(call) {id ->
                        val info = Gson().fromJson(decode(call.receiveText()), UserModification::class.java)
                        try {
                            val user = userManager.modifyUser(id, info)
                            call.respond(HttpStatusCode.Accepted, user)
                        } catch (e: BadParamException) {
                            call.respond(HttpStatusCode.BadRequest, ErrorAnswer(e.error))
                        } catch (e: CreateUserException) {
                            call.respond(HttpStatusCode.Conflict, ErrorAnswer(e.error))
                        }
                    }
                }
            }
        }

        /** GIFT **/
        authenticate {
            route("gifts") {
                get {
                    handle(call) {id ->
                        val userGifts = userManager.getUserGifts(id)
                        call.respond(HttpStatusCode.OK, userGifts)
                    }
                }
                put {
                    handle(call) {id ->
                        val gift = Gson().fromJson(decode(call.receiveText()), RestGift::class.java)
                        if (gift.name == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorAnswer("missing name node in json"))
                        } else {
                            val forUser = call.request.queryParameters["forUser"] /* being the name of the user --> add a secret gift*/
                            if (forUser == null) {
                                userManager.addGift(id, gift)
                            } else {
                                userManager.addSecretGift(id, forUser, gift)
                            }
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }
                patch("/{gid}") {
                    val gid = getGiftId(call.parameters)
                    handle(call) {id ->
                        val gift = Gson().fromJson(decode(call.receiveText()), RestGift::class.java)
                        if (gift.name == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorAnswer("missing name node in json"))
                        } else {
                            userManager.modifyGift(id, gid, gift)
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
                delete("/{gid}") {
                    val gid = getGiftId(call.parameters)
                    handle(call) {id ->
                        val status = Status.valueOf(call.request.queryParameters["status"] ?: throw Exception("status query parameter is mandatory"))
                        userManager.removeGift(id, gid, status)
                        call.respond(HttpStatusCode.Accepted)
                    }
                }

                post("/{gid}/rank-actions/{action}") {
                    val gid = getGiftId(call.parameters)
                    val action = call.parameters["action"] ?: throw IllegalStateException("Missing action")
                    if (action != "down" && action != "up") {
                        throw IllegalStateException("Only allowed action are up or down")
                    }
                    handle(call) {id ->
                        userManager.changeGiftRank(id, gid, RankAction.valueOf(action.toUpperCase()))
                        call.respond(HttpStatusCode.Accepted)
                    }
                }

                get("/{friendName}") {
                    handle(call) {id ->
                        val friendName = call.parameters["friendName"]!!
                        val userGifts = userManager.getFriendGifts(id, friendName)
                        call.respond(HttpStatusCode.OK, userGifts)
                    }
                }

                post("/{gid}/interested") {
                    val gid = getGiftId(call.parameters)
                    handle(call) {id ->
                        userManager.interested(gid, id)
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
                delete("/{gid}/interested") {
                    val gid = getGiftId(call.parameters)
                    handle(call) {id ->
                        userManager.notInterested(gid, id)
                        call.respond(HttpStatusCode.Accepted)
                    }
                }

                post("/{gid}/buy-action") {
                    val gid = getGiftId(call.parameters)
                    handle(call) {id ->
                        val action = BuyAction.valueOf(call.request.queryParameters["action"] ?: throw Exception("action query parameter is mandatory"))
                        userManager.buy(gid, id, action)
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
                delete("/{gid}/buy-action") {
                    val gid = getGiftId(call.parameters)
                    handle(call) {id ->
                        userManager.stopBuy(gid, id)
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
            }
        }

        /** BUY LIST **/
        authenticate {
            get("/buy-list") {
                handle(call) {id ->
                    val buyList = userManager.getBuyList(id)
                    call.respond(HttpStatusCode.OK, buyList)
                }
            }
            delete("/buy-list/deleted-gifts/{gid}") {
                val gid = call.parameters["gid"]!!.toLongOrNull() ?: throw NotANumberException("Gift id")
                handle(call) {id ->
                    userManager.deleteDeletedGift(gid, id)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        /** CATEGORIES **/
        authenticate {
            route("/categories") {
                put {
                    handle(call) {id ->
                        val category = Gson().fromJson(decode(call.receiveText()), RestCategory::class.java)
                        if (category.name == null || category.share == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorAnswer("Invalid category json"))
                        } else {
                            userManager.addCategory(category, id, category.share)
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
                patch("/{cid}") {
                    val cid = call.parameters["cid"]!!.toLongOrNull() ?: throw NotANumberException("Category id")
                    handle(call) {id ->
                        val category = Gson().fromJson(decode(call.receiveText()), RestCategory::class.java)
                        if (category.name == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorAnswer("Invalid category json"))
                        } else {
                            userManager.modifyCategory(id, cid, category)
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
                delete("/{cid}") {
                    val cid = call.parameters["cid"]!!.toLongOrNull() ?: throw NotANumberException("Category id")
                    handle(call) {id ->
                        userManager.removeCategory(id, cid)
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
                post("/{cid}/rank-actions/{action}") {
                    val cid = call.parameters["cid"]!!.toLongOrNull() ?: throw NotANumberException("Category id")
                    handle(call) {id ->
                        val action = call.parameters["action"] ?: throw IllegalStateException("Missing action")
                        if (action != "down" && action != "up") {
                            throw IllegalStateException("Only allowed action are up or down")
                        }
                        userManager.changeCategoryRank(id, cid, RankAction.valueOf(action.toUpperCase()))
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
            }
        }

        /** FRIEND REQUEST **/
        authenticate {
            get("/friends") {
                handle(call) {id ->
                    call.respond(HttpStatusCode.OK, userManager.getFriends(id))
                }
            }

            route("/friend-requests") {
                get("/pending") {
                    handle(call) {id ->
                        val requests = userManager.getPendingFriendRequests(id)
                        call.respond(HttpStatusCode.OK, requests)
                    }
                }
                get("/received-blocked") {
                    handle(call) {id ->
                        val requests = userManager.getReceivedBlockedFriendRequests(id)
                        call.respond(HttpStatusCode.OK, requests)
                    }
                }
                put {
                    handle(call) {id ->
                        val friendRequest = Gson().fromJson(decode(call.receiveText()), RestCreateFriendRequest::class.java)
                        if (friendRequest.name == null) {
                            call.respond(HttpStatusCode.BadRequest, ErrorAnswer("missing name node in json"))
                        } else {
                            try {
                                userManager.createFriendRequest(id, friendRequest)
                                call.respond(HttpStatusCode.OK)
                            } catch (e: FriendRequestAlreadyExistException) {
                                call.respond(HttpStatusCode.Conflict, FriendRequestConflict(id == e.dbFriendRequest.userOne, e.dbFriendRequest.status, e.message!!))
                            }
                        }
                    }
                }
                delete("/{fid}") {
                    val fid = call.parameters["fid"]!!.toLongOrNull() ?: throw NumberFormatException("Friend Request id")
                    handle(call) {id ->
                        userManager.deleteFriendRequest(id, fid)
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
                //not convince by get
                get("/{fid}/accept") {
                    val fid = call.parameters["fid"]!!.toLongOrNull() ?: throw NumberFormatException("Friend Request id")
                    handle(call) {id ->
                        userManager.acceptFriendRequest(id, fid)
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
                post("/{fid}/decline") {
                    val fid = call.parameters["fid"]!!.toLongOrNull() ?: throw NumberFormatException("Friend Request id")
                    handle(call) {id ->
                        val blockUser = (call.request.queryParameters["blockUser"] ?: "false").toBoolean()
                        userManager.declineFriendRequest(id, fid, blockUser)
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
            }
        }

        /** FILES **/
        authenticate {
            route("/files") {
                post {
                    var fileName = ""

                    val multipart = call.receiveMultipart()
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val name = File(part.originalFileName!!).name
                                val ext = File(part.originalFileName!!).extension
                                fileName = "upload-${System.currentTimeMillis()}-${name.hashCode()}.$ext"
                                val file = File(data.uploads, fileName)
                                part.streamProvider().use { input ->
                                    file.outputStream().buffered().use { output -> input.copyTo(output) }
                                }
                            }
                        }

                        part.dispose()
                    }
                    call.respond(HttpStatusCode.Accepted, FileAnswer(fileName))
                }
                get("/{name}") {
                    //All the conversion part should be moved somewhere else
                    val filename = call.parameters["name"]!!
                    val tmpFile = File("${data.tmp}/$filename")
                    if (tmpFile.exists()) {
                        call.respondFile(tmpFile)
                        return@get
                    }

                    val file = File("${data.uploads}/$filename")
                    if (file.exists()) {
                        val resized = resize(file, 300.toDouble())
                        val output = File("${data.tmp}/$filename")
                        ImageIO.write(resized, tmpFile.extension, output)
                        call.respondFile(output)
                    } else call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        route("passwords/reset/{uuid}") {
            get {
                val uuid = call.parameters["uuid"]!!
                try {
                    userManager.getEntry(uuid)
                    call.respond(HttpStatusCode.Found)
                } catch (e: Exception) {
                    call.application.environment.log.error("Error while getting password reset uuid", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorAnswer(e.message ?: "Unknown error"))
                }
            }
            post {
                try {
                    val uuid = call.parameters["uuid"]!!
                    val userAndResetPassword =
                        Gson().fromJson(decode(call.receiveText()), UserAndResetPassword::class.java)
                    userManager.modifyPassword(userAndResetPassword, uuid)
                    call.respond(HttpStatusCode.Accepted)
                } catch (e: Exception) {
                    call.application.environment.log.error("Error while resetting password", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorAnswer(e.message ?: "Unknown error"))
                }
            }
        }

        static("/") {
           resources("static")
           defaultResource("static/index.html")
        }

        static("/signin") {
            resources("static")
            defaultResource("static/index.html")
        }
        static("/signup") {
            resources("static")
            defaultResource("static/index.html")
        }
        static("/mywishlist") {
            resources("static")
            defaultResource("static/index.html")
        }
        static("/myfriends") {
            resources("static")
            defaultResource("static/index.html")
        }
        static("/friend/*") {
            resources("static")
            defaultResource("static/index.html")
        }
        static("/events") {
            resources("static")
            defaultResource("static/index.html")
        }
        static("/event/*") {
            resources("static")
            defaultResource("static/index.html")
        }
        static("/buy-list") {
            resources("static")
            defaultResource("static/index.html")
        }
        static("/manage-account") {
            resources("static")
            defaultResource("static/index.html")
        }
        static("/reset-password/*") {
            resources("static")
            defaultResource("static/index.html")
        }

        static("static") {
           resources("static/static")
        }
    }
}


fun getGiftId(parameters: Parameters) : Long {
    return parameters["gid"]!!.toLongOrNull() ?: throw NotANumberException("Gift id")
}

fun getEventId(parameters: Parameters) : Long {
    return parameters["eid"]!!.toLongOrNull() ?: throw NotANumberException("Event id")
}

fun decode(input: String) : String {
    return input.toByteArray(StandardCharsets.ISO_8859_1).toString(StandardCharsets.UTF_8)
}

//will be squared resize, size being a side
private fun resize(file: File, size: Double): BufferedImage {
    val img = ImageIO.read(file)

    /** Keep proportion **/
    val oHeight = img.height.toDouble()
    val oWidth = img.width.toDouble()
    val scale: Double = if (oHeight < oWidth) oHeight / size else oWidth / size
    val width = (oWidth / scale).toInt()
    val height = (oHeight / scale).toInt()

    val resized = BufferedImage(width, height, if (img.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB)
    val g2d = resized.createGraphics()
    g2d.drawImage(img, 0, 0, width, height, null)
    g2d.dispose()
    return resized
}

private suspend fun handle(call: ApplicationCall, function: suspend (Long) -> Unit) {
    val payload = (call.authentication.principal as JWTPrincipal).payload
    val id = payload.getClaim("id").asLong() ?: throw MalformedPayloadException()
    try {
        function(id)
    } catch (e: Exception) {
        call.application.environment.log.error("Error while handling request parameter parsing", e)
        call.respond(HttpStatusCode.InternalServerError, ErrorAnswer(e.message ?: "Unknown error"))
    }
}