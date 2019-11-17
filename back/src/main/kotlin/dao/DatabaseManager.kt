package dao

import RestCategory
import RestGift
import java.sql.ResultSet
import java.time.LocalDate

data class DbUser(val id: Long, val name: String, val password: String, val picture: String?)
data class DbCategory(val id: Long, val userId: Long, val name: String)
data class DbGift(val id: Long, val userId: Long, val name: String, val description: String?, val price: String?, val whereToBuy: String?, val categoryId: Long, val picture: String?, val secret: Boolean)
enum class BuyAction { NONE, WANT_TO_BUY, BOUGHT }
data class DbFriendActionOnGift(val id: Long, val giftId: Long, val userId: Long, val interested: Boolean, val buy: BuyAction)
enum class RequestStatus { ACCEPTED, PENDING, REJECTED }
data class DbFriendRequest(val id: Long, val userOne: Long, val userTwo: Long, val status: RequestStatus)
data class DbEvent(val id: Long, val name: String, val creatorId: Long, val description: String, val endDate: LocalDate, val target: Long?) //target = -1 if ALL, userId other
data class DbParticipant(val id: Long, val eventId: Long, val userId: Long, val status: RequestStatus)

data class NakedUser(val name: String, val picture: String?)

class FriendRequestAlreadyExistException(val dbFriendRequest: DbFriendRequest) : Exception("Friend request already exists and is ${dbFriendRequest.status}.")



class DatabaseManager(dbPath: String) {
    companion object {
        private const val DEFAULT_CATEGORY_NAME = "Default"
    }

    private val conn = DbConnection(dbPath)
    private val usersAccessor = UsersAccessor(conn)
    private val categoryAccessor = CategoryAccessor(conn)
    private val giftAccessor = GiftAccessor(conn)
    private val friendActionOnGiftAccessor = FriendActionOnGiftAccessor(conn)
    private val friendRequestAccessor = FriendRequestAccessor(conn)
    private val eventsAccessor = EventsAccessor(conn)
    private val participantsAccessor = ParticipantsAccessor(conn)

    init {
        createDataModelIfNeeded()
    }

    //Here only for test purpose
    fun cleanTables() {
        conn.execute("delete from ${usersAccessor.getTableName()}")
        conn.execute("delete from ${categoryAccessor.getTableName()}")
        conn.execute("delete from ${giftAccessor.getTableName()}")
        conn.execute("delete from ${friendActionOnGiftAccessor.getTableName()}")
        conn.execute("delete from ${friendRequestAccessor.getTableName()}")
        conn.execute("delete from ${eventsAccessor.getTableName()}")
        conn.execute("delete from ${participantsAccessor.getTableName()}")
    }

    private fun createDataModelIfNeeded() {
        usersAccessor.createIfNotExists()
        categoryAccessor.createIfNotExists()
        giftAccessor.createIfNotExists()
        friendActionOnGiftAccessor.createIfNotExists()
        friendRequestAccessor.createIfNotExists()
        eventsAccessor.createIfNotExists()
        participantsAccessor.createIfNotExists()
    }

    /**
     * Users
     */
    @Synchronized fun addUser(userName: String, password: String, picture: String?): DbUser {
        val newUser = usersAccessor.addUser(userName, password, picture ?: "")
        addCategory(newUser.id, RestCategory(DEFAULT_CATEGORY_NAME))
        return newUser
    }

    @Synchronized fun getUser(userName: String): DbUser? {
        return usersAccessor.getUser(userName)
    }

    @Synchronized fun getUser(userId: Long): NakedUser? {
        return usersAccessor.getUser(userId)
    }

    @Synchronized fun modifyUser(userId: Long, name: String, picture: String?) {
        return usersAccessor.modifyUser(userId, name, picture ?: "")
    }

    /**
     * Gift
     */
    @Synchronized fun addGift(userId: Long, gift: RestGift, secret: Boolean) {
        if (gift.name == null) {
            throw Exception("Name could not be null")
        }
        if (gift.categoryId == null) {
            throw Exception("CategoryId could not be null, a default one exist")
        }

        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryAccessor.categoryExists(gift.categoryId)) throw Exception("Unknown category " + gift.categoryId)
        if (!categoryAccessor.categoryBelongToUser(userId, gift.categoryId)) throw Exception("Category " + gift.categoryId + " does not belong to user $userId")

