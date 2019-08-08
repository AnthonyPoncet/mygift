import kotlin.Exception

/** RETURN CLASSES **/
data class User(val id: Long, val name: String)
data class Gift(val id: Long, val name: String, val description: String?, val price: String?, val whereToBuy: String?, val categoryId: Long)
data class Gifts(val gifts: List<Gift>)
data class FriendGift(val gift: Gift, val interestedUser: List<String>, val buyActionUser: Map<String, BuyAction>)
data class FriendGifts(val friendGifts: List<FriendGift>)
data class Category(val id: Long, val name: String)
data class Categories(val categories: List<Category>)
data class FriendRequest(val id: Long, val from: User, val to: User, val status: RequestStatus)

/** INPUT CLASSES **/
data class ConnectionInformation(val name: String?, val password: String?)
data class UserInformation(val name: String?, val password: String?)
data class RestGift(val name: String?, val description: String?, val price: String?, val whereToBuy: String?, val categoryId: Long?)
data class RestCategory(val name: String?)
data class RestCreateFriendRequest(val name: String?)

/**
 * No cleaning notion, take care of its validity
 */
class DummyUserCache(private val databaseManager: DatabaseManager) {
    private val cache = HashMap<Long, String>()

    fun query(userId: Long) : String {
        val name = cache[userId]
        return if (name != null) name
        else {
            val dbName = databaseManager.getUser(userId)
            cache[userId] = dbName ?: "Unknown"
            cache[userId]!!
        }
    }
}

class UserManager(private val databaseManager: DatabaseManager) {

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
        return Gifts(databaseManager.getUserGifts(userId).map { g -> Gift(g.id, g.name, g.description, g.price, g.whereToBuy, g.categoryId) })
    }

    //Not optimal at all!
    fun getFriendGifts(userId: Long, friendName: String): FriendGifts {
        val gifts = databaseManager.getFriendGifts(userId, friendName).map { g -> Gift(g.id, g.name, g.description, g.price, g.whereToBuy, g.categoryId) }

        val friendGifts = arrayListOf<FriendGift>()
        val dummyUserCache = DummyUserCache(databaseManager) //Cache only by call
        for (gift: Gift in gifts) {
            val actions = databaseManager.getFriendActionOnGift(gift.id)
            friendGifts.add(FriendGift(gift,
                actions.filter { it.interested || dummyUserCache.query(it.userId) == "Unknown" }.map { dummyUserCache.query(it.userId) },
                actions.filter { it.buy != BuyAction.NONE  || dummyUserCache.query(it.userId) == "Unknown" }.map { dummyUserCache.query(it.userId) to it.buy }.toMap() ))
        }

        return FriendGifts(friendGifts)
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

    fun interested(giftId: Long, userId: Long, interested: Boolean) {
        databaseManager.interested(giftId, userId, interested)
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

    fun wantToBuy(giftId: Long, userId: Long) {
        databaseManager.buyAction(giftId, userId, BuyAction.WANT_TO_BUY)
    }

    fun hasBought(giftId: Long, userId: Long) {
        databaseManager.buyAction(giftId, userId, BuyAction.BOUGHT)
    }

    fun getUserCategories(userId: Long): Categories {
        return Categories(databaseManager.getUserCategories(userId).map { c -> Category(c.id, c.name) })
    }

    fun getFriendCategories(userId: Long, friendName: String): Categories {
        return Categories(databaseManager.getFriendCategories(userId, friendName).map { c -> Category(c.id, c.name) })
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

    fun declineFriendRequest(userId: Long, friendRequestId: Long, blockUser: Boolean) {
        databaseManager.declineFriendRequest(userId, friendRequestId, blockUser)
    }

    private fun toFriendRequest(dbRequest: DbFriendRequest) : FriendRequest {
        val one = databaseManager.getUser(dbRequest.userOne)!!
        val two = databaseManager.getUser(dbRequest.userTwo)!!
        return FriendRequest(dbRequest.id, User(dbRequest.userOne, one), User(dbRequest.userTwo, two), dbRequest.status)
    }

}
