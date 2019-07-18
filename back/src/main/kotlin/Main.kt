import com.google.gson.Gson
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

data class Error(val error: String)
data class FriendRequestConflict(val ownRequest: Boolean, val status: RequestStatus, val message: String)

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
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@get
                        }

                        try {
                            val userGifts = userManager.getUserGifts(id)
                            call.respond(HttpStatusCode.OK, userGifts)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    put("/gifts") {
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@put
                        }

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
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@patch
                        }

                        val gid = call.parameters["gid"]!!.toLongOrNull()
                        if (gid == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided Gift ID must be a number."))
                            return@patch
                        }

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
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@delete
                        }

                        val gid = call.parameters["gid"]!!.toLongOrNull()
                        if (gid == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided Gift ID must be a number."))
                            return@delete
                        }

                        try {
                            userManager.removeGift(id, gid)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    /** CATEGORIES **/

                    get("/categories") {
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@get
                        }

                        try {
                            val userCategories = userManager.getUserCategories(id)
                            call.respond(HttpStatusCode.OK, userCategories)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    put("/categories") {
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@put
                        }

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
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@patch
                        }

                        val gid = call.parameters["cid"]!!.toLongOrNull()
                        if (gid == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided Category ID must be a number."))
                            return@patch
                        }

                        val category = Gson().fromJson(call.receiveText(), RestCategory::class.java)
                        if (category.name == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("missing name or password node in json"))
                            return@patch
                        }

                        try {
                            userManager.modifyCategory(id, gid, category)
                            call.respond(HttpStatusCode.OK)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    delete("/categories/{cid}") {
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@delete
                        }

                        val gid = call.parameters["cid"]!!.toLongOrNull()
                        if (gid == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided Category ID must be a number."))
                            return@delete
                        }

                        try {
                            userManager.removeCategory(id, gid)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }

                    /** FRIEND REQUEST **/
                    get("/friend-requests/sent") {
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@get
                        }

                        try {
                            val requests = userManager.getInitiatedFriendRequest(id)
                            call.respond(HttpStatusCode.OK, requests)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    get("/friend-requests/received") {
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@get
                        }

                        try {
                            val requests = userManager.getReceivedFriendRequest(id)
                            call.respond(HttpStatusCode.OK, requests)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    put("/friend-requests") {
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@put
                        }

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
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@delete
                        }

                        val fid = call.parameters["fid"]!!.toLongOrNull()
                        if (fid == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided Friend Request ID must be a number."))
                            return@delete
                        }

                        try {
                            userManager.deleteFriendRequest(id, fid)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    //not convince by get
                    get("/friend-requests/{fid}/decline") {
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@get
                        }

                        val fid = call.parameters["fid"]!!.toLongOrNull()
                        if (fid == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided Friend Request ID must be a number."))
                            return@get
                        }

                        try {
                            userManager.declineFriendRequest(id, fid, false)
                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, Error(e.message!!))
                        }
                    }
                    //not convince by get
                    get("/friend-requests/{fid}/accept") {
                        val id = call.parameters["id"]!!.toLongOrNull()
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided User ID must be a number."))
                            return@get
                        }

                        val fid = call.parameters["fid"]!!.toLongOrNull()
                        if (fid == null) {
                            call.respond(HttpStatusCode.BadRequest, Error("Provided Friend Request ID must be a number."))
                            return@get
                        }

                        try {
                            userManager.acceptFriendRequest(id, fid)
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
