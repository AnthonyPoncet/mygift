package org.aponcet.mygift.dbmanager

import java.sql.ResultSet

class FriendActionOnGiftAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO friendActionOnGift(giftId,userId,interested,buy) VALUES (?, ?, ?, ?)"
        const val SELECT_BY_GIFT_AND_USER = "SELECT * FROM friendActionOnGift WHERE giftId = ? AND userId = ?"
        const val SELECT_SOME_BY_GIFT = "SELECT * FROM friendActionOnGift WHERE giftId = ?"
        const val SELECT_SOME_BY_USER = "SELECT * FROM friendActionOnGift WHERE userId = ?"
        const val UPDATE_INTERESTED = "UPDATE friendActionOnGift SET interested=? WHERE giftId = ? AND userId = ?"
        const val UPDATE_BUY = "UPDATE friendActionOnGift SET buy=? WHERE giftId = ? AND userId = ?"
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
                    "interested     INTEGER NOT NULL, " +
                    "buy            TEXT NOT NULL, " +
                    "FOREIGN KEY(userId) REFERENCES users(id), " +
                    "FOREIGN KEY(giftId) REFERENCES gifts(id))"
        )
    }

    private fun insert(giftId: Long, userId: Long, interested: Boolean, buy: BuyAction) {
        conn.safeExecute(INSERT, {
            with(it) {
                setLong(1, giftId)
                setLong(2, userId)
                setBoolean(3, interested)
                setString(4, buy.name)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, giftId.toString(), userId.toString(), interested.toString(), buy.name))
    }

    private fun updateInterested(giftId: Long, userId: Long, interested: Boolean) {
        conn.safeExecute(UPDATE_INTERESTED, {
            with(it) {
                setBoolean(1, interested)
                setLong(2, giftId)
                setLong(3, userId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE_INTERESTED, interested.toString(), giftId.toString(), userId.toString()))
    }

    private fun updateBuy(giftId: Long, userId: Long, buy: BuyAction) {
        conn.safeExecute(UPDATE_BUY, {
            with(it) {
                setString(1, buy.name)
                setLong(2, giftId)
                setLong(3, userId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE_BUY, buy.name, giftId.toString(), userId.toString()))
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

    fun interested(giftId: Long, userId: Long, interested: Boolean) {
        val current = getActionByUserAndGift(giftId, userId)
        if (current != null) {
            val currentInterested = current.interested
            val currentBuy = current.buy

            if (currentInterested == interested) return
            else if (interested || currentBuy != BuyAction.NONE) {
                updateInterested(giftId, userId, interested)
            } else {
                delete(giftId, userId)
            }
        } else if (interested) {
            insert(giftId, userId, interested, BuyAction.NONE)
        }
    }

    fun buyAction(giftId: Long, userId: Long, buy: BuyAction) {
        val current = getActionByUserAndGift(giftId, userId)
        if (current != null) {
            val currentInterested = current.interested
            val currentBuy = current.buy

            if (currentBuy == buy) return
            else if (currentInterested || buy != BuyAction.NONE) {
                updateBuy(giftId, userId, buy)
            } else {
                delete(giftId, userId)
            }
        } else if (buy != BuyAction.NONE) {
            insert(giftId, userId, false, buy)
        }
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
            res.getLong("userId"),
            res.getBoolean("interested"),
            BuyAction.valueOf(res.getString("buy"))
        )
    }

    private fun getActionByUserAndGift(giftId: Long, userId: Long): DbFriendActionOnGift? {
        return conn.safeExecute(SELECT_BY_GIFT_AND_USER, {
            with(it) {
                setLong(1, giftId)
                setLong(2, userId)
                val res = executeQuery()
                return@with if (res.next()) convertOneRes(res) else null
            }
        }, errorMessage(SELECT_BY_GIFT_AND_USER, giftId.toString(), userId.toString()))
    }
}