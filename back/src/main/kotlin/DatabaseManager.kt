import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

data class DbUser(val id: Long, val name: String, val password: String)
data class DbCategory(val id: Long, val userId: Long, val name: String)
data class DbGift(val id: Long, val userId: Long, val name: String, val description: String?, val price: String?, val whereToBuy: String?, val categoryId: Long)
enum class BuyAction { NONE, WANT_TO_BUY, BOUGHT }
data class DbFriendActionOnGift(val id: Long, val giftId: Long, val userId: Long, val interested: Boolean, val buy: BuyAction)
enum class RequestStatus { ACCEPTED, PENDING, REJECTED }
data class DbFriendRequest(val id: Long, val userOne: Long, val userTwo: Long, val status: RequestStatus)

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

    private fun createDataModelIfNeeded() {
        conn.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id         INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name       TEXT NOT NULL, " +
                "password   TEXT NOT NULL)")
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
    }

    /**
     * Users
     */
    @Synchronized fun addUser(userInformation: UserInformation): DbUser {
        if (userInformation.name == null) {
            throw Exception("Name could not be null")
        }
        if (userInformation.password == null) {
            throw Exception("Password could not be null")
        }

        if (getUser(userInformation.name) != null) {
            throw Exception("User already exists")
        }

        conn.execute("INSERT INTO users(name,password) VALUES ('" + userInformation.name + "', '" + userInformation.password + "')")
        val nextUserId = conn.executeQuery("SELECT last_insert_rowid()").getLong(1)
        addCategory(nextUserId, RestCategory(DEFAULT_CATEGORY_NAME))

        return DbUser(nextUserId, userInformation.name, userInformation.password)
    }

    @Synchronized fun getUser(userName: String): DbUser? {
        val res = conn.executeQuery("SELECT * FROM users WHERE users.name='$userName'")
        return if (res.next()) {
            DbUser(res.getLong("id"), res.getString("name"), res.getString("password"))
        } else {
            null
        }
    }

    @Synchronized fun getUser(userId: Long): String? {
        val res = conn.executeQuery("SELECT * FROM users WHERE users.id='$userId'")
        return if (res.next()) {
            res.getString("name")
        } else {
            null
        }
    }

    /**
     * Gift
     */
    @Synchronized fun addGift(userId: Long, gift: RestGift) {
        if (gift.name == null) {
            throw Exception("Name could not be null")
        }
        if (gift.categoryId == null) {
            throw Exception("CategoryId could not be null, a default one exist")
        }

        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryExists(gift.categoryId)) throw Exception("Unknown category " + gift.categoryId)
        if (!categoryBelongToUser(userId, gift.categoryId)) throw Exception("Category " + gift.categoryId + " does not belong to user $userId")

        conn.execute("INSERT INTO gifts(userId,name,description,price,whereToBuy,categoryId) VALUES " +
                "($userId, '${gift.name}', '${gift.description ?: ""}', '${gift.price ?: ""}', '${gift.whereToBuy ?: ""}', ${gift.categoryId})")
        //API return 0 instead of null for price...
        //Maybe this query should be dynamic
    }

    @Synchronized fun getUserGifts(userId: Long) : List<DbGift> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val gifts = arrayListOf<DbGift>()
        val res = conn.executeQuery("SELECT * FROM gifts WHERE gifts.userId=$userId")
        while (res.next()) {
            gifts.add(DbGift(
                res.getLong("id"),
                res.getLong("userId"),
                res.getString("name"),
                res.getString("description"),
                res.getString("price"),
                res.getString("whereToBuy"),
                res.getLong("categoryId")))
        }

        return gifts
    }

    @Synchronized fun getFriendGifts(userId: Long, friendName: String): List<DbGift> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val friend = getUser(friendName) ?: throw Exception("Unknown user name $friendName")
        getFriendRequest(userId, friend.id) ?: getFriendRequest(friend.id, userId) ?: throw Exception("You are not friend with $friendName")

        return getUserGifts(friend.id)
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
        if (!giftBelongToUser(userId, giftId)) throw Exception("Gift $giftId does not belong to user $userId")

        conn.executeUpdate("UPDATE gifts SET name = '${gift.name}', description = '${gift.description}', price = '${gift.price}', whereToBuy = '${gift.whereToBuy}', categoryId = '${gift.categoryId}' WHERE id = $giftId")
    }

    @Synchronized fun removeGift(userId: Long, giftId: Long) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!giftBelongToUser(userId, giftId)) throw Exception("Gift $giftId does not belong to user $userId")

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
        val res = conn.executeQuery("SELECT * FROM friendRequest WHERE friendRequest.userOne=$userId")
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
        val res = conn.executeQuery("SELECT * FROM friendRequest WHERE friendRequest.userTwo=$userId")
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
        if (!friendRequestBelongToUser(userId, friendRequestId)) throw Exception("Friend request $friendRequestId does not belong to user $userId")

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
     * Private
     */

    private fun userExists(userId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM users WHERE users.id=$userId")
        return res.next()
    }

    private fun giftExists(giftId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM gifts WHERE gifts.id=$giftId")
        return res.next()
    }

    private fun giftBelongToUser(userId: Long, giftId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM gifts WHERE gifts.id=$giftId AND gifts.userId=$userId")
        return res.next()
    }

    private fun categoryExists(categoryId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM categories WHERE categories.id=$categoryId")
        return res.next()
    }

    private fun categoryBelongToUser(userId: Long, categoryId: Long): Boolean {
        val res = conn.executeQuery("SELECT * FROM categories WHERE categories.id=$categoryId AND categories.userId=$userId")
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

}
