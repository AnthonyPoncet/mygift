package dao

import RestCategory

class CategoryAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO categories(userId,name) VALUES (?, ?)"
        const val SELECT_BY_ID = "SELECT * FROM categories WHERE id=?"
        const val SELECT_BY_USER = "SELECT * FROM categories WHERE userId=?"
        const val SELECT_BY_ID_AND_USER = "SELECT * FROM categories WHERE id=? AND userId=?"
        const val UPDATE = "UPDATE categories SET name = ? WHERE id = ?"
        const val DELETE = "DELETE FROM categories WHERE id = ?"
    }

    override fun getTableName(): String {
        return "categories"
    }

    override fun createIfNotExists() {
        conn.execute("CREATE TABLE IF NOT EXISTS categories (" +
            "id         INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "userId     INTEGER NOT NULL, " +
            "name       TEXT NOT NULL, " +
            "FOREIGN KEY(userId) REFERENCES users(id))")
    }

    fun addCategory(userId: Long, category: RestCategory) {
        conn.safeExecute(INSERT, {
            with(it) {
                setLong(1, userId)
                setString(2, category.name)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, userId.toString(), category.name ?: "no name"))
    }

    fun getUserCategories(userId: Long) : List<DbCategory> {
         return conn.safeExecute(SELECT_BY_USER, {
            with(it){
                setLong(1, userId)
                val res = executeQuery()
                val categories = arrayListOf<DbCategory>()
                while (res.next()) {
                    categories.add(DbCategory(res.getLong("id"), userId, res.getString("name")))
                }
                return@with categories
            }
        }, errorMessage(SELECT_BY_USER, userId.toString()))
    }

    fun getFriendCategories(friendId: Long): List<DbCategory> {
        return getUserCategories(friendId)
    }

    fun modifyCategory(categoryId: Long, category: RestCategory) {
        conn.safeExecute(UPDATE, {
            with(it) {
                setString(1, category.name)
                setLong(2, categoryId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE, category.name ?: "no name", categoryId.toString()))
    }

    fun removeCategory(categoryId: Long) {
        //TODO: either remove gift or move it to another Category (client or side?)
        //TODO: should be handle by foreign key...
        conn.safeExecute(DELETE, {
            with(it) {
                setLong(1, categoryId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(DELETE, categoryId.toString()))
    }

    fun categoryExists(categoryId: Long): Boolean {
        return conn.safeExecute(SELECT_BY_ID, {
            with(it) {
                setLong(1, categoryId)
                return@with executeQuery().next()
            }
        }, errorMessage(SELECT_BY_ID, categoryId.toString()))
    }

    fun categoryBelongToUser(userId: Long, categoryId: Long): Boolean {
        return conn.safeExecute(SELECT_BY_ID_AND_USER, {
            with(it) {
                setLong(1, categoryId)
                setLong(2, userId)
                return@with executeQuery().next()
            }
        }, errorMessage(SELECT_BY_ID_AND_USER, categoryId.toString(), userId.toString()))
    }
}