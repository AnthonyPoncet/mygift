package org.aponcet.mygift

//TODO: remove linked to db model?
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.HttpStatusCode
import org.aponcet.authserver.ErrorResponse
import org.aponcet.authserver.TokenResponse
import org.aponcet.authserver.UserJson
import org.aponcet.mygift.dbmanager.*
import java.time.LocalDate

/** RETURN CLASSES **/
data class User(val token: String, val name: String, val picture: String?)
data class Friend(val name: String, val picture: String?)
data class Gift(val id: Long, val name: String, val description: String?, val price: String?, val whereToBuy: String?, val categoryId: Long, val picture: String?)
data class Category(val id: Long, val name: String, val rank: Long)
data class CatAndGift(val category: Category, val gifts: List<Gift>)
data class FriendGift(val gift: Gift, val interestedUser: List<String>, val buyActionUser: Map<String, BuyAction>, val secret: Boolean)
data class CatAndFriendGift(val category: Category, val gifts: List<FriendGift>)
data class FriendRequest(val id: Long, val otherUser: Friend)
data class PendingFriendRequest(val sent: List<FriendRequest>, val received: List<FriendRequest>)
data class Participant(val name: String, val status: RequestStatus)
data class Event(val id: Long, val type: EventType, val name: String, val creatorName: String, val description: String, val endDate: LocalDate, val target: String?, val participants: Set<Participant>)
data class BuyListByFriend(val friendName: String, val gifts: List<FriendGift>)

/** INPUT CLASSES **/
data class UserInformation(val name: String?, val password: String?, val picture: String?)
data class UserModification(val name: String?, val picture: String?)
data class RestGift(val name: String?, val description: String?, val price: String?, val whereToBuy: String?, val categoryId: Long?, val picture: String?, val rank: Long?)
data class RestCategory(val name: String?, val rank: Long?)
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

class DummyEventCache(private val databaseManager: DatabaseManager) {
    private val cacheIds = HashMap<Long, DbEvent?>()

    fun query(eventId: Long) : DbEvent? {
        val event = cacheIds[eventId]
        return if (event != null) event
        else {
            val dbEvent = databaseManager.getEventsById(eventId)
            cacheIds[eventId] = dbEvent
            cacheIds[eventId]
        }
    }

}


class UserManager(private val databaseManager: DatabaseManager) {

