package org.aponcet.mygift.dbmanager

data class DbUser(val id: Long, val name: String, val password: ByteArray, val salt: ByteArray, val picture: String?, val dateOfBirth: Long?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DbUser) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (!password.contentEquals(other.password)) return false
        if (!salt.contentEquals(other.salt)) return false
        if (picture != other.picture) return false
        if (dateOfBirth != other.dateOfBirth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + (picture?.hashCode() ?: 0)
        result = 31 * result + (dateOfBirth?.hashCode() ?: 0)
        return result
    }
}

data class DbCategory(val id: Long, val name: String, val rank: Long, val share: Set<Long>)
data class DbGift(
    val id: Long,
    val name: String,
    val description: String?,
    val price: String?,
    val whereToBuy: String?,
    val categoryId: Long,
    val picture: String?,
    val secret: Boolean,
    val heart: Boolean,
    val rank: Long
)

data class DbFriendActionOnGift(
    val id: Long,
    val giftId: Long,
    val userId: Long
)

enum class RequestStatus { ACCEPTED, PENDING, REJECTED }
data class DbFriendRequest(val id: Long, val userOne: Long, val userTwo: Long, val status: RequestStatus)

data class DbToDeleteGifts(
    val giftId: Long,
    val giftUserId: Long,
    val name: String,
    val description: String?,
    val price: String?,
    val whereToBuy: String?,
    val picture: String?,
    val giftUserStatus: Status,
    val friendId: Long
)

data class NakedUser(val name: String, val picture: String?, val dateOfBirth: Long?)

class FriendRequestAlreadyExistException(val dbFriendRequest: DbFriendRequest) :
    Exception("Friend request already exists and is ${dbFriendRequest.status}.")


class DatabaseManager(dbPath: String) {
    companion object {
        private const val DEFAULT_CATEGORY_NAME = "Default"
    }

    private val conn = DbConnection("sqlite", dbPath)
    private val usersAccessor = UsersAccessor(conn)
    private val categoryAccessor = CategoryAccessor(conn)
    private val giftAccessor = GiftAccessor(conn)
    private val toDeleteGiftsAccessor = ToDeleteGiftsAccessor(conn)
    private val friendActionOnGiftAccessor = FriendActionOnGiftAccessor(conn)
    private val friendRequestAccessor = FriendRequestAccessor(conn)
    private val resetPasswordAccessor = ResetPasswordAccessor(conn)
    private val joinUserAndCategoryAccessor = JoinUserAndCategoryAccessor(conn)
    private val sessionAccessor = SessionAccessor(conn)

    init {
        createDataModelIfNeeded()
    }

    //Here only for test purpose
    fun cleanTables() {
        conn.execute("delete from ${usersAccessor.getTableName()}")
        conn.execute("delete from ${categoryAccessor.getTableName()}")
        conn.execute("delete from ${giftAccessor.getTableName()}")
        conn.execute("delete from ${toDeleteGiftsAccessor.getTableName()}")
        conn.execute("delete from ${friendActionOnGiftAccessor.getTableName()}")
        conn.execute("delete from ${friendRequestAccessor.getTableName()}")
        conn.execute("delete from ${resetPasswordAccessor.getTableName()}")
        conn.execute("delete from ${joinUserAndCategoryAccessor.getTableName()}")
    }

    private fun createDataModelIfNeeded() {
        usersAccessor.createIfNotExists()
        categoryAccessor.createIfNotExists()
        giftAccessor.createIfNotExists()
        toDeleteGiftsAccessor.createIfNotExists()
        friendActionOnGiftAccessor.createIfNotExists()
        friendRequestAccessor.createIfNotExists()
        resetPasswordAccessor.createIfNotExists()
        joinUserAndCategoryAccessor.createIfNotExists()
    }

    /**
     * Users
     */
    @Synchronized
    fun addUser(userName: String, password: ByteArray, salt: ByteArray, picture: String?, dateOfBirth: Long?): DbUser {
        val newUser = usersAccessor.addUser(userName, password, salt, picture ?: "", dateOfBirth)
        addCategory(DEFAULT_CATEGORY_NAME, listOf(newUser.id))
        return newUser
    }

    @Synchronized
    fun getUser(userName: String): DbUser? {
        return usersAccessor.getUser(userName)
    }

    @Synchronized
    fun getUser(userId: Long): NakedUser? {
        return usersAccessor.getUser(userId)
    }

    @Synchronized
    fun modifyUser(userId: Long, name: String, picture: String?, dateOfBirth: Long?) {
        return usersAccessor.modifyUser(userId, name, picture ?: "", dateOfBirth)
    }

