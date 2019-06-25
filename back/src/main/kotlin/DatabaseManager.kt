import java.lang.IllegalStateException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

data class DbUser(val id: Long, val name: String, val password: String)
data class DbGift(val id: Long, val userId: Long, val name: String, val categoryId: Long)
data class DbCategory(val id: Long, val userId: Long, val name: String)

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
        conn.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, password TEXT NOT NULL)")
        conn.execute("CREATE TABLE IF NOT EXISTS gifts (id INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER NOT NULL, name TEXT NOT NULL, categoryId INTEGER NOT NULL)")
        conn.execute("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER NOT NULL, name TEXT NOT NULL)")
    }

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

        conn.execute("INSERT INTO gifts(userId,name,categoryId) VALUES ($userId, '" + gift.name + "', " + gift.categoryId + ")")
    }

    @Synchronized fun getUserGifts(userId: Long) : List<DbGift> {
        if (!userExists(userId)) throw Exception("Unknown user $userId")

        val gifts = arrayListOf<DbGift>()
        val res = conn.executeQuery("SELECT * FROM gifts WHERE gifts.userId=$userId")
        while (res.next()) {
            gifts.add(DbGift(res.getLong("id"), userId, res.getString("name"), res.getLong("categoryId")))
        }

        return gifts
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

        conn.executeUpdate("UPDATE gifts SET name = '" + gift.name + "', " + gift.categoryId + " WHERE id = $giftId")
    }

    @Synchronized fun removeGift(userId: Long, giftId: Long) {
        if (!userExists(userId)) throw Exception("Unknown user $userId")
        if (!giftExists(giftId)) throw Exception("Unknown gift $giftId")
        if (!giftBelongToUser(userId, giftId)) throw Exception("Gift $giftId does not belong to user $userId")

        conn.executeUpdate("DELETE FROM gifts WHERE id = $giftId")
    }

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
}