    suspend fun connect(userJson: UserJson): User {
        if (userJson.name == null) throw BadParamException("Username could not be null")
        if (userJson.password == null) throw BadParamException("Password could not be null")

        val client = HttpClient(Apache)
        try {
            val response = client.post<HttpResponse> {
                url("http://127.0.0.1:9876/login")
                body = Gson().toJson(userJson)
            }

            val tokenResponse = Gson().fromJson(response.readText(), TokenResponse::class.java)
            val name = userJson.name!!
            val user = databaseManager.getUser(name)!! //to get picture, if should come from here
            return User(tokenResponse.token, user.name, user.picture)
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.Unauthorized) throw ConnectionException(Gson().fromJson(e.response.readText(), ErrorResponse::class.java).error)
            System.err.println("Error while authenticate: $e")
            throw IllegalStateException(e)
        }
    }

    suspend fun addUser(userInformation: UserInformation): User {
        if (userInformation.name == null) throw BadParamException("Name could not be null")
        if (userInformation.password == null) throw BadParamException("Password could not be null")
        if (databaseManager.getUser(userInformation.name) != null) throw CreateUserException("User already exists")

        databaseManager.addUser(userInformation.name, userInformation.password, userInformation.picture)
        return connect(UserJson(userInformation.name, userInformation.password))
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
            g.picture
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

        val gifts = friendActionOnGiftsUserHasActionOn
            .filter { g -> g.buy != BuyAction.NONE }
            .map { g -> databaseManager.getGift(g.giftId) }

        val map = gifts
            .filterNotNull()
            .groupBy({ it.userId }, { it })

        val dummyUserCache = DummyUserCache(databaseManager) //Cache only by call
        return map
            .filter { dummyUserCache.queryName(it.key) != null }
            .map { m ->
                BuyListByFriend(
                    dummyUserCache.queryName(m.key)!!,
                    m.value.map { g ->
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

    fun removeGift(userId: Long, giftId: Long) {
        databaseManager.removeGift(userId, giftId)
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

    fun addCategory(userId: Long, category: RestCategory) {
        databaseManager.addCategory(userId, toNewCategory(category))
    }

    private fun validateRestCategory(category: RestCategory, withRank: Boolean) {
        if (category.name == null) {
            throw BadParamException("Category JSON need to contain 'name' node")
        }
        if (withRank && category.rank == null) {
            throw BadParamException("Gift JSON need to contain 'name' node")
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

    fun createEvent(event: RestCreateEvent, userId: Long) {
        if (event.type == null) throw Exception("Event type is mandatory")
        if (event.name == null) throw Exception("Event Name is mandatory")
        if (event.endDate == null) throw Exception("Event End Date is mandatory")
        if (event.type == EventType.ALL_FOR_ONE && event.target == null) throw Exception("Event End Date is mandatory")

        when (event.type) {
            EventType.ALL_FOR_ALL -> databaseManager.createEventAllForAll(event.name, userId, event.description, event.endDate, setOf(userId))
            EventType.ALL_FOR_ONE -> databaseManager.createEventAllForOne(event.name, userId, event.description, event.endDate, event.target!!, if (userId != event.target) setOf(userId) else setOf())
        }
    }

    fun deleteEvent(eventId: Long) {
        databaseManager.deleteEvent(eventId)
    }

    fun addParticipants(eventId: Long, participants: Set<String>) {
        val dummyUserCache = DummyUserCache(databaseManager)
        databaseManager.addParticipants(eventId, participants.filterNot { dummyUserCache.queryId(it) == -1L }.map { dummyUserCache.queryId(it) }.toSet())
    }

    fun acceptEventInvitation(userId: Long, eventId: Long) {
        databaseManager.acceptEventInvitation(userId, eventId)
    }

    fun declineEventInvitation(userId: Long, eventId: Long, blockInvites: Boolean) {
        databaseManager.declineEventInvitation(userId, eventId, blockInvites)
    }

    fun getEvent(eventId: Long): Event {
        val dbEvent = databaseManager.getEventsById(eventId) ?: throw Exception("Unknown event with id $eventId")
        val dummyUserCache = DummyUserCache(databaseManager)
        return toEvent(dbEvent, dummyUserCache)
    }

    fun getEventsCreateBy(userId: Long) : List<Event> {
        return toEvents(databaseManager.getEventsCreateBy(userId))
    }

    fun getEventsNamed(name: String)  : List<Event> {
        return toEvents(databaseManager.getEventsNamed(name))
    }

    fun getEventsAsParticipants(userId: Long) : List<Event>{
        val dummyUserCache = DummyUserCache(databaseManager)
        val dummyEventCache = DummyEventCache(databaseManager)

        val events = databaseManager.getEventsAsParticipants(userId)
            .filterNot { dummyUserCache.queryName(it.userId) == null  }
            .filterNot { dummyEventCache.query(it.eventId) == null }
            .map { dummyEventCache.query(it.eventId)!! }

        return toEvents(events)
    }

    private fun toEvents(dbEvents: List<DbEvent>) : List<Event> {
        val dummyUserCache = DummyUserCache(databaseManager)

        return dbEvents
            .filterNot { dummyUserCache.queryName(it.creatorId) == null  }
            .map { toEvent(it, dummyUserCache) }
    }

    private fun toEvent(dbEvent: DbEvent, dummyUserCache: DummyUserCache) : Event {
        return Event(dbEvent.id,
            if (dbEvent.target == null) EventType.ALL_FOR_ALL else EventType.ALL_FOR_ONE,
            dbEvent.name,
            dummyUserCache.queryName(dbEvent.creatorId)!!,
            dbEvent.description,
            dbEvent.endDate,
            if (dbEvent.target == null) null else dummyUserCache.queryName(dbEvent.target as Long),
            databaseManager.getParticipants(dbEvent.id).filterNot { p -> dummyUserCache.queryName(p.userId) == null }.map { p ->
                Participant(
                    dummyUserCache.queryName(p.userId)!!,
                    p.status
                )
            }.toSet()
        )
    }
}
