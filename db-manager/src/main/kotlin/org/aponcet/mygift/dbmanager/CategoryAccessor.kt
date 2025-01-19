package org.aponcet.mygift.dbmanager

import java.util.*
import java.util.stream.Collectors

data class NewCategory(val name: String)
data class Category(val name: String, val rank: Long)

class CategoryAccessor(private val conn: DbConnection) : DaoAccessor() {

    private val joinUserAndCategoryAccessor = JoinUserAndCategoryAccessor(conn)

    companion object {
        const val INSERT = "INSERT INTO categories(name) VALUES (?)"
        const val SELECT_BY_ID =
            "SELECT id, name, rank FROM categories C LEFT JOIN joinUserAndCategory J on C.id = J.categoryId WHERE C.id=?"
        const val SELECT_BY_ID_AND_USER_ID =
            "SELECT id, name, rank FROM categories C LEFT JOIN joinUserAndCategory J on C.id = J.categoryId WHERE C.id=? AND J.userId=?"
        const val SELECT_FRIEND_CATEGORY =
            "select id, name, rank FROM categories C LEFT JOIN joinUserAndCategory J on C.id = J.categoryId where J.userId=? and c.id not in (select id FROM categories C LEFT JOIN joinUserAndCategory J on C.id = J.categoryId where J.userId=?) ORDER BY rank"
        const val SELECT_BY_USER_ID =
            "WITH cat_to_share as (SELECT categoryId, GROUP_CONCAT(userId) as userIds FROM joinUserAndCategory GROUP BY joinUserAndCategory.categoryId) " +
            "SELECT C.id, C.name, J.rank, CS.userIds " +
            "FROM categories C " +
            "LEFT JOIN cat_to_share CS on C.id = CS.categoryId " +
            "LEFT JOIN joinUserAndCategory J on C.id = J.categoryId " +
            "WHERE J.userId=? ORDER BY rank"
        const val UPDATE = "UPDATE categories SET name=? WHERE id=?"
        const val DELETE = "DELETE FROM categories WHERE id=?"

        const val SELECT_CAT_WITH_SMALLER_RANK =
            "SELECT id, name, rank FROM categories C LEFT JOIN joinUserAndCategory J on C.id = J.categoryId WHERE userId=? AND rank=(SELECT MAX(rank) FROM joinUserAndCategory WHERE userId=? AND rank<?)"
        const val SELECT_CAT_WITH_HIGHER_RANK =
            "SELECT id, name, rank FROM categories C LEFT JOIN joinUserAndCategory J on C.id = J.categoryId WHERE userId=? AND rank=(SELECT MIN(rank) FROM joinUserAndCategory WHERE userId=? AND rank>?)"
    }

    override fun getTableName(): String {
        return "categories"
    }

    override fun createIfNotExists() {
        conn.execute(
            "CREATE TABLE IF NOT EXISTS categories (" +
                    "id     INTEGER PRIMARY KEY ${conn.autoIncrement}, " +
                    "name   TEXT NOT NULL)"
        )
    }

    fun addCategory(name: String, userIds: List<Long>) {
        val categoryId = conn.safeExecute(INSERT, {
            with(it) {
                setString(1, name)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                if (generatedKeys.next()) {
                    return@with generatedKeys.getLong(1)
                } else {
                    throw Exception("executeUpdate, no key generated")
                }
            }
        }, errorMessage(INSERT, name))

        joinUserAndCategoryAccessor.addCategory(userIds, categoryId)
    }

    fun getUserCategories(userId: Long): List<DbCategory> {
        return conn.safeExecute(SELECT_BY_USER_ID, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                val categories = arrayListOf<DbCategory>()
                while (res.next()) {
                    //array are stupid and does not do what it is supposed to do, it returns an array of one CSV string
                    val share = res.getString(4)
                        .split(",")
                        .map { v -> v.toLong() }
                        .filter { u -> u != userId }
                        .toCollection(hashSetOf())

                    categories.add(
                        DbCategory(
                            res.getLong("id"),
                            res.getString("name"),
                            res.getLong("rank"),
                            share
                        )
                    )
                }
                return@with categories
            }
        }, errorMessage(SELECT_BY_USER_ID, userId.toString()))
    }

    fun getFriendCategories(userId: Long, friendId: Long): List<DbCategory> {
        return conn.safeExecute(SELECT_FRIEND_CATEGORY, {
            with(it) {
                setLong(1, friendId)
                setLong(2, userId)
                val res = executeQuery()
                val categories = arrayListOf<DbCategory>()
                while (res.next()) {
                    categories.add(
                        DbCategory(
                            res.getLong("id"),
                            res.getString("name"),
                            res.getLong("rank"),
                            setOf()
                        )
                    )
                }
                return@with categories
            }
        }, errorMessage(SELECT_FRIEND_CATEGORY, userId.toString()))
    }

    fun modifyCategory(categoryId: Long, name: String) {
        conn.safeExecute(UPDATE, {
            with(it) {
                setString(1, name)
                setLong(2, categoryId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE, name, categoryId.toString()))
    }

    fun removeCategory(categoryId: Long) {
        joinUserAndCategoryAccessor.deleteCategory(categoryId)

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
        return joinUserAndCategoryAccessor.getCategories(userId).contains(categoryId)
    }

    fun rankDownCategory(userId: Long, categoryId: Long) {
        val dbCategory = getCategory(userId, categoryId)
        val otherCat = getOtherCategory(userId, dbCategory, SELECT_CAT_WITH_SMALLER_RANK)
            ?: throw Exception("There is no category with smaller rank, could not proceed.")

        switchCategory(userId, dbCategory, otherCat)
    }

    fun rankUpCategory(userId: Long, categoryId: Long) {
        val dbCategory = getCategory(userId, categoryId)
        val otherCat = getOtherCategory(userId, dbCategory, SELECT_CAT_WITH_HIGHER_RANK)
            ?: throw Exception("There is no category with higher rank, could not proceed.")

        switchCategory(userId, dbCategory, otherCat)
    }

    private fun getCategory(userId: Long, categoryId: Long): DbCategory {
        return conn.safeExecute(SELECT_BY_ID_AND_USER_ID, {
            with(it) {
                setLong(1, categoryId)
                setLong(2, userId)
                val rs = executeQuery()
                if (!rs.next()) throw IllegalStateException("No category $categoryId")
                return@with DbCategory(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getLong("rank"),
                    setOf()
                )
            }
        }, errorMessage(SELECT_BY_ID_AND_USER_ID, categoryId.toString()))
    }

    private fun getOtherCategory(userId: Long, dbCategory: DbCategory, query: String): DbCategory? {
        return conn.safeExecute(query, {
            with(it) {
                setLong(1, userId)
                setLong(2, userId)
                setLong(3, dbCategory.rank)
                val rs = executeQuery()
                if (!rs.next()) return@with null
                return@with DbCategory(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getLong("rank"),
                    setOf()
                )
            }
        }, errorMessage(query, userId.toString(), dbCategory.rank.toString()))
    }

    private fun switchCategory(userId: Long, dbCategory: DbCategory, downCat: DbCategory) {
        joinUserAndCategoryAccessor.modifyRank(userId, dbCategory.id, downCat.rank)
        try {
            joinUserAndCategoryAccessor.modifyRank(userId, downCat.id, dbCategory.rank)
        } catch (e: DbException) {
            //Try to reverse first switch
            joinUserAndCategoryAccessor.modifyRank(userId, dbCategory.id, dbCategory.rank)
            throw DbException("No change applied", e)
        }
    }
}