import java.time.LocalDate
import kotlin.Exception

/** RETURN CLASSES **/
data class User(val id: Long, val name: String, val picture: String?)
data class Friend(val name: String, val picture: String?)
data class Gift(val id: Long, val name: String, val description: String?, val price: String?, val whereToBuy: String?, val categoryId: Long, val picture: String?)
data class Category(val id: Long, val name: String)
data class CatAndGift(val category: Category, val gifts: List<Gift>)
data class FriendGift(val gift: Gift, val interestedUser: List<String>, val buyActionUser: Map<String, BuyAction>)
data class CatAndFriendGift(val category: Category, val gifts: List<FriendGift>)
data class FriendRequest(val id: Long, val otherUser: Friend)
data class PendingFriendRequest(val sent: List<FriendRequest>, val received: List<FriendRequest>)
data class Participant(val name: String, val status: RequestStatus)
data class Event(val id: Long, val type: EventType, val name: String, val creatorName: String, val description: String, val endDate: LocalDate, val target: String?, val participants: Set<Participant>)

/** INPUT CLASSES **/
data class ConnectionInformation(val name: String?, val password: String?)
data class UserInformation(val name: String?, val password: String?, val picture: String?)
data class RestGift(val name: String?, val description: String?, val price: String?, val whereToBuy: String?, val categoryId: Long?, val picture: String?)
data class RestCategory(val name: String?)
data class RestCreateFriendRequest(val name: String?)
enum class EventType { ALL_FOR_ALL, ALL_FOR_ONE }
data class RestCreateEvent(val type: EventType?, val name: String?, val description: String?, val endDate: LocalDate?, val target: Long?) //end date being epoch

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

    fun connect(connectionInformation: ConnectionInformation): User {
        if (connectionInformation.name == null) throw BadParamException("Username could not be null")
        if (connectionInformation.password == null) throw BadParamException("Password could not be null")

        val user = databaseManager.getUser(connectionInformation.name) ?: throw ConnectionException("Unknown user")
        if (user.password != connectionInformation.password) throw ConnectionException("Wrong password")

        return User(user.id, user.name, user.picture)
    }

    fun addUser(userInformation: UserInformation): User {
        if (userInformation.name == null) throw BadParamException("Name could not be null")
        if (userInformation.password == null) throw BadParamException("Password could not be null")
        if (databaseManager.getUser(userInformation.name) != null) throw CreateUserException("User already exists")

        val dbUser = databaseManager.addUser(userInformation.name, userInformation.password, userInformation.picture)
        return User(dbUser.id, dbUser.name, dbUser.picture)
    }

    private fun toGift(g: DbGift): Gift {
        return Gift(g.id, g.name, g.description, g.price, g.whereToBuy, g.categoryId, g.picture)
    }

    fun getUserGifts(userId: Long): List<CatAndGift> {
        val categories = databaseManager.getUserCategories(userId)
        val gifts = databaseManager.getUserGifts(userId)
        return categories.map { c ->
            CatAndGift(Category(c.id, c.name), gifts.filter { g -> g.categoryId == c.id }.map { g -> toGift(g) }) }
    }

    //Not optimal at all!
    fun getFriendGifts(userId: Long, friendName: String): List<CatAndFriendGift> {
        val categories = databaseManager.getFriendCategories(userId, friendName)
        val gifts = databaseManager.getFriendGifts(userId, friendName)

        val dummyUserCache = DummyUserCache(databaseManager) //Cache only by call
        return categories.map { c ->
            CatAndFriendGift(Category(c.id, c.name), gifts.filter { g -> g.categoryId == c.id }.map { g ->
                val actions = databaseManager.getFriendActionOnGift(g.id)
                FriendGift(toGift(g),
                    actions.filter { it.interested || dummyUserCache.queryName(it.userId) != null }.map { dummyUserCache.queryName(it.userId)!! },
                    actions.filter { it.buy != BuyAction.NONE  || dummyUserCache.queryName(it.userId) != null }.map { dummyUserCache.queryName(it.userId)!! to it.buy }.toMap() )
            })
        }
    }

    fun addGift(userId: Long, gift: RestGift) {
        databaseManager.addGift(userId, gift)
    }

    fun modifyGift(userId: Long, giftId: Long, gift: RestGift) {
        databaseManager.modifyGift(userId, giftId, gift)
    }

    fun removeGift(userId: Long, giftId: Long) {
        databaseManager.removeGift(userId, giftId)
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
        databaseManager.addCategory(userId, category)
    }

    fun modifyCategory(userId: Long, categoryId: Long, category: RestCategory) {
        databaseManager.modifyCategory(userId, categoryId, category)
    }

    fun removeCategory(userId: Long, categoryId: Long) {
        databaseManager.removeCategory(userId, categoryId)
    }


    fun createFriendRequest(userId: Long, otherUser: RestCreateFriendRequest) {
        if (otherUser.name == null) throw BadParamException("Username to send request to could not be null")

        val user = databaseManager.getUser(otherUser.name) ?: throw Exception("User ${otherUser.name} does not exist.")

        databaseManager.createFriendRequest(userId, user.id)
    }

    fun getFriends(userId: Long) : List<Friend> {
        val initiatedFriendRequests = databaseManager.getInitiatedFriendRequests(userId).filter { it.status == RequestStatus.ACCEPTED }
        val receivedFriendRequests = databaseManager.getReceivedFriendRequests(userId).filter { it.status == RequestStatus.ACCEPTED }

        val ini = initiatedFriendRequests.map { toFriend(it.userTwo) }
        val rec = receivedFriendRequests.map { toFriend(it.userOne) }

        val mut: MutableList<Friend> = mutableListOf()
        mut.addAll(ini)
        mut.addAll(rec)
        mut.sortBy { it.name }
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

        when {
            event.type == EventType.ALL_FOR_ALL -> databaseManager.createEventAllForAll(event.name, userId, event.description, event.endDate, setOf(userId))
            event.type == EventType.ALL_FOR_ONE -> databaseManager.createEventAllForOne(event.name, userId, event.description, event.endDate, event.target!!, if (userId != event.target) setOf(userId) else setOf())
            else -> throw Exception("Unknown type")
        }
    }

    fun deleteEvent(eventId: Long) {
        databaseManager.deleteEvent(eventId);
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
            if (dbEvent.target == null) null else dummyUserCache.queryName(dbEvent.target),
            databaseManager.getParticipants(dbEvent.id).filterNot { p -> dummyUserCache.queryName(p.userId) == null }.map { p -> Participant(dummyUserCache.queryName(p.userId)!!, p.status) }.toSet())
    }

}
