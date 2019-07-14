import kotlin.Exception

/** RETURN CLASSES **/
data class User(val id: Long, val name: String)
data class Gift(val id: Long, val name: String, val categoryId: Long)
data class Gifts(val gifts: List<Gift>)
data class Category(val id: Long, val name: String)
data class Categories(val categories: List<Category>)
data class FriendRequest(val id: Long, val from: User, val to: User, val status: RequestStatus)

/** INPUT CLASSES **/
data class ConnectionInformation(val name: String?, val password: String?)
data class UserInformation(val name: String?, val password: String?)
data class RestGift(val name: String?, val categoryId: Long?)
data class RestCategory(val name: String?)
data class RestCreateFriendRequest(val name: String?)

class UserManager(private var databaseManager: DatabaseManager) {

    fun connect(connectionInformation: ConnectionInformation): User {
        if (connectionInformation.name == null) throw Exception("Username could not be null")
        if (connectionInformation.password == null) throw Exception("Password could not be null")

        val user = databaseManager.getUser(connectionInformation.name) ?: throw Exception("Unknown user")
        if (user.password != connectionInformation.password) throw Exception("Wrong password")

        return User(user.id, user.name)
    }

    fun addUser(userInformation: UserInformation): User {
        val dbUser = databaseManager.addUser(userInformation)
        return User(dbUser.id, dbUser.name)
    }

    fun getUserGifts(userId: Long): Gifts {
        return Gifts(databaseManager.getUserGifts(userId).map { g -> Gift(g.id, g.name, g.categoryId) })
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

    fun getUserCategories(userId: Long): Categories {
        return Categories(databaseManager.getUserCategories(userId).map { c -> Category(c.id, c.name) })
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
        if (otherUser.name == null) throw Exception("Username to send request to could not be null")

        val user = databaseManager.getUser(otherUser.name) ?: throw Exception("User ${otherUser.name} does not exist.")

        databaseManager.createFriendRequest(userId, user.id)
    }

    fun getInitiatedFriendRequest(userId: Long) : List<FriendRequest> {
        val initiatedFriendRequests = databaseManager.getInitiatedFriendRequests(userId)

        return initiatedFriendRequests.map { i -> toFriendRequest(i) }
    }

    fun getReceivedFriendRequest(userId: Long) : List<FriendRequest> {
        val receivedFriendRequests = databaseManager.getReceivedFriendRequests(userId)

        return receivedFriendRequests.map { r -> toFriendRequest(r) }
    }

    fun deleteFriendRequest(userId: Long, friendRequestId: Long) {
        databaseManager.deleteFriendRequest(userId, friendRequestId)
    }

    fun acceptFriendRequest(userId: Long, friendRequestId: Long) {
        databaseManager.acceptFriendRequest(userId, friendRequestId)
    }

    fun declineFriendRequest(userId: Long, friendRequestId: Long) {
        databaseManager.declineFriendRequest(userId, friendRequestId)
    }

    private fun toFriendRequest(dbRequest: DbFriendRequest) : FriendRequest {
        val one = databaseManager.getUser(dbRequest.userOne)!!
        val two = databaseManager.getUser(dbRequest.userTwo)!!
        return FriendRequest(dbRequest.id, User(one.id, one.name), User(two.id, two.name), dbRequest.status)
    }

}
