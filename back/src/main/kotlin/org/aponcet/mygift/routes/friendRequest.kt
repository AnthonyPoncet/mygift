package org.aponcet.mygift.routes

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.aponcet.mygift.ErrorAnswer
import org.aponcet.mygift.RestCreateFriendRequest
import org.aponcet.mygift.UserManager
import org.aponcet.mygift.dbmanager.FriendRequestAlreadyExistException
import org.aponcet.mygift.dbmanager.RequestStatus

private data class FriendRequestConflict(val ownRequest: Boolean, val status: RequestStatus, val message: String)

fun Route.friendRequest(userManager: UserManager) {
    authenticate {
        get("/friends") {
            handle(call) { id ->
                call.respond(HttpStatusCode.OK, userManager.getFriends(id))
            }
        }

        route("/friend-requests") {
            get("/pending") {
                handle(call) { id ->
                    val requests = userManager.getPendingFriendRequests(id)
                    call.respond(HttpStatusCode.OK, requests)
                }
            }
            get("/received-blocked") {
                handle(call) { id ->
                    val requests = userManager.getReceivedBlockedFriendRequests(id)
                    call.respond(HttpStatusCode.OK, requests)
                }
            }
            put {
                handle(call) { id ->
                    val friendRequest =
                        Gson().fromJson(decode(call.receiveText()), RestCreateFriendRequest::class.java)
                    if (friendRequest.name == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorAnswer("missing name node in json"))
                    } else {
                        try {
                            userManager.createFriendRequest(id, friendRequest)
                            call.respond(HttpStatusCode.OK)
                        } catch (e: FriendRequestAlreadyExistException) {
                            call.respond(
                                HttpStatusCode.Conflict,
                                FriendRequestConflict(
                                    id == e.dbFriendRequest.userOne,
                                    e.dbFriendRequest.status,
                                    e.message!!
                                )
                            )
                        }
                    }
                }
            }
            delete("/{fid}") {
                val fid =
                    call.parameters["fid"]!!.toLongOrNull() ?: throw NumberFormatException("Friend Request id")
                handle(call) { id ->
                    userManager.deleteFriendRequest(id, fid)
                    call.respond(HttpStatusCode.Accepted)
                }
            }
            //not convince by get
            get("/{fid}/accept") {
                val fid =
                    call.parameters["fid"]!!.toLongOrNull() ?: throw NumberFormatException("Friend Request id")
                handle(call) { id ->
                    userManager.acceptFriendRequest(id, fid)
                    call.respond(HttpStatusCode.Accepted)
                }
            }
            post("/{fid}/decline") {
                val fid =
                    call.parameters["fid"]!!.toLongOrNull() ?: throw NumberFormatException("Friend Request id")
                handle(call) { id ->
                    val blockUser = (call.request.queryParameters["blockUser"] ?: "false").toBoolean()
                    userManager.declineFriendRequest(id, fid, blockUser)
                    call.respond(HttpStatusCode.Accepted)
                }
            }
        }
    }
}