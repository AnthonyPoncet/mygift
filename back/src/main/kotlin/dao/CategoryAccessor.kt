package dao

import RestCategory

class CategoryAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO categories(userId,name,rank) VALUES (?,?,?)"
        const val SELECT_BY_ID = "SELECT * FROM categories WHERE id=?"
        const val SELECT_BY_USER = "SELECT * FROM categories WHERE userId=? ORDER BY rank ASC"
        const val SELECT_BY_ID_AND_USER = "SELECT * FROM categories WHERE id=? AND userId=? ORDER BY rank ASC"
        const val UPDATE = "UPDATE categories SET name=?, rank=? WHERE id=?"
        const val DELETE = "DELETE FROM categories WHERE id=?"
        const val SELECT_MAX_RANK = "SELECT MAX(rank) FROM categories WHERE userId=?"
        const val SELECT_CAT_WITH_SMALLER_RANK = "SELECT * FROM categories WHERE userId=? AND rank=(SELECT MAX(rank) FROM categories WHERE userId=? AND rank<?)"
        const val SELECT_CAT_WITH_HIGHER_RANK = "SELECT * FROM categories WHERE userId=? AND rank=(SELECT MIN(rank) FROM categories WHERE userId=? AND rank>?)"
    }

    override fun getTableName(): String {
        return "categories"
    }

    override fun createIfNotExists() {
        conn.execute("CREATE TABLE IF NOT EXISTS categories (" +
            "id     INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "userId INTEGER NOT NULL, " +
            "name   TEXT NOT NULL, " +
            "rank   INTEGER NOT NULL," +
            "FOREIGN KEY(userId) REFERENCES users(id))")
    }

    fun addCategory(userId: Long, category: RestCategory) {
        val maxId = getCurrentMaxRank(userId)
        conn.safeExecute(INSERT, {
            with(it) {
                setLong(1, userId)
                setString(2, category.name)
                setLong(3, maxId + 1)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, userId.toString(), category.name ?: "no name", (maxId + 1).toString()))
    }

    fun getUserCategories(userId: Long) : List<DbCategory> {
         return conn.safeExecute(SELECT_BY_USER, {
            with(it){
                setLong(1, userId)
                val res = executeQuery()
                val categories = arrayListOf<DbCategory>()
                while (res.next()) {
                    categories.add(DbCategory(res.getLong("id"), userId, res.getString("name"), res.getLong("rank")))
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
                setLong(2, category.rank!!)
                setLong(3, categoryId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE, category.name ?: "no name", category.rank.toString(), categoryId.toString()))
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

    fun rankDownCategory(userId: Long, categoryId: Long) {
        val dbCategory = getCategory(categoryId)
        val otherCat = getOtherCategory(userId, dbCategory, SELECT_CAT_WITH_SMALLER_RANK)
            ?: throw Exception("There is no category with smaller rank, could not proceed.")

        switchCategory(dbCategory, otherCat)
    }

    fun rankUpCategory(userId: Long, categoryId: Long) {
        val dbCategory = getCategory(categoryId)
        val otherCat = getOtherCategory(userId, dbCategory, SELECT_CAT_WITH_HIGHER_RANK)
            ?: throw Exception("There is no category with higher rank, could not proceed.")

        switchCategory(dbCategory, otherCat)
    }

    private fun getCategory(categoryId: Long): DbCategory {
        return conn.safeExecute(SELECT_BY_ID, {
            with(it) {
                setLong(1, categoryId)
                val rs = executeQuery()
                if (!rs.next()) throw IllegalStateException("No category $categoryId")
                return@with DbCategory(
                    rs.getLong("id"),
                    rs.getLong("userId"),
                    rs.getString("name"),
                    rs.getLong("rank")
                )
            }
        }, errorMessage(SELECT_BY_ID, categoryId.toString()))
    }

    private fun getOtherCategory(userId: Long, dbCategory: DbCategory, query: String): DbCategory? {
        return conn.safeExecute(query, {
            with(it) {
                setLong(1, userId)
                setLong(2, userId)
                setLong(3, dbCategory.rank)
                val rs = executeQuery()
                if (!rs.next()) return@with null
                return@with DbCategory(rs.getLong("id"), rs.getLong("userId"), rs.getString("name"), rs.getLong("rank"))
            }
        }, errorMessage(SELECT_CAT_WITH_SMALLER_RANK, userId.toString(), userId.toString(), dbCategory.rank.toString()))
    }

    private fun switchCategory(dbCategory: DbCategory, downCat: DbCategory) {
        modifyCategory(dbCategory.id, RestCategory(dbCategory.name, downCat.rank))
        try {
            modifyCategory(downCat.id, RestCategory(downCat.name, dbCategory.rank))
        } catch (e: DbException) {
            //Try to reverse first switch
            modifyCategory(dbCategory.id, RestCategory(dbCategory.name, dbCategory.rank))
            throw DbException("No change applied", e)
        }
    }

    private fun getCurrentMaxRank(userId: Long) : Long {
        return conn.safeExecute(SELECT_MAX_RANK, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                if (res.next()) {
                    return@with res.getLong(1)
                }
                return@with 0
            }
        }, errorMessage(SELECT_MAX_RANK, userId.toString()))
    }
}