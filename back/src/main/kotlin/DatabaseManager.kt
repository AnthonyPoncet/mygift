import java.sql.Connection
import java.sql.DriverManager
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

class DbConnection(dbPath: String) {
    private var conn: Connection
    init {
        val url = "jdbc:sqlite:$dbPath"
        conn = DriverManager.getConnection(url)
    }

    fun execute(query: String) {
        val statement = conn.createStatement()
        statement.execute(query)
    }

    fun executeQuery(query: String): ResultSet {
        val statement = conn.createStatement()
        return statement.executeQuery(query)
    }

    fun executeUpdate(query: String) {
        val statement = conn.createStatement()
        statement.executeUpdate(query)
    }

    fun close() {
        conn.close()
    }
}

class DatabaseManager(dbPath: String) {
    companion object {
        private const val DEFAULT_CATEGORY_NAME = "Default"
    }

    private var conn = DbConnection(dbPath)

    init {
        createDataModelIfNeeded()
    }

    //Here only for test purpose
    fun cleanTables() {
        conn.execute("delete from users")
        conn.execute("delete from categories")
        conn.execute("delete from gifts")
        conn.execute("delete from friendActionOnGift")
        conn.execute("delete from friendRequest")
        conn.execute("delete from events")
        conn.execute("delete from participants")
    }

    private fun createDataModelIfNeeded() {
        conn.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id         INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name       TEXT NOT NULL, " +
                "password   TEXT NOT NULL, " +
                "picture    TEXT)")
        conn.execute("CREATE TABLE IF NOT EXISTS categories (" +
                "id         INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId     INTEGER NOT NULL, " +
                "name       TEXT NOT NULL, " +
                "FOREIGN KEY(userId) REFERENCES users(id))")
        conn.execute("CREATE TABLE IF NOT EXISTS gifts (" +
                "id             INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId         INTEGER NOT NULL, " +
                "name           TEXT NOT NULL, " +
                "description    TEXT, " +
                "price          TEXT, " +
                "whereToBuy     TEXT, " +
                "categoryId     INTEGER NOT NULL, " +
                "picture        TEXT, " +
                "secret         INTEGER NOT NULL, " +
                "FOREIGN KEY(userId) REFERENCES users(id), " +
                "FOREIGN KEY(categoryId) REFERENCES categories(id))")
        conn.execute("CREATE TABLE IF NOT EXISTS friendActionOnGift (" +
                "id             INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "giftId         INTEGER NOT NULL, " +
                "userId         INTEGER NOT NULL, " +
                "interested     INTEGER NOT NULL, " +
                "buy            TEXT NOT NULL, " +
                "FOREIGN KEY(userId) REFERENCES users(id), " +
                "FOREIGN KEY(giftId) REFERENCES gifts(id))")
        conn.execute("CREATE TABLE IF NOT EXISTS friendRequest (" +
                "id         INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userOne    INTEGER NOT NULL, " +
                "userTwo    INTEGER NOT NULL, " +
                "status     TEXT NOT NULL, " +
                "FOREIGN KEY(userOne) REFERENCES users(id), " +
                "FOREIGN KEY(userTwo) REFERENCES users(id))")

