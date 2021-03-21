package org.aponcet.mygift

//TODO: remove linked to db model?
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.ResponseException
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.request.post
import io.ktor.client.statement.*
import io.ktor.http.*
import org.aponcet.authserver.*
import org.aponcet.mygift.dbmanager.*
import org.aponcet.mygift.model.AuthServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

/** RETURN CLASSES **/
data class User(val token: String, val name: String, val picture: String?)
data class Friend(val name: String, val picture: String?)
data class Gift(val id: Long, val name: String, val description: String?, val price: String?, val whereToBuy: String?, val categoryId: Long, val picture: String?, val rank: Long)
data class Category(val id: Long, val name: String, val rank: Long)
data class CatAndGift(val category: Category, val gifts: List<Gift>)
data class FriendGift(val gift: Gift, val interestedUser: List<String>, val buyActionUser: Map<String, BuyAction>, val secret: Boolean)
data class CatAndFriendGift(val category: Category, val gifts: List<FriendGift>)
data class FriendRequest(val id: Long, val otherUser: Friend)
data class PendingFriendRequest(val sent: List<FriendRequest>, val received: List<FriendRequest>)
data class Participant(val name: String, val status: RequestStatus)
data class Event(val id: Long, val type: EventType, val name: String, val creatorName: String, val description: String, val endDate: LocalDate, val target: String?, val participants: Set<Participant>)
data class BuyListByFriend(val friendName: String, val gifts: List<FriendGift>, val deletedGifts: List<DeletedGifts>)
data class DeletedGifts(val id: Long, val name: String, val description: String?, val price: String?, val whereToBuy: String?, val picture: String?, val buyAction: BuyAction, val status: Status)
data class ResetPassword(val userId: Long, val uuid: String, val expiry: LocalDateTime)

/** INPUT CLASSES **/
data class UserModification(val name: String?, val picture: String?)
data class RestGift(val name: String?, val description: String?, val price: String?, val whereToBuy: String?, val categoryId: Long?, val picture: String?, val rank: Long?)
data class RestCategory(val name: String?, val rank: Long?, val share: List<String>?)
data class RestCreateFriendRequest(val name: String?)
enum class EventType { ALL_FOR_ALL, ALL_FOR_ONE }
data class RestCreateEvent(val type: EventType?, val name: String?, val description: String?, val endDate: LocalDate?, val target: Long?) //end date being epoch
enum class RankAction { DOWN, UP }

/** Exceptions **/
class BadParamException(val error: String) : Exception("Bad parameter $error")
class ConnectionException(val error: String) : Exception("Unable to connect. Cause: $error")
class CreateUserException(val error: String) : Exception("Unable to create user. Cause: $error")

/**
 * No cleaning notion, take care of its validity
 */
class DummyUserCache(private val databaseManager: DatabaseManager) {
    private val cacheIds = HashMap<Long, String?>()
    private val cacheNames = HashMap<String, Long>()

    fun queryName(userId: Long) : String? {
        val name = cacheIds[userId]
        return if (name != null) name
        else {
            cacheIds[userId] = databaseManager.getUser(userId)?.name
            cacheIds[userId]
        }
    }

    fun queryId(userName: String) : Long {
        val id = cacheNames[userName]
        return if (id != null) id
        else {
            val dbUser = databaseManager.getUser(userName)
            cacheNames[userName] = dbUser?.id ?: -1
            cacheNames[userName]!!
        }
    }
}


class UserManager(private val databaseManager: DatabaseManager, private val authServer: AuthServer) {

    companion object {
        val LOGGER : Logger = LoggerFactory.getLogger(UserManager::class.java)
    }

    suspend fun connect(userJson: UserJson): User {
        if (userJson.name == null) throw BadParamException("Username could not be null")
        if (userJson.password == null) throw BadParamException("Password could not be null")

        val client = HttpClient(Apache){
            install(JsonFeature){
                serializer = GsonSerializer()
            }
        }
        try {
            val tokenResponse = client.post<TokenResponse> {
                url("http://${authServer.host}:${authServer.port}/login")
                body = Gson().toJson(userJson)
            }

            val name = userJson.name!!
            val user = databaseManager.getUser(name)!! //to get picture, if should come from here
            return User(tokenResponse.token, user.name, user.picture)
        } catch (e: ResponseException) {
            val response = e.response

            if (response.status == HttpStatusCode.Unauthorized) {
                val error = Gson().fromJson(response.readText(), ErrorResponse::class.java).error
                LOGGER.error("Error while authenticate '${error}'", e)
                throw ConnectionException(error)
            }

            LOGGER.error("Error while creating user '${e.message}'", e)
            throw IllegalStateException(e)
        }
    }

