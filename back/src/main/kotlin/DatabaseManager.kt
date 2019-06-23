
data class DbUser(val id: Long, val name: String, val password: String)
data class DbGift(val id: Long, val userId: Long, val name: String, val categoryId: Long)
data class DbCategory(val id: Long, val userId: Long, val name: String)

/**
 * In memory DB for now on.
 */
class DatabaseManager {
    companion object {
        private const val DEFAULT_CATEGORY_NAME = "Default"
    }

    private var nextUserId = 0L
    private val users = arrayListOf<DbUser>()
    private var nextGiftId = 0L
    private val gifts = arrayListOf<DbGift>()
    private var nextCategory = 0L
    private val categories = arrayListOf<DbCategory>()

    @Synchronized fun addUser(userInformation: UserInformation): DbUser {
        if (getUser(ConnectionInformation(userInformation.name, userInformation.password)) != null) {
            throw Exception("User already exists")
        }

        val newUser = DbUser(nextUserId, userInformation.name!!, userInformation.password!!)
        users.add(newUser)
        addCategory(nextUserId, RestCategory(DEFAULT_CATEGORY_NAME))
        nextUserId++

        return newUser
    }

    @Synchronized fun getUser(connectionInformation: ConnectionInformation): DbUser? {
        return users.firstOrNull { u -> u.name == connectionInformation.name && u.password == connectionInformation.password }
    }

    @Synchronized fun addGift(userId: Long, gift: RestGift) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (gift.categoryId != null) {
            if (!categoryExists(gift.categoryId)) throw Exception("Unknown category " + gift.categoryId)
            if (!categoryBelongToUser(userId, gift.categoryId)) throw Exception("Category " + gift.categoryId + " does not belong to user $userId")
        }

        gifts.add(DbGift(nextGiftId++, userId, gift.name!!, gift.categoryId ?: getDefaultCategory(userId)))
    }

    @Synchronized fun getUserGifts(userId: Long) : List<DbGift> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        return gifts.filter { g -> g.userId == userId }
    }

    @Synchronized fun modifyGift(userId: Long, giftId: Long, gift: RestGift) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!giftBelongToUser(userId, giftId)) throw Exception("Gift $giftId does not belong to user $userId")

        gifts.removeIf { g -> g.id == giftId }
        gifts.add(DbGift(giftId, userId, gift.name!!, gift.categoryId ?: getDefaultCategory(userId)))
    }

    @Synchronized fun removeGift(userId: Long, giftId: Long) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!giftBelongToUser(userId, giftId)) throw Exception("Gift $giftId does not belong to user $userId")

        gifts.removeIf { g -> g.id == giftId }
    }

    @Synchronized fun addCategory(userId: Long, category: RestCategory) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        categories.add(DbCategory(nextCategory++, userId, category.name!!))
    }

    @Synchronized fun getUserCategories(userId: Long) : List<DbCategory> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        return categories.filter { c -> c.userId == userId }
    }

    @Synchronized fun modifyCategory(userId: Long, categoryId: Long, category: RestCategory) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryExists(categoryId)) throw Exception("Unknown category $categoryId")
        if (!categoryBelongToUser(userId, categoryId)) throw Exception("Category $categoryId does not belong to user $userId")

        categories.removeIf { c -> c.id == categoryId }
        categories.add(DbCategory(categoryId, userId, category.name!!))
    }

    @Synchronized fun removeCategory(userId: Long, categoryId: Long) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!categoryExists(categoryId)) throw Exception("Unknown category $categoryId")
        if (!categoryBelongToUser(userId, categoryId)) throw Exception("Category $categoryId does not belong to user $userId")

        categories.removeIf { c -> c.id == categoryId }
    }

    private fun userExists(userId: Long): Boolean {
        return users.any { u -> u.id == userId }
    }

    private fun giftExists(giftId: Long): Boolean {
        return gifts.any { g -> g.id == giftId }
    }

    private fun giftBelongToUser(userId: Long, giftId: Long): Boolean {
        return gifts.any { g -> g.id == giftId && g.userId == userId }
    }

    private fun categoryExists(categoryId: Long): Boolean {
        return categories.any { c -> c.id == categoryId }
    }

    private fun categoryBelongToUser(userId: Long, categoryId: Long): Boolean {
        return categories.any { c -> c.id == categoryId && c.userId == userId }
    }

    private fun getDefaultCategory(userId: Long): Long {
        return categories.first { c -> c.userId == userId && c.name == DEFAULT_CATEGORY_NAME }.id
    }
}
