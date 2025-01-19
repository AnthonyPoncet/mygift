package org.aponcet.mygift.dbmanager

import org.aponcet.mygift.dbmanager.CategoryAccessor.Companion.INSERT

class JoinUserAndCategoryAccessor(private val conn: DbConnection) : DaoAccessor() {
    companion object {
        const val ADD_CATEGORY = "INSERT into joinUserAndCategory(userId, categoryId, rank) VALUES (?,?,?)"
        const val SELECT_BY_USER_ID = "SELECT categoryId from joinUserAndCategory where userId=?"
        const val SELECT_BY_CATEGORY_ID = "SELECT userId from joinUserAndCategory where categoryId=?"
        const val UPDATE = "UPDATE joinUserAndCategory SET rank=? WHERE userId=? AND categoryId=?"
        const val DELETE_BY_CATEGORY_ID = "DELETE from joinUserAndCategory where categoryId=?"
        const val SELECT_MAX_RANK = "SELECT MAX(rank) FROM joinUserAndCategory WHERE userId=?"
    }

    override fun getTableName(): String {
        return "joinUserAndCategory"
    }

    override fun createIfNotExists() {
        conn.execute(
            "CREATE TABLE IF NOT EXISTS joinUserAndCategory (" +
                    "userId INTEGER NOT NULL, " +
                    "categoryId INTEGER NOT NULL, " +
                    "rank INTEGER NOT NULL," +
                    "FOREIGN KEY(userId) REFERENCES users(id)," +
                    "FOREIGN KEY(categoryId) REFERENCES categories(id))"
        )
    }

    private fun addCategory(userId: Long, categoryId: Long) {
        val maxId = getCurrentMaxRank(userId)
        conn.safeExecute(ADD_CATEGORY, {
            with(it) {
                setLong(1, userId)
                setLong(2, categoryId)
                setLong(3, maxId + 1)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(ADD_CATEGORY, userId.toString(), categoryId.toString(), (maxId + 1).toString()))
    }

    fun addCategory(userIds: Collection<Long>, categoryId: Long) {
        userIds.forEach { addCategory(it, categoryId) }
    }

    fun getCategories(userId: Long): Set<Long> {
        return conn.safeExecute(SELECT_BY_USER_ID, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                val categoryIds = HashSet<Long>()
                while (res.next()) {
                    categoryIds.add(res.getLong(1))
                }
                return@with categoryIds
            }
        }, errorMessage(SELECT_BY_USER_ID, userId.toString()))
    }

    fun getUsers(categoryId: Long): Set<Long> {
        return conn.safeExecute(SELECT_BY_CATEGORY_ID, {
            with(it) {
                setLong(1, categoryId)
                val res = executeQuery()
                val users = HashSet<Long>()
                while (res.next()) {
                    users.add(res.getLong(1))
                }
                return@with users
            }
        }, errorMessage(SELECT_BY_CATEGORY_ID, categoryId.toString()))
    }

    fun modifyRank(userId: Long, categoryId: Long, rank: Long) {
        conn.safeExecute(UPDATE, {
            with(it) {
                setLong(1, rank)
                setLong(2, userId)
                setLong(3, categoryId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE, rank.toString(), userId.toString(), categoryId.toString()))
    }

    fun deleteCategory(categoryId: Long) {
        conn.safeExecute(DELETE_BY_CATEGORY_ID, {
            with(it) {
                setLong(1, categoryId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(DELETE_BY_CATEGORY_ID, categoryId.toString()))
    }

    private fun getCurrentMaxRank(userId: Long): Long {
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