    /**
     * Gift
     */
    @Synchronized
    fun addGift(userId: Long, gift: NewGift, secret: Boolean) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryAccessor.categoryExists(gift.categoryId)) throw Exception("Unknown category " + gift.categoryId)
        if (!categoryAccessor.categoryBelongToUser(
                userId,
                gift.categoryId
            )
        ) throw Exception("Category " + gift.categoryId + " does not belong to user $userId")

        giftAccessor.addGift(gift, secret)
    }

    @Synchronized
    fun getGift(giftId: Long): DbGift? {
        if (!giftAccessor.giftExists(giftId)) throw Exception("Unknown gift $giftId")
        return giftAccessor.getGift(giftId)
    }

    /** Return gift for a given user, secret gift will be filter out */
    @Synchronized
    fun getUserGifts(userId: Long): List<DbGift> {
        return giftAccessor.getUserGifts(userId)
    }

    /** Return gift for a given friend, secret gift will be returned */
    @Synchronized
    fun getFriendGifts(userId: Long, friendName: String): List<DbGift> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        val friend = getUser(friendName) ?: throw Exception("Unknown user name $friendName")
        friendRequestAccessor.getFriendRequest(userId, friend.id) ?: friendRequestAccessor.getFriendRequest(
            friend.id,
            userId
        ) ?: throw Exception("You are not friend with $friendName")

        return giftAccessor.getFriendGifts(friend.id)
    }

    @Synchronized
    fun modifyGift(userId: Long, giftId: Long, gift: NewGift) {
        checkUpdateGiftsInputs(userId, giftId)
        giftAccessor.modifyGift(giftId, gift)
    }

    @Synchronized
    fun removeGift(userId: Long, giftId: Long, status: Status) {
        checkUpdateGiftsInputs(userId, giftId)
        giftAccessor.removeGift(giftId, status)
    }

    @Synchronized
    fun rankDownGift(userId: Long, giftId: Long) {
        checkUpdateGiftsInputs(userId, giftId)
        giftAccessor.rankDownGift(userId, giftId)
    }

    @Synchronized
    fun rankUpGift(userId: Long, giftId: Long) {
        checkUpdateGiftsInputs(userId, giftId)
        giftAccessor.rankUpGift(userId, giftId)
    }

    @Synchronized
    fun updateHeart(userId: Long, giftId: Long, heart: Boolean) {
        checkUpdateGiftsInputs(userId, giftId)
        giftAccessor.updateHeart(giftId, heart)
    }

    private fun checkUpdateGiftsInputs(userId: Long, giftId: Long) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!giftAccessor.giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!giftAccessor.giftBelongToUser(
                userId,
                giftId
            ) && !giftAccessor.giftIsSecret(giftId)
        ) throw Exception("Gift $giftId does not belong to user $userId and is not secret") /* secret gift could be modified by anyone */
    }

    /**
     * Gift Actions
     */
    @Synchronized
    fun changeReserve(giftId: Long, userId: Long, reserve: Boolean) {
        if (!giftAccessor.giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (giftAccessor.giftBelongToUser(
                userId,
                giftId
            )
        ) throw Exception("Gift $giftId belong to you. You cannot buy something at yourself.")

        if (reserve) {
            friendActionOnGiftAccessor.insert(giftId, userId)
        } else {
            friendActionOnGiftAccessor.delete(giftId, userId)
        }
    }

    @Synchronized
    fun getFriendActionOnGift(giftId: Long): List<DbFriendActionOnGift> {
        if (!giftAccessor.giftExists(giftId)) throw Exception("Unknown gift $giftId")

        return friendActionOnGiftAccessor.getFriendActionOnGift(giftId)
    }

    @Synchronized
    fun getFriendActionOnGiftsUserHasActionOn(userId: Long): List<DbFriendActionOnGift> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return friendActionOnGiftAccessor.getFriendActionOnGiftsUserHasActionOn(userId)
    }

    @Synchronized
    fun getDeletedGiftsUserHasActionOn(userId: Long): List<DbToDeleteGifts> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return toDeleteGiftsAccessor.getDeletedGiftsWhereUserHasActionOn(userId)
    }

    @Synchronized
    fun deleteDeletedGift(giftId: Long, friendId: Long) {
        if (!usersAccessor.userExists(friendId)) throw Exception("Unknown user $friendId")

        return toDeleteGiftsAccessor.deleteDeletedGift(giftId, friendId)
    }


    /**
     * Category
     */
    @Synchronized
    fun addCategory(name: String, userIds: List<Long>) {
        userIds.forEach { if (!usersAccessor.userExists(it)) throw Exception("Unknown user $it") }

        categoryAccessor.addCategory(name, userIds)
    }

    @Synchronized
    fun getUserCategories(userId: Long): List<DbCategory> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return categoryAccessor.getUserCategories(userId)
    }

    @Synchronized
    fun getFriendCategories(userId: Long, friendName: String): List<DbCategory> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        val friend = getUser(friendName) ?: throw Exception("Unknown user name $friendName")
        friendRequestAccessor.getFriendRequest(userId, friend.id) ?: friendRequestAccessor.getFriendRequest(
            friend.id,
            userId
        ) ?: throw Exception("You are not friend with $friendName")

        return categoryAccessor.getFriendCategories(userId, friend.id)
    }

    @Synchronized
    fun modifyCategory(userId: Long, categoryId: Long, name: String) {
        checkCategoryInputs(userId, categoryId)
        categoryAccessor.modifyCategory(categoryId, name)
    }

    @Synchronized
    fun removeCategory(userId: Long, categoryId: Long) {
        checkCategoryInputs(userId, categoryId)
        categoryAccessor.removeCategory(categoryId)
    }

    @Synchronized
    fun rankDownCategory(userId: Long, categoryId: Long) {
        checkCategoryInputs(userId, categoryId)
        categoryAccessor.rankDownCategory(userId, categoryId)
    }

    @Synchronized
    fun rankUpCategory(userId: Long, categoryId: Long) {
        checkCategoryInputs(userId, categoryId)
        categoryAccessor.rankUpCategory(userId, categoryId)
    }

    @Synchronized
    fun getUsersFromCategory(categoryId: Long): Set<Long> {
        if (!categoryAccessor.categoryExists(categoryId)) throw Exception("Unknown category $categoryId")
        return joinUserAndCategoryAccessor.getUsers(categoryId)
    }

    private fun checkCategoryInputs(userId: Long, categoryId: Long) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryAccessor.categoryExists(categoryId)) throw Exception("Unknown category $categoryId")
        if (!categoryAccessor.categoryBelongToUser(
                userId,
                categoryId
            )
        ) throw Exception("Category $categoryId does not belong to user $userId")
    }

    /**
     * Friend request
     */
    @Synchronized
    fun createFriendRequest(userOne: Long, userTwo: Long) {
        if (userOne == userTwo) throw Exception("You cannot be friend with yourself")
        if (!usersAccessor.userExists(userOne)) throw Exception("Unknown user $userOne")
        if (!usersAccessor.userExists(userTwo)) throw Exception("Unknown user $userTwo")

        val friendRequest = friendRequestAccessor.getFriendRequest(userOne, userTwo)
        if (friendRequest != null) throw FriendRequestAlreadyExistException(
            friendRequest
        )

        val receivedRequest = friendRequestAccessor.getFriendRequest(userTwo, userOne)
        if (receivedRequest != null) {
            when (receivedRequest.status) {
                RequestStatus.REJECTED -> deleteFriendRequest(userTwo, receivedRequest.id)
                else -> throw FriendRequestAlreadyExistException(
                    receivedRequest
                )
            }
        }

        friendRequestAccessor.createFriendRequest(userOne, userTwo)
    }

    @Synchronized
    fun getInitiatedFriendRequests(userId: Long): List<DbFriendRequest> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return friendRequestAccessor.getInitiatedFriendRequests(userId)
    }

    @Synchronized
    fun getReceivedFriendRequests(userId: Long): List<DbFriendRequest> {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")

        return friendRequestAccessor.getReceivedFriendRequests(userId)
    }

    @Synchronized
    fun deleteFriendRequest(userId: Long, friendRequestId: Long) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!friendRequestAccessor.friendRequestBelongToUser(
                userId,
                friendRequestId
            ) && !friendRequestAccessor.friendRequestIsNotForUser(userId, friendRequestId)
        ) throw Exception("Friend request $friendRequestId does not belong to user $userId")

        friendRequestAccessor.deleteFriendRequest(friendRequestId)
    }

    @Synchronized
    fun acceptFriendRequest(userId: Long, friendRequestId: Long) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!friendRequestAccessor.friendRequestIsNotForUser(
                userId,
                friendRequestId
            )
        ) throw Exception("Friend request $friendRequestId is not targeting user $userId")

        friendRequestAccessor.acceptFriendRequest(friendRequestId)
    }

    @Synchronized
    fun declineFriendRequest(userId: Long, friendRequestId: Long, blockUser: Boolean) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        if (!friendRequestAccessor.friendRequestIsNotForUser(
                userId,
                friendRequestId
            )
        ) throw Exception("Friend request $friendRequestId is not targeting user $userId")

        friendRequestAccessor.declineFriendRequest(friendRequestId, blockUser)
    }

    /**
     * Reset password
     */
    @Synchronized
    fun getEntry(uuid: String): DbResetPassword {
        return resetPasswordAccessor.getEntry(uuid) ?: throw Exception("Unknown uuid $uuid")
    }

    @Synchronized
    fun deleteEntry(userId: Long, uuid: String) {
        resetPasswordAccessor.delete(userId, uuid)
    }

    /**
     * Sessions
     */
    @Synchronized
    fun deleteSession(session: String, userId: Long) {
        if (!usersAccessor.userExists(userId)) throw Exception("Unknown user $userId")
        return sessionAccessor.deleteSession(session, userId)
    }

    @Synchronized
    fun getUsersOfSession(currentUserId: Long, session: String): List<Long> {
        val users = sessionAccessor.getUsersOfSession(session)
        if (!users.contains(currentUserId)) throw Exception("Session does not belongs to you")

        return users.filter { it != currentUserId }
    }
}
