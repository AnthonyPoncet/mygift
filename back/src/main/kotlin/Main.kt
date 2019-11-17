import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import dao.BuyAction
import dao.DatabaseManager
import dao.FriendRequestAlreadyExistException
import dao.RequestStatus
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
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
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File
import java.nio.charset.StandardCharsets
import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO


data class FileAnswer(val name: String)
data class Error(val error: String)
data class FriendRequestConflict(val ownRequest: Boolean, val status: RequestStatus, val message: String)
data class NotANumberException(val target: String) : Exception()

fun main(args: Array<String>) {
    mainBody {
        val arguments = ArgParser(args).parseInto(::ArgumentParser)
        val databaseManager = DatabaseManager(arguments.db)
        val userManager = UserManager(databaseManager)

        if (arguments.resetDB) {
            println("Reset DB with default values")
            DbInitializerForTest(databaseManager)
        }

        val uploadsDir = File("uploads")
        if (!uploadsDir.exists()) {
            uploadsDir.mkdir()
        }
        val tmpDir = File("tmp")
        if (!tmpDir.exists()) {
            tmpDir.mkdir()
        }

        println("Start server on port ${arguments.port}")

        val server = embeddedServer(Netty, port = arguments.port) {
            install(CORS) {
                method(HttpMethod.Get)
                method(HttpMethod.Post)
                method(HttpMethod.Delete)
                method(HttpMethod.Put)
                method(HttpMethod.Patch)
                anyHost()
            }
            install(Compression)
            install(ContentNegotiation) {
                gson { setPrettyPrinting() }
            }

            install(StatusPages) {
                exception<NotANumberException> { e -> call.respond(HttpStatusCode.BadRequest, Error("Provided ${e.target} must be a number.")) }
            }

            routing {
                route("/user") {
                    post("/connect") {
                        val connectionInformation = Gson().fromJson(decode(call.receiveText()), ConnectionInformation::class.java)

                        try {
                            val user = userManager.connect(connectionInformation)
                            call.respond(HttpStatusCode.OK, user)
                        } catch (e: BadParamException) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.error))
                        } catch (e: ConnectionException) {
                            call.respond(HttpStatusCode.Conflict, Error(e.error))
                        } catch (e: Exception) {
                            System.err.println(e)
                            call.respond(HttpStatusCode.InternalServerError, Error(e.message?: "Unknown error"))
                        }
                    }
                }

                route("/users") {
                    put {
                        val basicUserInformation = Gson().fromJson(decode(call.receiveText()), UserInformation::class.java)

                        try {
                            val user = userManager.addUser(basicUserInformation)
                            call.respond(HttpStatusCode.Created, user)
                        } catch (e: BadParamException) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.error))
                        } catch (e: CreateUserException) {
                            call.respond(HttpStatusCode.Conflict, Error(e.error))
                        } catch (e: Exception) {
                            System.err.println(e)
                            call.respond(HttpStatusCode.InternalServerError, Error(e.message?: "Unknown error"))
                        }
                    }
                }

                route("users/{id}") {
                    /** USER INFO **/
                    patch {
                        val id = getUserId(call.parameters)
                        val info = Gson().fromJson(decode(call.receiveText()), UserModification::class.java)

                        try {
                            val user = userManager.modifyUser(id, info)
                            call.respond(HttpStatusCode.Accepted, user)
                        } catch (e: BadParamException) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.error))
                        } catch (e: CreateUserException) {
                            call.respond(HttpStatusCode.Conflict, Error(e.error))
                        } catch (e: Exception) {
                            System.err.println(e)
                            call.respond(HttpStatusCode.InternalServerError, Error(e.message?: "Unknown error"))
                        }
                    }

                    /** GIFT **/
                    get("/gifts") {
                        val id = getUserId(call.parameters)

                        try {
                            val userGifts = userManager.getUserGifts(id)
                            call.respond(HttpStatusCode.OK, userGifts)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    get("/gifts/{friendName}") {
                        val id = getUserId(call.parameters)
                        val friendName = call.parameters["friendName"]!!

                        try {
                            val userGifts = userManager.getFriendGifts(id, friendName)
                            call.respond(HttpStatusCode.OK, userGifts)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    put("/gifts") {
                        val id = getUserId(call.parameters)

                        val gift = Gson().fromJson(decode(call.receiveText()), RestGift::class.java)
                        if (gift.name == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("missing name node in json"))
                            return@put
                        }

                        try {
                            val forUser = call.request.queryParameters["forUser"] /* being the name of the user --> add a secret gift*/
                            if (forUser == null) {
                                userManager.addGift(id, gift)
                            } else {
                                userManager.addSecretGift(id, forUser, gift)
                            }

                            call.respond(HttpStatusCode.OK)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    patch("/gifts/{gid}") {
                        val id = getUserId(call.parameters)
                        val gid = getGiftId(call.parameters)

                        val gift = Gson().fromJson(decode(call.receiveText()), RestGift::class.java)
                        if (gift.name == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("missing name node in json"))
                            return@patch
                        }

                        try {
                            userManager.modifyGift(id, gid, gift)
                            call.respond(HttpStatusCode.OK)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    delete("/gifts/{gid}") {
                        val id = getUserId(call.parameters)
                        val gid = getGiftId(call.parameters)

                        try {
                            userManager.removeGift(id, gid)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    //These end points should be without the owner id or else the owner id should be used
                    post("/gifts/{gid}/interested") {
                        val gid = getGiftId(call.parameters)

                        try {
                            val userId = (call.request.queryParameters["userId"] ?: throw Exception("userId query parameter is mandatory")).toLong()
                            userManager.interested(gid, userId)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    delete("/gifts/{gid}/interested") {
                        val gid = getGiftId(call.parameters)

                        try {
                            val userId = (call.request.queryParameters["userId"] ?: throw Exception("userId query parameter is mandatory")).toLong()
                            userManager.notInterested(gid, userId)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    post("/gifts/{gid}/buy-action") {
                        val gid = getGiftId(call.parameters)

                        try {
                            val userId = (call.request.queryParameters["userId"] ?: throw Exception("userId query parameter is mandatory")).toLong()
                            val action = BuyAction.valueOf(call.request.queryParameters["action"] ?: throw Exception("action query parameter is mandatory"))
                            userManager.buy(gid, userId, action)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    delete("/gifts/{gid}/buy-action") {
                        val gid = getGiftId(call.parameters)

                        try {
                            val userId = (call.request.queryParameters["userId"] ?: throw Exception("userId query parameter is mandatory")).toLong()
                            userManager.stopBuy(gid, userId)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    get("/buy-list") {
                        val id = getUserId(call.parameters)

                        try {
                            val buyList = userManager.getBuyList(id)
                            call.respond(HttpStatusCode.OK, buyList)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    /** CATEGORIES **/
                    put("/categories") {
                        val id = getUserId(call.parameters)

                        val category = Gson().fromJson(decode(call.receiveText()), RestCategory::class.java)
                        if (category.name == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("missing name or password node in json"))
                            return@put
                        }

                        try {
                            userManager.addCategory(id, category)
                            call.respond(HttpStatusCode.OK)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    patch("/categories/{cid}") {
                        val id = getUserId(call.parameters)
                        val cid = call.parameters["cid"]!!.toLongOrNull() ?: throw NotANumberException("Category id")

                        val category = Gson().fromJson(decode(call.receiveText()), RestCategory::class.java)
                        if (category.name == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("missing name or password node in json"))
                            return@patch
                        }

                        try {
                            userManager.modifyCategory(id, cid, category)
                            call.respond(HttpStatusCode.OK)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    delete("/categories/{cid}") {
                        val id = getUserId(call.parameters)
                        val cid = call.parameters["cid"]!!.toLongOrNull() ?: throw NotANumberException("Category id")

                        try {
                            userManager.removeCategory(id, cid)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    /** FRIEND REQUEST **/
                    get("/friends") {
                        val id = getUserId(call.parameters)

                        try {
                            val friends = userManager.getFriends(id)
                            call.respond(HttpStatusCode.OK, friends)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    get("/friend-requests/pending") {
                        val id = getUserId(call.parameters)

                        try {
                            val requests = userManager.getPendingFriendRequests(id)
                            call.respond(HttpStatusCode.OK, requests)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    get("/friend-requests/received-blocked") {
                        val id = getUserId(call.parameters)

                        try {
                            val requests = userManager.getReceivedBlockedFriendRequests(id)
                            call.respond(HttpStatusCode.OK, requests)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    put("/friend-requests") {
                        val id = getUserId(call.parameters)

                        val receiveText = decode(call.receiveText())
                        val friendRequest = Gson().fromJson(receiveText, RestCreateFriendRequest::class.java)
                        if (friendRequest.name == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("missing name node in json"))
                            return@put
                        }

                        try {
                            userManager.createFriendRequest(id, friendRequest)
                            call.respond(HttpStatusCode.OK)
                        } catch (e: FriendRequestAlreadyExistException) {
                            call.respond(HttpStatusCode.Conflict, FriendRequestConflict(id == e.dbFriendRequest.userOne, e.dbFriendRequest.status, e.message!!))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    delete("/friend-requests/{fid}") {
                        val id = getUserId(call.parameters)
                        val fid = call.parameters["fid"]!!.toLongOrNull() ?: throw NumberFormatException("Friend Request id")

                        try {
                            userManager.deleteFriendRequest(id, fid)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    //not convince by get
                    get("/friend-requests/{fid}/accept") {
                        val id = getUserId(call.parameters)
                        val fid = call.parameters["fid"]!!.toLongOrNull() ?: throw NumberFormatException("Friend Request id")

                        try {
                            userManager.acceptFriendRequest(id, fid)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    post("/friend-requests/{fid}/decline") {
                        val id = getUserId(call.parameters)
                        val fid = call.parameters["fid"]!!.toLongOrNull() ?: throw NumberFormatException("Friend Request id")

                        try {
                            val blockUser = (call.request.queryParameters["blockUser"] ?: "false").toBoolean()
                            userManager.declineFriendRequest(id, fid, blockUser)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    /** EVENTS **/
                    put("/events") {
                        val id = getUserId(call.parameters)

                        try {
                            val event = Gson().fromJson(decode(call.receiveText()), RestCreateEvent::class.java)
                            userManager.createEvent(event, id)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    delete("/events/{eid}") {
                        val eid = getEventId(call.parameters)

                        try {
                            userManager.deleteEvent(eid)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    get("/events/{eid}") {
                        val eid = getEventId(call.parameters)

                        try {
                            val event = userManager.getEvent(eid)
                            call.respond(HttpStatusCode.OK, event)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    get("/events") {
                        val id = getUserId(call.parameters)

                        try {
                            val name = call.request.queryParameters["name"]
                            val events = if (name == null) {
                                userManager.getEventsCreateBy(id)
                            } else {
                                userManager.getEventsNamed(name)
                            }
                            call.respond(HttpStatusCode.OK, events)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    get("/events-as-participant") {
                        val id = getUserId(call.parameters)

                        try {
                            val events = userManager.getEventsAsParticipants(id)
                            call.respond(HttpStatusCode.OK, events)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    post("/events/{eid}/add-participants") {
                        val eid = getEventId(call.parameters)

                        try {
                            val listType = object : TypeToken<Set<String>>() { }.type
                            val participants: Set<String> = Gson().fromJson(decode(call.receiveText()), listType)
                            userManager.addParticipants(eid, participants)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    get("/events/{eid}/accept") {
                        val id = getUserId(call.parameters)
                        val eid = getEventId(call.parameters)

                        try {
                            userManager.acceptEventInvitation(id, eid)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    post("/events/{eid}/decline") {
                        val id = getUserId(call.parameters)
                        val eid = getEventId(call.parameters)

                        try {
                            val blockEvent = (call.request.queryParameters["blockEvent"] ?: "false").toBoolean()
                            userManager.declineEventInvitation(id, eid, blockEvent)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                }

                post ("/files") {
                    var fileName = ""

                    val multipart = call.receiveMultipart()
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val name = File(part.originalFileName!!).name
                                val ext = File(part.originalFileName!!).extension
                                fileName = "upload-${System.currentTimeMillis()}-${name.hashCode()}.$ext"
                                val file = File("uploads", fileName)
                                part.streamProvider().use {
                                        input -> file.outputStream().buffered().use { output -> input.copyTo(output) } }
                            }
                        }

                        part.dispose()
                    }
                    call.respond(HttpStatusCode.Accepted, FileAnswer(fileName))
                }
                get("/files/{name}") {
                    //All the conversion part should be moved somewhere else
                    //It also write all file in jpg. Could be an issue.
                    val filename = call.parameters["name"]!!
                    val tmpFile = File("tmp/$filename")
                    if (tmpFile.exists()) {
                        call.respondFile(tmpFile)
                        return@get
                    }

                    val file = File("uploads/$filename")
                    if(file.exists()) {
                        val resized = resize(file, 300.toDouble())
                        val output = File("tmp/$filename")
                        ImageIO.write(resized, "jpg", output)
                        call.respondFile(output)
                    }
                    else call.respond(HttpStatusCode.NotFound)
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

                static("static") {
                   resources("static/static")
                }
            }
        }

        server.start(wait = true)
    }
}

fun getUserId(parameters: Parameters) : Long {
    return parameters["id"]!!.toLongOrNull() ?: throw NotANumberException("User id")
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
    var scale: Double = if (oHeight < oWidth) oHeight / size else oWidth / size
    val width = (oWidth / scale).toInt()
    val height = (oHeight / scale).toInt()

    val tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH)
    val resized = BufferedImage(width, height, if (img.colorModel.hasAlpha()) BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_RGB)
    val g2d = resized.createGraphics()
    g2d.drawImage(tmp, 0, 0, null)
    g2d.dispose()
    return resized
}