        giftAccessor.addGift(userId, gift, secret)
    }

    @Synchronized fun getGift(giftId: Long) : DbGift? {
        if (!giftAccessor.giftExists(giftId)) throw Exception("Unknown gift $giftId")
        return giftAccessor.getGift(giftId)
    }

    /** Return gift for a given user, secret gift will be filter out */
    @Synchronized fun getUserGifts(userId: Long) : List<DbGift> {
        return giftAccessor.getUserGifts(userId)
    }

    /** Return gift for a given friend, secret gift will be returned */
    @Synchronized fun getFriendGifts(userId: Long, friendName: String): List<DbGift> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        val friend = getUser(friendName) ?: throw Exception("Unknown user name $friendName")
        friendRequestAccessor.getFriendRequest(userId, friend.id) ?: friendRequestAccessor.getFriendRequest(friend.id, userId) ?: throw Exception("You are not friend with $friendName")

        return giftAccessor.getFriendGifts(friend.id)
    }

    @Synchronized fun modifyGift(userId: Long, giftId: Long, gift: RestGift) {
        if (gift.name == null) {
            throw Exception("Name could not be null")
        }
        if (gift.categoryId == null) {
            throw Exception("CategoryId could not be null, a default one exist")
        }

        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!giftAccessor.giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!giftAccessor.giftBelongToUser(userId, giftId) && !giftAccessor.giftIsSecret(giftId)) throw Exception("Gift $giftId does not belong to user $userId and is not secret") /* secret gift could be modified by anyone */

        giftAccessor.modifyGift(giftId, gift)
    }

    @Synchronized fun removeGift(userId: Long, giftId: Long) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!giftAccessor.giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!giftAccessor.giftBelongToUser(userId, giftId) && !giftAccessor.giftIsSecret(giftId)) throw Exception("Gift $giftId does not belong to user $userId and is not secret") /* secret gift could be deleted by anyone */

        giftAccessor.removeGift(giftId)
    }

    /**
     * Gift Actions
     */
    @Synchronized fun interested(giftId: Long, userId: Long, interested: Boolean) {
        if (!giftAccessor.giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (giftAccessor.giftBelongToUser(userId, giftId)) throw Exception("Gift $giftId belong to you. I hope you are interested in.")

        friendActionOnGiftAccessor.interested(giftId, userId, interested)
    }

    @Synchronized fun buyAction(giftId: Long, userId: Long, buy: BuyAction) {
        if (!giftAccessor.giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (giftAccessor.giftBelongToUser(userId, giftId)) throw Exception("Gift $giftId belong to you. You cannot buy something at yourself.")

        friendActionOnGiftAccessor.buyAction(giftId, userId, buy)
    }

    @Synchronized fun getFriendActionOnGift(giftId: Long) : List<DbFriendActionOnGift> {
        if (!giftAccessor.giftExists(giftId)) throw Exception("Unknown gift $giftId")

        return friendActionOnGiftAccessor.getFriendActionOnGift(giftId)
    }

    @Synchronized fun getFriendActionOnGiftsUserHasActionOn(userId: Long) : List<DbFriendActionOnGift> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return friendActionOnGiftAccessor.getFriendActionOnGiftsUserHasActionOn(userId)
    }

    /**
     * Category
     */
    @Synchronized fun addCategory(userId: Long, category: RestCategory) {
        if (category.name == null) {
            throw Exception("Name could not be null")
        }
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        categoryAccessor.addCategory(userId, category)
    }

    @Synchronized fun getUserCategories(userId: Long) : List<DbCategory> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return categoryAccessor.getUserCategories(userId)
    }

    @Synchronized fun getFriendCategories(userId: Long, friendName: String): List<DbCategory> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        val friend = getUser(friendName) ?: throw Exception("Unknown user name $friendName")
        friendRequestAccessor.getFriendRequest(userId, friend.id) ?: friendRequestAccessor.getFriendRequest(friend.id, userId) ?: throw Exception("You are not friend with $friendName")

        return categoryAccessor.getFriendCategories(friend.id)
    }

    @Synchronized fun modifyCategory(userId: Long, categoryId: Long, category: RestCategory) {
        if (category.name == null) {
            throw Exception("Name could not be null")
        }

        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryAccessor.categoryExists(categoryId)) throw Exception("Unknown category $categoryId")
        if (!categoryAccessor.categoryBelongToUser(userId, categoryId)) throw Exception("Category $categoryId does not belong to user $userId")

        categoryAccessor.modifyCategory(categoryId, category)
    }

    @Synchronized fun removeCategory(userId: Long, categoryId: Long) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryAccessor.categoryExists(categoryId)) throw Exception("Unknown category $categoryId")
        if (!categoryAccessor.categoryBelongToUser(userId, categoryId)) throw Exception("Category $categoryId does not belong to user $userId")

        categoryAccessor.removeCategory(categoryId)
    }

    /**
     * Friend request
     */
    @Synchronized fun createFriendRequest(userOne: Long, userTwo: Long) {
        if (userOne == userTwo) throw Exception("You cannot be friend with yourself")
        if (!usersAccessor.userExists(userOne)) throw Exception("Unknown user $userOne")
        if (!usersAccessor.userExists(userTwo)) throw Exception("Unknown user $userTwo")

        val friendRequest = friendRequestAccessor.getFriendRequest(userOne, userTwo)
        if (friendRequest != null) throw FriendRequestAlreadyExistException(friendRequest)

        val receivedRequest = friendRequestAccessor.getFriendRequest(userTwo, userOne)
        if (receivedRequest != null) {
            when {
                receivedRequest.status == RequestStatus.REJECTED -> deleteFriendRequest(userTwo, receivedRequest.id)
                else -> throw FriendRequestAlreadyExistException(receivedRequest)
            }
        }

        friendRequestAccessor.createFriendRequest(userOne, userTwo)
    }

    @Synchronized fun getInitiatedFriendRequests(userId: Long) : List<DbFriendRequest> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return friendRequestAccessor.getInitiatedFriendRequests(userId)
    }

    @Synchronized fun getReceivedFriendRequests(userId: Long) : List<DbFriendRequest> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return friendRequestAccessor.getReceivedFriendRequests(userId)
    }

    @Synchronized fun deleteFriendRequest(userId: Long, friendRequestId: Long) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!friendRequestAccessor.friendRequestBelongToUser(userId, friendRequestId) && !friendRequestAccessor.friendRequestIsNotForUser(userId, friendRequestId)) throw Exception("Friend request $friendRequestId does not belong to user $userId")

        friendRequestAccessor.deleteFriendRequest(friendRequestId)
    }

    @Synchronized fun acceptFriendRequest(userId: Long, friendRequestId: Long) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!friendRequestAccessor.friendRequestIsNotForUser(userId, friendRequestId)) throw Exception("Friend request $friendRequestId is not targeting user $userId")

        friendRequestAccessor.acceptFriendRequest(friendRequestId)
    }

    @Synchronized fun declineFriendRequest(userId: Long, friendRequestId: Long, blockUser: Boolean) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!friendRequestAccessor.friendRequestIsNotForUser(userId, friendRequestId)) throw Exception("Friend request $friendRequestId is not targeting user $userId")

        friendRequestAccessor.declineFriendRequest(friendRequestId, blockUser)
    }

    /**
     * Events
     */
    @Synchronized fun createEventAllForAll(name: String, creatorId: Long, description: String?, endDate: LocalDate, participantIds: Set<Long>) {
        createEventAllForOne(name, creatorId, description, endDate, -1, participantIds)
    }

    @Synchronized fun createEventAllForOne(name: String, creatorId: Long, description: String?, endDate: LocalDate, target: Long, participantIds: Set<Long>) {
        if (target != -1L && !usersAccessor.userExists(target)) throw Exception("Unknown target user $target")

        val eventId = eventsAccessor.insertEvent(name, creatorId, description, endDate, target)

        addParticipants(eventId, participantIds)
        if (target != creatorId) acceptEventInvitation(creatorId, eventId)
    }

    fun deleteEvent(eventId: Long) {
        if (!eventsAccessor.eventExists(eventId)) throw Exception("Unknown event $eventId")
        eventsAccessor.deleteEvent(eventId)
    }

    @Synchronized fun addParticipants(eventId: Long, participantIds: Set<Long>) {
        if (!eventsAccessor.eventExists(eventId)) throw Exception("Unknown event $eventId")

        val unknownParticipantIds = arrayListOf<Long>()
        for (participantId: Long in participantIds) {
            if (!usersAccessor.userExists(participantId)) {
                unknownParticipantIds.add(participantId)
                continue
            }
            participantsAccessor.insert(eventId, participantId)
        }

        if (unknownParticipantIds.isNotEmpty()) throw Exception("Unknown participants $participantIds")
    }

    @Synchronized fun acceptEventInvitation(userId: Long, eventId: Long) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!eventsAccessor.eventExists(eventId)) throw Exception("Unknown event $eventId")
        if (!participantsAccessor.userInvitedToEvent(eventId, userId)) throw Exception("User $userId not invited to event $eventId")

        participantsAccessor.update(eventId, userId, RequestStatus.ACCEPTED)
    }

    @Synchronized fun declineEventInvitation(userId: Long, eventId: Long, blockInvites: Boolean) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!eventsAccessor.eventExists(eventId)) throw Exception("Unknown event $eventId")
        if (!participantsAccessor.userInvitedToEvent(eventId, userId)) throw Exception("User $userId not invited to event $eventId")

        if (blockInvites) {
            participantsAccessor.update(eventId, userId, RequestStatus.REJECTED)
        } else {
            participantsAccessor.delete(eventId, userId)
        }
    }

    @Synchronized fun getEventsById(eventId: Long) : DbEvent? {
        return eventsAccessor.getEventsById(eventId)
    }

    @Synchronized fun getEventsCreateBy(userId: Long) : List<DbEvent> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return eventsAccessor.getEventsCreateBy(userId)
    }

    @Synchronized fun getEventsNamed(name: String) : List<DbEvent> {
        return eventsAccessor.getEventsNamed(name)
    }

    @Synchronized fun getEventsAsParticipants(userId: Long) : List<DbParticipant>{
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return participantsAccessor.getEventsAsParticipants(userId)
    }

    @Synchronized fun getParticipants(eventId: Long) : List<DbParticipant> {
        if (!eventsAccessor.eventExists(eventId)) throw Exception("Unknown event $eventId")

        return participantsAccessor.getParticipants(eventId)
    }
}