    suspend fun addUser(userAndPictureJson: UserAndPictureJson): User {
        val client = HttpClient(Apache)
        try {
            client.put<HttpStatement> {
                url("http://${authServer.host}:${authServer.port}/create")
                body = Gson().toJson(userAndPictureJson)
            }.execute()

            return connect(UserJson(userAndPictureJson.name, userAndPictureJson.password))
        } catch (e: ResponseException) {
            val response = e.response

            if (response.status == HttpStatusCode.Unauthorized) {
                val error = Gson().fromJson(response.readText(), ErrorResponse::class.java).error
                LOGGER.error("Error while creating user '${error}'", e)
                throw ConnectionException(error)
            }
            if (response.status == HttpStatusCode.Conflict) {
                val readText = response.readText()
                val error = Gson().fromJson(readText, ErrorResponse::class.java).error
                LOGGER.error("Error while creating user '${error}'", e)
                throw CreateUserException(error)
            }

            LOGGER.error("Error while creating user '${e.message}'", e)
            throw IllegalStateException(e)
        }
    }

    fun modifyUser(userId: Long, userModification: UserModification): User {
        if (userModification.name == null) throw BadParamException("Name could not be null")
        databaseManager.getUser(userId) ?: throw CreateUserException("User does not exists")

        databaseManager.modifyUser(userId, userModification.name, userModification.picture)
        return User("", userModification.name, userModification.picture)
    }

    private fun toGift(g: DbGift): Gift {
        return Gift(
            g.id,
            g.name,
            g.description,
            g.price,
            g.whereToBuy,
            g.categoryId,
            g.picture,
            g.rank
        )
    }

    fun getUserGifts(userId: Long): List<CatAndGift> {
        val categories = databaseManager.getUserCategories(userId)
        val gifts = databaseManager.getUserGifts(userId)
        return categories.map { c ->
            CatAndGift(
                Category(
                    c.id,
                    c.name,
                    c.rank
                ), gifts.filter { g -> g.categoryId == c.id }.map { g -> toGift(g) })
        }
    }

    //Not optimal at all!
    fun getFriendGifts(userId: Long, friendName: String): List<CatAndFriendGift> {
        val categories = databaseManager.getFriendCategories(userId, friendName)
        val gifts = databaseManager.getFriendGifts(userId, friendName)

        val dummyUserCache = DummyUserCache(databaseManager) //Cache only by call
        return categories.map { c ->
            CatAndFriendGift(
                Category(
                    c.id,
                    c.name,
                    c.rank
                ), gifts.filter { g -> g.categoryId == c.id }.map { g ->
                    val actions = databaseManager.getFriendActionOnGift(g.id)
                    FriendGift(toGift(g),
                        actions.filter { it.interested || dummyUserCache.queryName(it.userId) != null }.map {
                            dummyUserCache.queryName(
                                it.userId
                            )!!
                        },
                        actions.filter { it.buy != BuyAction.NONE || dummyUserCache.queryName(it.userId) != null }.map {
                            dummyUserCache.queryName(
                                it.userId
                            )!! to it.buy
                        }.toMap(),
                        g.secret
                    )
                })
        }
    }

    fun getBuyList(userId: Long) : List<BuyListByFriend> {
        val friendActionOnGiftsUserHasActionOn = databaseManager.getFriendActionOnGiftsUserHasActionOn(userId)

        val dbGifts = friendActionOnGiftsUserHasActionOn
            .filter { g -> g.buy != BuyAction.NONE }
            .mapNotNull { g -> databaseManager.getGift(g.giftId) }

        val giftsAndUser = ArrayList<Pair<Long, DbGift>>()
        for (gift in dbGifts) {
            val usersFromCategory = databaseManager.getUsersFromCategory(gift.categoryId)
            usersFromCategory.forEach { giftsAndUser.add(Pair(it, gift)) }
        }

        val gifts = giftsAndUser.groupBy( { it.first }, { it.second } )

        val dummyUserCache = DummyUserCache(databaseManager) //Cache only by call

        val deletedGiftsUserHasActionOn = databaseManager.getDeletedGiftsUserHasActionOn(userId)
        val deletedGiftByFriend = deletedGiftsUserHasActionOn.groupBy({ it.giftUserId }, { DeletedGifts(it.giftId, it.name, it.description, it.price, it.whereToBuy, it.picture, it.friendAction, it.giftUserStatus) })

        val map = (gifts.keys + deletedGiftByFriend.keys).associateWith { Pair(if (gifts[it] == null) emptyList() else gifts[it]!!, if (deletedGiftByFriend[it] == null) emptyList() else deletedGiftByFriend[it]!!) }

        return map
            .filter { dummyUserCache.queryName(it.key) != null }
            .map { m ->
                BuyListByFriend(
                    dummyUserCache.queryName(m.key)!!,
                    m.value.first.map { g ->
                        val actions = databaseManager.getFriendActionOnGift(g.id)
                        FriendGift(toGift(g),
                            actions.filter { it.interested || dummyUserCache.queryName(it.userId) != null }.map { dummyUserCache.queryName(it.userId)!! },
                            actions.filter { it.buy != BuyAction.NONE || dummyUserCache.queryName(it.userId) != null }.map { dummyUserCache.queryName(it.userId)!! to it.buy }.toMap(),
                            g.secret
                        )
                    }.sortedBy { it.gift.rank },
                    m.value.second.sortedBy { it.name })
            }.sortedBy { it.friendName }
    }

