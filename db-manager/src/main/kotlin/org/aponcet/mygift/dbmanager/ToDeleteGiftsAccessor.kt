package org.aponcet.mygift.dbmanager

enum class Status { RECEIVED, NOT_WANTED }

class ToDeleteGiftsAccessor(private val conn: DbConnection) : DaoAccessor() {
    companion object {
        const val INSERT =
            "INSERT INTO toDeleteGifts (giftId,giftUserId,name,description,price,whereToBuy,picture,giftUserStatus,friendId,friendAction) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?)"
        const val SELECT_SOME_BY_FRIEND_ID = "SELECT * FROM toDeleteGifts WHERE friendId=?"
        const val DELETE = "DELETE FROM toDeleteGifts WHERE giftId=? AND friendId=?"
    }

    override fun getTableName(): String {
        return "toDeleteGifts"
    }

    override fun createIfNotExists() {
        conn.execute(
            "CREATE TABLE IF NOT EXISTS toDeleteGifts (" +
                    "giftId         INTEGER NOT NULL, " +
                    "giftUserId     INTEGER NOT NULL, " + //user id of the gift owner
                    "name           TEXT NOT NULL, " +
                    "description    TEXT, " +
                    "price          TEXT, " +
                    "whereToBuy     TEXT, " +
                    "picture        TEXT, " +
                    "giftUserStatus TEXT NOT NULL, " + //why the gift owner deleted the gift
                    "friendId       INTEGER NOT NULL, " + //friend user id that had an action on the gift
                    "friendAction   TEXT NOT NULL, " + //friend action that user had on the gift
                    "FOREIGN KEY(giftUserId) REFERENCES users(id), FOREIGN KEY(friendId) REFERENCES users(id))"
        )
    }

    fun add(gift: DbGift, giftUserId: Long, status: Status, friendActionOnGift: DbFriendActionOnGift) {
        conn.safeExecute(INSERT, {
            with(it) {
                setLong(1, gift.id)
                setLong(2, giftUserId)
                setString(3, gift.name)
                setString(4, gift.description)
                setString(5, gift.price)
                setString(6, gift.whereToBuy)
                setString(7, gift.picture)
                setString(8, status.name)
                setLong(9, friendActionOnGift.userId)
                setString(10, friendActionOnGift.buy.name)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, gift.toString()))
    }

    fun getDeletedGiftsWhereUserHasActionOn(friendId: Long): List<DbToDeleteGifts> {
        return conn.safeExecute(SELECT_SOME_BY_FRIEND_ID, {
            val toDeleteGifts = arrayListOf<DbToDeleteGifts>()
            with(it) {
                setLong(1, friendId)
                val res = executeQuery()
                while (res.next()) {
                    toDeleteGifts.add(
                        DbToDeleteGifts(
                            res.getLong("giftId"),
                            res.getLong("giftUserId"),
                            res.getString("name"),
                            res.getString("description"),
                            res.getString("price"),
                            res.getString("whereToBuy"),
                            res.getString("picture"),
                            Status.valueOf(res.getString("giftUserStatus")),
                            res.getLong("friendId"),
                            BuyAction.valueOf(res.getString("friendAction"))
                        )
                    )
                }
                return@with toDeleteGifts
            }
        }, errorMessage(SELECT_SOME_BY_FRIEND_ID, friendId.toString()))
    }

    //Delete an action on a gift from a given friend
    fun deleteDeletedGift(giftId: Long, friendId: Long) {
        conn.safeExecute(DELETE, {
            with(it) {
                setLong(1, giftId)
                setLong(2, friendId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(DELETE, giftId.toString()))
    }
}