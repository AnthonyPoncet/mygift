import com.google.gson.Gson
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
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
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.lang.NumberFormatException

data class Error(val error: String)
data class FriendRequestConflict(val ownRequest: Boolean, val status: RequestStatus, val message: String)

data class NotANumberException(val target: String) : Exception()

fun main(args: Array<String>) {
    mainBody {
        val arguments = ArgParser(args).parseInto(::ArgumentParser)
        val databaseManager = DatabaseManager(arguments.db)
        val userManager = UserManager(databaseManager)

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
                        val receiveText = call.receiveText()
                        val connectionInformation = Gson().fromJson(receiveText, ConnectionInformation::class.java)

                        if (connectionInformation.name == null || connectionInformation.password == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("missing name or password node in json"))
                            return@post
                        }

                        try {
                            val user = userManager.connect(connectionInformation)
                            call.respond(HttpStatusCode.OK, user)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.Conflict, Error(e.message!!))
                        }
                    }
                }

                route("/users") {
                    put {
                        val basicUserInformation = Gson().fromJson(call.receiveText(), UserInformation::class.java)

                        if (basicUserInformation.name == null || basicUserInformation.password == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("missing name or password node in json"))
                            return@put
                        }

                        try {
                            val user = userManager.addUser(basicUserInformation)
                            call.respond(HttpStatusCode.Created, user)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.Conflict, Error(e.message!!))
                        }
                    }
                }

                route("users/{id}") {
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

                        val receiveText = call.receiveText()
                        val gift = Gson().fromJson(receiveText, RestGift::class.java)
                        if (gift.name == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("missing name node in json"))
                            return@put
                        }

                        try {
                            userManager.addGift(id, gift)
                            call.respond(HttpStatusCode.OK)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    patch("/gifts/{gid}") {
                        val id = getUserId(call.parameters)
                        val gid = call.parameters["gid"]!!.toLongOrNull() ?: throw NotANumberException("Gift id")

                        val gift = Gson().fromJson(call.receiveText(), RestGift::class.java)
                        if (gift.name == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("missing name or password node in json"))
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
                        val gid = call.parameters["gid"]!!.toLongOrNull() ?: throw NotANumberException("Gift id")

                        try {
                            userManager.removeGift(id, gid)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    /** CATEGORIES **/
                    get("/categories") {
                        val id = getUserId(call.parameters)

                        try {
                            val userCategories = userManager.getUserCategories(id)
                            call.respond(HttpStatusCode.OK, userCategories)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    get("/categories/{friendName}") {
                        val id = getUserId(call.parameters)
                        val friendName = call.parameters["friendName"]!!

                        try {
                            val userCategories = userManager.getFriendCategories(id, friendName)
                            call.respond(HttpStatusCode.OK, userCategories)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    put("/categories") {
                        val id = getUserId(call.parameters)

                        val category = Gson().fromJson(call.receiveText(), RestCategory::class.java)
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

                        val category = Gson().fromJson(call.receiveText(), RestCategory::class.java)
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
                    get("/friend-requests/sent") {
                        val id = getUserId(call.parameters)

                        try {
                            val requests = userManager.getInitiatedFriendRequest(id)
                            call.respond(HttpStatusCode.OK, requests)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    get("/friend-requests/received") {
                        val id = getUserId(call.parameters)

                        try {
                            val requests = userManager.getReceivedFriendRequest(id)
                            call.respond(HttpStatusCode.OK, requests)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    put("/friend-requests") {
                        val id = getUserId(call.parameters)

                        val receiveText = call.receiveText()
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
                }
            }
        }

        server.start(wait = true)
    }
}


fun getUserId(parameters: Parameters) : Long {
    return parameters["id"]!!.toLongOrNull() ?: throw NotANumberException("User id")
}