    fun deleteDeletedGift(giftId: Long, friendId: Long) {
        return databaseManager.deleteDeletedGift(giftId, friendId)
    }

    fun addGift(userId: Long, gift: RestGift) {
        databaseManager.addGift(userId, toNewGift(gift), false)
    }

    fun addSecretGift(userId: Long, friendName: String, gift: RestGift) {
        val initiatedRequest = getInitiatedRequest(userId, RequestStatus.ACCEPTED)
        val receivedRequest = getReceivedRequest(userId, RequestStatus.ACCEPTED)

        if (initiatedRequest.none { it.otherUser.name == friendName } && receivedRequest.none { it.otherUser.name == friendName }) {
            throw Exception("You are not friend with $friendName.")
        }

        val friendUserId = (databaseManager.getUser(friendName) ?: throw Exception("No user named $friendName.")).id
        databaseManager.addGift(friendUserId, toNewGift(gift), true)
    }

    private fun validateRestGift(restGift: RestGift, withRank: Boolean) {
        if (restGift.name == null) {
            throw BadParamException("Gift JSON need to contain 'name' node")
        }
        if (restGift.categoryId == null) {
            throw BadParamException("Gift JSON need to contain 'categoryId' node")
        }
        if (withRank && restGift.rank == null) {
            throw BadParamException("Gift JSON need to contain 'rank' node")
        }
    }

    private fun toNewGift(restGift: RestGift): NewGift {
        validateRestGift(restGift, false)
        return NewGift(restGift.name!!, restGift.description, restGift.price, restGift.whereToBuy, restGift.categoryId!!, restGift.picture)
    }

    fun modifyGift(userId: Long, giftId: Long, gift: RestGift) {
        databaseManager.modifyGift(userId, giftId, toGift(gift))
    }

    private fun toGift(restGift: RestGift) : org.aponcet.mygift.dbmanager.Gift {
        validateRestGift(restGift, true)
        return Gift(restGift.name!!, restGift.description, restGift.price, restGift.whereToBuy, restGift.categoryId!!, restGift.picture, restGift.rank!!)
    }

    fun removeGift(userId: Long, giftId: Long, status: Status) {
        databaseManager.removeGift(userId, giftId, status)
    }

    fun changeGiftRank(userId: Long, giftId: Long, rankAction: RankAction) {
        when (rankAction) {
            RankAction.DOWN -> databaseManager.rankDownGift(userId, giftId)
            RankAction.UP -> databaseManager.rankUpGift(userId, giftId)
        }
    }

    fun interested(giftId: Long, userId: Long) {
        databaseManager.interested(giftId, userId, true)
    }

    fun notInterested(giftId: Long, userId: Long) {
        databaseManager.interested(giftId, userId, false)
    }

    fun buy(giftId: Long, userId: Long, buy: BuyAction) {
        databaseManager.buyAction(giftId, userId, buy)
    }

    fun stopBuy(giftId: Long, userId: Long) {
        databaseManager.buyAction(giftId, userId, BuyAction.NONE)
    }

    fun addCategory(category: RestCategory, userId: Long, userNames: List<String>) {
        val users = userNames.map { databaseManager.getUser(it) }
        if (users.any { it == null }) throw BadParamException("At leats one passed user does not exist")
        val userIds = users.map { it!!.id }
        databaseManager.addCategory(toNewCategory(category), userIds.plus(userId))
    }

    private fun validateRestCategory(category: RestCategory, withRank: Boolean) {
        if (category.name == null) {
            throw BadParamException("Category JSON need to contain 'name' node")
        }
        if (withRank && category.rank == null) {
            throw BadParamException("Category JSON need to contain 'rank' node")
        }
    }

    private fun toNewCategory(category: RestCategory) : NewCategory {
        validateRestCategory(category, false)
        return NewCategory(category.name!!)
    }

    fun modifyCategory(userId: Long, categoryId: Long, category: RestCategory) {
        databaseManager.modifyCategory(userId, categoryId, toCategory(category))
    }

