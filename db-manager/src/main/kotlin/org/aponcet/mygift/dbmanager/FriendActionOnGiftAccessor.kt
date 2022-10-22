package org.aponcet.mygift.dbmanager

import java.sql.ResultSet

class FriendActionOnGiftAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO friendActionOnGift(giftId,userId) VALUES (?, ?)"
        const val SELECT_SOME_BY_GIFT = "SELECT * FROM friendActionOnGift WHERE giftId = ?"
        const val SELECT_SOME_BY_USER = "SELECT * FROM friendActionOnGift WHERE userId = ?"
        const val DELETE = "DELETE FROM friendActionOnGift WHERE giftId = ? AND userId = ?"
    }

    override fun getTableName(): String {
        return "friendActionOnGift"
    }

    override fun createIfNotExists() {
        conn.execute(
            "CREATE TABLE IF NOT EXISTS friendActionOnGift (" +
                    "id             INTEGER PRIMARY KEY ${conn.autoIncrement}, " +
                    "giftId         INTEGER NOT NULL, " +
                    "userId         INTEGER NOT NULL, " +
                    "UNIQUE(giftId, userId) ON CONFLICT IGNORE," +
                    "FOREIGN KEY(userId) REFERENCES users(id), " +
                    "FOREIGN KEY(giftId) REFERENCES gifts(id))"
        )
    }

    fun insert(giftId: Long, userId: Long) {
        conn.safeExecute(INSERT, {
            with(it) {
                setLong(1, giftId)
                setLong(2, userId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, giftId.toString(), userId.toString()))
    }

    fun delete(giftId: Long, userId: Long) {
        conn.safeExecute(DELETE, {
            with(it) {
                setLong(1, giftId)
                setLong(2, userId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(DELETE, giftId.toString(), userId.toString()))
    }

    fun getFriendActionOnGift(giftId: Long): List<DbFriendActionOnGift> {
        return conn.safeExecute(SELECT_SOME_BY_GIFT, {
            with(it) {
                setLong(1, giftId)
                val res = executeQuery()
                return@with queryAnswerToDbFriendActionOnGift(res)
            }
        }, errorMessage(SELECT_SOME_BY_GIFT, giftId.toString()))
    }

    fun getFriendActionOnGiftsUserHasActionOn(userId: Long): List<DbFriendActionOnGift> {
        return conn.safeExecute(SELECT_SOME_BY_USER, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                return@with queryAnswerToDbFriendActionOnGift(res)
            }
        }, errorMessage(SELECT_SOME_BY_GIFT, userId.toString()))
    }

    private fun queryAnswerToDbFriendActionOnGift(res: ResultSet): ArrayList<DbFriendActionOnGift> {
        val out = arrayListOf<DbFriendActionOnGift>()
        while (res.next()) {
            out.add(convertOneRes(res))
        }
        return out
    }

    private fun convertOneRes(res: ResultSet): DbFriendActionOnGift {
        return DbFriendActionOnGift(
            res.getLong("id"),
            res.getLong("giftId"),
            res.getLong("userId")
        )
    }
}