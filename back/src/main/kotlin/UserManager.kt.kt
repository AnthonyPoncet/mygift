import java.lang.Exception

/** RETURN CLASSES **/
data class User(val id: Long, val name: String)
data class Gift(val id: Long, val name: String, val categoryId: Long)
data class Gifts(val gifts: List<Gift>)
data class Category(val id: Long, val name: String)
data class Categories(val categories: List<Category>)

/** INPUT CLASSES **/
data class ConnectionInformation(val name: String?, val password: String?)
data class UserInformation(val name: String?, val password: String?)
data class RestGift(val name: String?, val categoryId: Long?)
data class RestCategory(val name: String?)

class UserManager(private var databaseManager: DatabaseManager) {

    fun connect(connectionInformation: ConnectionInformation): User {
        val user = databaseManager.getUser(connectionInformation) ?: throw Exception("Password or user may be wrong")
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

}