    private fun toCategory(category: RestCategory) : org.aponcet.mygift.dbmanager.Category {
        validateRestCategory(category, true)
        return Category(category.name!!, category.rank!!)
    }

    fun removeCategory(userId: Long, categoryId: Long) {
        databaseManager.removeCategory(userId, categoryId)
    }

    fun changeCategoryRank(userId: Long, categoryId: Long, rankAction: RankAction) {
        when (rankAction) {
            RankAction.DOWN -> databaseManager.rankDownCategory(userId, categoryId)
            RankAction.UP -> databaseManager.rankUpCategory(userId, categoryId)
        }
    }

    fun createFriendRequest(userId: Long, otherUser: RestCreateFriendRequest) {
        if (otherUser.name == null) throw BadParamException("Username to send request to could not be null")

        val user = databaseManager.getUser(otherUser.name) ?: throw Exception("User ${otherUser.name} does not exist.")

        databaseManager.createFriendRequest(userId, user.id)
    }

    fun getFriends(userId: Long) : List<FriendRequest> {
        val initiatedFriendRequests = databaseManager.getInitiatedFriendRequests(userId).filter { it.status == RequestStatus.ACCEPTED }
        val receivedFriendRequests = databaseManager.getReceivedFriendRequests(userId).filter { it.status == RequestStatus.ACCEPTED }

        val ini = initiatedFriendRequests.map {
            FriendRequest(
                it.id,
                toFriend(it.userTwo)
            )
        }
        val rec = receivedFriendRequests.map {
            FriendRequest(
                it.id,
                toFriend(it.userOne)
            )
        }

        val mut: MutableList<FriendRequest> = mutableListOf()
        mut.addAll(ini)
        mut.addAll(rec)
        mut.sortBy { it.otherUser.name }
        return mut
    }

    private fun getInitiatedRequest(userId: Long, status: RequestStatus) : List<FriendRequest> {
        return databaseManager.getInitiatedFriendRequests(userId)
            .filter { it.status == status }
            .map { FriendRequest(it.id, toFriend(it.userTwo)) }
    }
    private fun getReceivedRequest(userId: Long, status: RequestStatus) : List<FriendRequest> {
        return databaseManager.getReceivedFriendRequests(userId)
            .filter { it.status == status }
            .map { FriendRequest(it.id, toFriend(it.userOne)) }
    }

    fun getPendingFriendRequests(userId: Long): PendingFriendRequest {
        val initiated = getInitiatedRequest(userId, RequestStatus.PENDING).sortedBy { it.otherUser.name }
        val received = getReceivedRequest(userId, RequestStatus.PENDING).sortedBy { it.otherUser.name }

        return PendingFriendRequest(initiated, received)
    }

    fun getReceivedBlockedFriendRequests(userId: Long): List<FriendRequest> {
        return getReceivedRequest(userId, RequestStatus.REJECTED)
    }

    fun deleteFriendRequest(userId: Long, friendRequestId: Long) {
        databaseManager.deleteFriendRequest(userId, friendRequestId)
    }

    fun acceptFriendRequest(userId: Long, friendRequestId: Long) {
        databaseManager.acceptFriendRequest(userId, friendRequestId)
    }

    fun declineFriendRequest(userId: Long, friendRequestId: Long, blockUser: Boolean) {
        databaseManager.declineFriendRequest(userId, friendRequestId, blockUser)
    }

    private fun toFriend(userId: Long) : Friend {
        val user = databaseManager.getUser(userId)!!
        return Friend(user.name, user.picture)
    }

    fun getEntry(uuid: String) : ResetPassword {
        val entry = databaseManager.getEntry(uuid)
        return ResetPassword(entry.userId, entry.uuid, entry.expiry)
    }

    suspend fun modifyPassword(userAndResetPassword: UserAndResetPassword, uuid: String) {
        if (userAndResetPassword.name == null) throw BadParamException("Username could not be null")
        if (userAndResetPassword.password == null) throw BadParamException("Password could not be null")

        val entry = databaseManager.getEntry(uuid)
        val user = databaseManager.getUser(userAndResetPassword.name!!)
        if (entry.userId != user?.id ?: -1L) {
            databaseManager.deleteEntry(entry.userId, uuid)
            throw Exception("This uuid does not belong to you. Reset password uuid have been deleted.")
        }

        //send modify to authserver
        val client = HttpClient(Apache)
        try {
            client.post<HttpStatement> {
                url("http://${authServer.host}:${authServer.port}/update")
                body = Gson().toJson(userAndResetPassword)
            }.execute()
        } catch (e: ResponseException) {
            LOGGER.error("Error while updating user: $e")
            throw IllegalStateException(e)
        }

        databaseManager.deleteEntry(entry.userId, uuid)
    }
}