        conn.execute("CREATE TABLE IF NOT EXISTS events (" +
                "id             INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name           TEXT NOT NULL, " +
                "creatorId      INTEGER NOT NULL, " +
                "description    TEXT, " +
                "endDate        INTEGER NOT NULL, " +
                "target         INTEGER NOT NULL)")
        conn.execute("CREATE TABLE IF NOT EXISTS participants (" +
                "id         INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "eventId    INTEGER NOT NULL, " +
                "userId     INTEGER NOT NULL, " +
                "status     TEXT NOT NULL, " +
                "FOREIGN KEY(eventId) REFERENCES events(id), " +
                "FOREIGN KEY(userId) REFERENCES users(id))")
    }

    /**
     * Users
     */
    @Synchronized fun addUser(userName: String, password: String, picture: String?): DbUser {
        conn.execute("INSERT INTO users(name,password,picture) VALUES " +
                "('$userName', '$password', '${picture?: ""}')")
        val nextUserId = conn.executeQuery("SELECT last_insert_rowid()").getLong(1)
        addCategory(nextUserId, RestCategory(DEFAULT_CATEGORY_NAME))

        return DbUser(nextUserId, userName, password, picture)
    }

    @Synchronized fun getUser(userName: String): DbUser? {
        val res = conn.executeQuery("SELECT * FROM users WHERE users.name='$userName'")
        return if (res.next()) {
            val picture = res.getString("picture")
            DbUser(res.getLong("id"),
                res.getString("name"),
                res.getString("password"),
                if (picture.isEmpty()) null else picture)
        } else {
            null
        }
    }

    @Synchronized fun getUser(userId: Long): NakedUser? {
        val res = conn.executeQuery("SELECT * FROM users WHERE users.id='$userId'")
        return if (res.next()) {
            val picture = res.getString("picture")
            NakedUser(res.getString("name"), if (picture.isEmpty()) null else picture)
        } else {
            null
        }
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

        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryExists(gift.categoryId)) throw Exception("Unknown category " + gift.categoryId)
        if (!categoryBelongToUser(userId, gift.categoryId)) throw Exception("Category " + gift.categoryId + " does not belong to user $userId")

        conn.execute("INSERT INTO gifts(userId,name,description,price,whereToBuy,categoryId,picture,secret) VALUES " +
                "($userId, '${gift.name}', '${gift.description ?: ""}', '${gift.price ?: ""}', '${gift.whereToBuy ?: ""}', ${gift.categoryId}, '${gift.picture ?: ""}', $secret)")
        //API return 0 instead of null for price...
        //Maybe this query should be dynamic
    }

    @Synchronized fun getGift(giftId: Long) : DbGift? {
        if (!giftExists(giftId)) throw Exception("Unknown gift $giftId")

        val res = conn.executeQuery("SELECT * FROM gifts WHERE id=$giftId")
        while (res.next()) {
            val picture = res.getString("picture")
            return DbGift(
                res.getLong("id"),
                res.getLong("userId"),
                res.getString("name"),
                res.getString("description"),
                res.getString("price"),
                res.getString("whereToBuy"),
                res.getLong("categoryId"),
                if (picture.isEmpty()) null else picture,
                res.getBoolean("secret"))
        }

        return null
    }

    private fun getGifts(userId: Long, withSecret: Boolean) : List<DbGift> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val gifts = arrayListOf<DbGift>()
        val res = conn.executeQuery("SELECT * FROM gifts WHERE userId=$userId" + (if (withSecret) "" else " AND secret=0"))
        while (res.next()) {
            val picture = res.getString("picture")
            gifts.add(DbGift(
                res.getLong("id"),
                res.getLong("userId"),
                res.getString("name"),
                res.getString("description"),
                res.getString("price"),
                res.getString("whereToBuy"),
                res.getLong("categoryId"),
                if (picture.isEmpty()) null else picture,
                res.getBoolean("secret")))
        }

        return gifts
    }

    /** Return gift for a given user, secret gift will be filter out */
    @Synchronized fun getUserGifts(userId: Long) : List<DbGift> {
        return getGifts(userId, false)
    }

    /** Return gift for a given friend, secret gift will be returned */
    @Synchronized fun getFriendGifts(userId: Long, friendName: String): List<DbGift> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val friend = getUser(friendName) ?: throw Exception("Unknown user name $friendName")
        getFriendRequest(userId, friend.id) ?: getFriendRequest(friend.id, userId) ?: throw Exception("You are not friend with $friendName")

        return getGifts(friend.id, true)
    }

    @Synchronized fun modifyGift(userId: Long, giftId: Long, gift: RestGift) {
        if (gift.name == null) {
            throw Exception("Name could not be null")
        }
        if (gift.categoryId == null) {
            throw Exception("CategoryId could not be null, a default one exist")
        }

        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!giftBelongToUser(userId, giftId) && !giftIsSecret(giftId)) throw Exception("Gift $giftId does not belong to user $userId and is not secret") /* secret gift could be modified by anyone */

        conn.executeUpdate("UPDATE gifts SET name = '${gift.name}', description = '${gift.description}', price = '${gift.price}', whereToBuy = '${gift.whereToBuy}', categoryId = '${gift.categoryId}', picture = '${gift.picture}' WHERE id = $giftId")
    }

    @Synchronized fun removeGift(userId: Long, giftId: Long) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!giftBelongToUser(userId, giftId) && !giftIsSecret(giftId)) throw Exception("Gift $giftId does not belong to user $userId and is not secret") /* secret gift could be deleted by anyone */

        conn.executeUpdate("DELETE FROM gifts WHERE id = $giftId")
    }

    /**
     * Gift Actions
     */
    @Synchronized fun interested(giftId: Long, userId: Long, interested: Boolean) {
        if (!giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (giftBelongToUser(userId, giftId)) throw Exception("Gift $giftId belong to you. I hope you are interested in.")

        val res = conn.executeQuery("SELECT * FROM friendActionOnGift WHERE giftId = $giftId AND userId = $userId")
        if (res.next()) {
            val currentInterested = res.getBoolean("interested")
            val currentBuy = BuyAction.valueOf(res.getString("buy"))

            if (currentInterested == interested) return
            else if (interested || currentBuy != BuyAction.NONE) {
                conn.executeUpdate("UPDATE friendActionOnGift SET interested=$interested WHERE giftId = $giftId AND userId = $userId")
            } else {
                conn.executeUpdate("DELETE FROM friendActionOnGift WHERE giftId = $giftId AND userId = $userId")
            }
        } else if (interested) {
            conn.execute("INSERT INTO friendActionOnGift(giftId,userId,interested,buy) VALUES ($giftId, $userId, $interested, '${BuyAction.NONE}')")
        } else {
            //Nothing to do.
        }
    }

    @Synchronized fun buyAction(giftId: Long, userId: Long, buy: BuyAction) {
        if (!giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (giftBelongToUser(userId, giftId)) throw Exception("Gift $giftId belong to you. You cannot buy something at yourself.")

        val res = conn.executeQuery("SELECT * FROM friendActionOnGift WHERE giftId = $giftId AND userId = $userId")
        if (res.next()) {
            val currentInterested = res.getBoolean("interested")
            val currentBuy = BuyAction.valueOf(res.getString("buy"))

            if (currentBuy == buy) return
            else if (currentInterested || buy != BuyAction.NONE) {
                conn.executeUpdate("UPDATE friendActionOnGift SET buy='$buy' WHERE giftId = $giftId AND userId = $userId")
            } else {
                conn.executeUpdate("DELETE FROM friendActionOnGift WHERE giftId = $giftId AND userId = $userId")
            }
        } else if (buy != BuyAction.NONE) {
            conn.execute("INSERT INTO friendActionOnGift(giftId,userId,interested,buy) VALUES ($giftId, $userId, ${false}, '$buy')")
        } else {
            //Nothing to do.
        }
    }

    @Synchronized fun getFriendActionOnGift(giftId: Long) : List<DbFriendActionOnGift> {
        if (!giftExists(giftId)) throw Exception("Unknown gift $giftId")

        val res = conn.executeQuery("SELECT * FROM friendActionOnGift WHERE giftId = $giftId")
        return queryAnswerToDbFriendActionOnGift(res)
    }

    @Synchronized fun getFriendActionOnGiftsUserHasActionOn(userId: Long) : List<DbFriendActionOnGift> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val res = conn.executeQuery("SELECT * FROM friendActionOnGift WHERE userId = $userId")
        return queryAnswerToDbFriendActionOnGift(res)
    }

    private fun queryAnswerToDbFriendActionOnGift(res: ResultSet): ArrayList<DbFriendActionOnGift> {
        val out = arrayListOf<DbFriendActionOnGift>()
        while (res.next()) {
            out.add(
                DbFriendActionOnGift(
                    res.getLong("id"),
                    res.getLong("giftId"),
                    res.getLong("userId"),
                    res.getBoolean("interested"),
                    BuyAction.valueOf(res.getString("buy"))
                )
            )
        }

        return out
    }

    /**
     * Category
     */
    @Synchronized fun addCategory(userId: Long, category: RestCategory) {
        if (category.name == null) {
            throw Exception("Name could not be null")
        }

        if (!userExists(userId)) throw Exception("Unknown user $userId")

        conn.execute("INSERT INTO categories(userId,name) VALUES ($userId, '" + category.name + "')")
    }

    @Synchronized fun getUserCategories(userId: Long) : List<DbCategory> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val categories = arrayListOf<DbCategory>()
        val res = conn.executeQuery("SELECT * FROM categories WHERE categories.userId=$userId")
        while (res.next()) {
            categories.add(DbCategory(res.getLong("id"), userId, res.getString("name")))
        }

        return categories
    }

    @Synchronized fun getFriendCategories(userId: Long, friendName: String): List<DbCategory> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val friend = getUser(friendName) ?: throw Exception("Unknown user name $friendName")
        getFriendRequest(userId, friend.id) ?: getFriendRequest(friend.id, userId) ?: throw Exception("You are not friend with $friendName")

        return getUserCategories(friend.id)
    }

    @Synchronized fun modifyCategory(userId: Long, categoryId: Long, category: RestCategory) {
        if (category.name == null) {
            throw Exception("Name could not be null")
        }

        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryExists(categoryId)) throw Exception("Unknown category $categoryId")
        if (!categoryBelongToUser(userId, categoryId)) throw Exception("Category $categoryId does not belong to user $userId")

        conn.executeUpdate("UPDATE categories SET name = '" + category.name + "' WHERE id = $categoryId")
    }

    @Synchronized fun removeCategory(userId: Long, categoryId: Long) {
        //TODO: either remove gift or move it to another Category (client or side?)

        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryExists(categoryId)) throw Exception("Unknown category $categoryId")
        if (!categoryBelongToUser(userId, categoryId)) throw Exception("Category $categoryId does not belong to user $userId")

        conn.executeUpdate("DELETE FROM categories WHERE id = $categoryId")
    }

    /**
     * Friend request
     */
    @Synchronized fun createFriendRequest(userOne: Long, userTwo: Long) {
        if (userOne == userTwo) throw Exception("You cannot be friend with yourself")
        if (!userExists(userOne)) throw Exception("Unknown user $userOne")
        if (!userExists(userTwo)) throw Exception("Unknown user $userTwo")

        val friendRequest = getFriendRequest(userOne, userTwo)
        if (friendRequest != null) throw FriendRequestAlreadyExistException(friendRequest)

        val receivedRequest = getFriendRequest(userTwo, userOne)
        if (receivedRequest != null) {
            when {
                receivedRequest.status == RequestStatus.REJECTED -> deleteFriendRequest(userTwo, receivedRequest.id)
                else -> throw FriendRequestAlreadyExistException(receivedRequest)
            }
        }

        conn.execute("INSERT INTO friendRequest(userOne,userTwo,status) VALUES ($userOne, $userTwo, '${RequestStatus.PENDING}')")
    }

    @Synchronized fun getInitiatedFriendRequests(userId: Long) : List<DbFriendRequest> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val requests = arrayListOf<DbFriendRequest>()
        val res = conn.executeQuery("SELECT * FROM friendRequest WHERE userOne=$userId")
        while (res.next()) {
            requests.add(DbFriendRequest(
                res.getLong("id"),
                res.getLong("userOne"),
                res.getLong("userTwo"),
                RequestStatus.valueOf(res.getString("status"))))
        }

        return requests
    }

    @Synchronized fun getReceivedFriendRequests(userId: Long) : List<DbFriendRequest> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val requests = arrayListOf<DbFriendRequest>()
        val res = conn.executeQuery("SELECT * FROM friendRequest WHERE userTwo=$userId")
        while (res.next()) {
            requests.add(DbFriendRequest(
                res.getLong("id"),
                res.getLong("userOne"),
                res.getLong("userTwo"),
                RequestStatus.valueOf(res.getString("status"))))
        }

        return requests
    }

    @Synchronized fun deleteFriendRequest(userId: Long, friendRequestId: Long) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!friendRequestBelongToUser(userId, friendRequestId) && !friendRequestIsNotForUser(userId, friendRequestId)) throw Exception("Friend request $friendRequestId does not belong to user $userId")

        conn.executeUpdate("DELETE FROM friendRequest WHERE id = $friendRequestId")
    }

    @Synchronized fun acceptFriendRequest(userId: Long, friendRequestId: Long) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!friendRequestIsNotForUser(userId, friendRequestId)) throw Exception("Friend request $friendRequestId is not targeting user $userId")

        conn.executeUpdate("UPDATE friendRequest SET status = '${RequestStatus.ACCEPTED}' WHERE id = $friendRequestId")
    }

    @Synchronized fun declineFriendRequest(userId: Long, friendRequestId: Long, blockUser: Boolean) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!friendRequestIsNotForUser(userId, friendRequestId)) throw Exception("Friend request $friendRequestId is not targeting user $userId")

        if (blockUser) {
            conn.executeUpdate("UPDATE friendRequest SET status = '${RequestStatus.REJECTED}' WHERE id = $friendRequestId")
        } else {
            conn.executeUpdate("DELETE FROM friendRequest WHERE id = $friendRequestId")
        }
    }

    /**
     * Events
     */
    @Synchronized fun createEventAllForAll(name: String, creatorId: Long, description: String?, endDate: LocalDate, participantIds: Set<Long>) {
        createEventAllForOne(name, creatorId, description, endDate, -1, participantIds)
    }

    @Synchronized fun createEventAllForOne(name: String, creatorId: Long, description: String?, endDate: LocalDate, target: Long, participantIds: Set<Long>) {
        if (target != -1L && !userExists(target)) throw Exception("Unknown target user $target")

        conn.execute("INSERT INTO events(name,creatorId,description,endDate,target) VALUES ('$name', $creatorId, '${description ?: ""}', ${endDate.toEpochDay()}, $target)")
        val eventId = conn.executeQuery("SELECT last_insert_rowid()").getLong(1)

        addParticipants(eventId, participantIds)
        if (target != creatorId) acceptEventInvitation(creatorId, eventId)
    }

    fun deleteEvent(eventId: Long) {
        if (!eventExists(eventId)) throw Exception("Unknown event $eventId")

        //FOREIGN_KEY should delete participants?
        conn.executeUpdate("DELETE FROM events WHERE id = $eventId")
    }

    @Synchronized fun addParticipants(eventId: Long, participantIds: Set<Long>) {
        if (!eventExists(eventId)) throw Exception("Unknown event $eventId")

        val unknownParticipantIds = arrayListOf<Long>()
        for (participantId: Long in participantIds) {
            if (!userExists(participantId)) {
                unknownParticipantIds.add(participantId)
                continue
            }
            conn.execute("INSERT INTO participants(eventId,userId,status) VALUES($eventId,$participantId,'${RequestStatus.PENDING}')")
        }

        if (unknownParticipantIds.isNotEmpty()) throw Exception("Unknown participants $participantIds")
    }

    @Synchronized fun acceptEventInvitation(userId: Long, eventId: Long) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!eventExists(eventId)) throw Exception("Unknown event $eventId")
        if (!userInvitedToEvent(eventId, userId)) throw Exception("User $userId not invited to event $eventId")

        conn.executeUpdate("UPDATE participants SET status = '${RequestStatus.ACCEPTED}' WHERE eventId = $eventId AND userId = $userId")
    }

    @Synchronized fun declineEventInvitation(userId: Long, eventId: Long, blockInvites: Boolean) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!eventExists(eventId)) throw Exception("Unknown event $eventId")
        if (!userInvitedToEvent(eventId, userId)) throw Exception("User $userId not invited to event $eventId")

        if (blockInvites) {
            conn.executeUpdate("UPDATE participants SET status = '${RequestStatus.REJECTED}' WHERE eventId = $eventId AND userId = $userId")
        } else {
            conn.executeUpdate("DELETE FROM participants WHERE eventId = $eventId AND userId = $userId")
        }
    }

    @Synchronized fun getEventsById(eventId: Long) : DbEvent? {
        val res = conn.executeQuery("SELECT * FROM events WHERE id=$eventId")
        val resToDbEvents = resToDbEvents(res)
        return if (resToDbEvents.isEmpty()) null else resToDbEvents[0]
    }

    @Synchronized fun getEventsCreateBy(userId: Long) : List<DbEvent> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val res = conn.executeQuery("SELECT * FROM events WHERE creatorId=$userId")
        return resToDbEvents(res)
    }

    @Synchronized fun getEventsNamed(name: String) : List<DbEvent> {
        val res = conn.executeQuery("SELECT * FROM events WHERE name=$name")
        return resToDbEvents(res)
    }

    private fun resToDbEvents(res: ResultSet): ArrayList<DbEvent> {
        val events = arrayListOf<DbEvent>()
        while (res.next()) {
            events.add(DbEvent(
                    res.getLong("id"),
                    res.getString("name"),
                    res.getLong("creatorId"),
                    res.getString("description"),
                    LocalDate.ofEpochDay(res.getLong("endDate")),
                    if (res.getLong("target") == -1L) null else res.getLong("target")
                )
            )
        }

        return events
    }

    @Synchronized fun getEventsAsParticipants(userId: Long) : List<DbParticipant>{
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val participants = arrayListOf<DbParticipant>()
        val res = conn.executeQuery("SELECT * FROM participants WHERE userId=$userId")
        while (res.next()) {
            participants.add(DbParticipant(res.getLong("id"), res.getLong("eventId"), res.getLong("userId"), RequestStatus.valueOf(res.getString("status"))))
        }

        return participants
    }

    @Synchronized fun getParticipants(eventId: Long) : List<DbParticipant> {
        if (!eventExists(eventId)) throw Exception("Unknown event $eventId")

        val participants = arrayListOf<DbParticipant>()
        val res = conn.executeQuery("SELECT * FROM participants WHERE eventId=$eventId")
        while (res.next()) {
            participants.add(DbParticipant(res.getLong("id"), res.getLong("eventId"), res.getLong("userId"), RequestStatus.valueOf(res.getString("status"))))
        }

        return  participants
    }

    /**
     * Private
     */
    private fun userExists(userId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM users WHERE id=$userId")
        return res.next()
    }

    private fun giftExists(giftId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM gifts WHERE id=$giftId")
        return res.next()
    }

    private fun giftBelongToUser(userId: Long, giftId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM gifts WHERE id=$giftId AND userId=$userId")
        return res.next()
    }

    private fun giftIsSecret(giftId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM gifts WHERE id=$giftId AND secret=1")
        return res.next()
    }

    private fun categoryExists(categoryId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM categories WHERE id=$categoryId")
        return res.next()
    }

    private fun categoryBelongToUser(userId: Long, categoryId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM categories WHERE id=$categoryId AND userId=$userId")
        return res.next()
    }

    private fun getFriendRequest(userOne: Long, userTwo: Long) : DbFriendRequest? {
        val res = conn.executeQuery("SELECT * FROM friendRequest WHERE userOne = $userOne AND userTwo = $userTwo")
        if (!res.next()) return null

        return DbFriendRequest(
            res.getLong("id"),
            res.getLong("userOne"),
            res.getLong("userTwo"),
            RequestStatus.valueOf(res.getString("status")))
    }

    private fun friendRequestBelongToUser(userId: Long, friendRequestId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM friendRequest WHERE id = $friendRequestId AND userOne = $userId")
        return res.next()
    }

    private fun friendRequestIsNotForUser(userId: Long, friendRequestId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM friendRequest WHERE id = $friendRequestId AND userTwo = $userId")
        return res.next()
    }

    private fun eventExists(eventId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM events WHERE id=$eventId")
        return res.next()
    }

    private fun userInvitedToEvent(eventId: Long, userId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM participants WHERE eventId = $eventId AND userId = $userId")
        return res.next()
    }
}
