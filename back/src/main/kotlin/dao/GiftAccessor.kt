package dao

import RestGift

class GiftAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO gifts (userId,name,description,price,whereToBuy,categoryId,picture,secret) VALUES " +
                "(?,?,?,?,?,?,?,?)"
        const val SELECT_BY_ID = "SELECT * FROM gifts WHERE id=?"
        const val SELECT_SOME_BY_USER = "SELECT * FROM gifts WHERE userId=?"
        const val SELECT_SOME_BY_USER_NO_SECRET = "SELECT * FROM gifts WHERE userId=? AND secret=0"
        const val SELECT_BY_ID_AND_USER = "SELECT * FROM gifts WHERE id=? AND userId=?"
        const val UPDATE = "UPDATE gifts SET name = ?, description = ?, price = ?, whereToBuy = ?, categoryId = ?, picture = ? WHERE id = ?"
        const val DELETE = "DELETE FROM gifts WHERE id = ?"
        const val IS_SECRET = "SELECT * FROM gifts WHERE id=? AND secret=1"
    }

    override fun getTableName(): String {
        return "gifts"
    }

    override fun createIfNotExists() {
        conn.execute("CREATE TABLE IF NOT EXISTS gifts (" +
            "id             INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "userId         INTEGER NOT NULL, " +
            "name           TEXT NOT NULL, " +
            "description    TEXT, " +
            "price          TEXT, " +
            "whereToBuy     TEXT, " +
            "categoryId     INTEGER NOT NULL, " +
            "picture        TEXT, " +
            "secret         INTEGER NOT NULL, " +
            "FOREIGN KEY(userId) REFERENCES users(id), " +
            "FOREIGN KEY(categoryId) REFERENCES categories(id))")
    }

    fun addGift(userId: Long, gift: RestGift, secret: Boolean) {
        conn.safeExecute(INSERT, {
            with(it) {
                setLong(1, userId)
                setString(2, gift.name)
                setString(3, gift.description ?: "")
                setString(4, gift.price ?: "")
                setString(5, gift.whereToBuy ?: "")
                setLong(6, gift.categoryId!!)
                setString(7, gift.picture ?: "")
                setBoolean(8, secret)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, userId.toString(), gift.toString(), secret.toString()))
    }

    fun getGift(giftId: Long) : DbGift? {
        return conn.safeExecute(SELECT_BY_ID, {
            with(it) {
                setLong(1, giftId)
                val res = executeQuery()
                return@with if (res.next()) {
                    val picture = res.getString("picture")
                    DbGift(
                        res.getLong("id"),
                        res.getLong("userId"),
                        res.getString("name"),
                        res.getString("description"),
                        res.getString("price"),
                        res.getString("whereToBuy"),
                        res.getLong("categoryId"),
                        if (picture.isEmpty()) null else picture,
                        res.getBoolean("secret")
                    )
                } else {
                    null
                }
            }
        }, errorMessage(SELECT_BY_ID, giftId.toString()))
    }

    private fun getGifts(userId: Long, withSecret: Boolean) : List<DbGift> {
        return conn.safeExecute(if (withSecret) SELECT_SOME_BY_USER else SELECT_SOME_BY_USER_NO_SECRET,{
            val gifts = arrayListOf<DbGift>()
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                while (res.next()) {
                    val picture = res.getString("picture")
                    gifts.add(
                        DbGift(
                            res.getLong("id"),
                            res.getLong("userId"),
                            res.getString("name"),
                            res.getString("description"),
                            res.getString("price"),
                            res.getString("whereToBuy"),
                            res.getLong("categoryId"),
                            if (picture.isEmpty()) null else picture,
                            res.getBoolean("secret")
                        )
                    )
                }
                return@with gifts
            }
        }, errorMessage(if(withSecret) SELECT_SOME_BY_USER else SELECT_SOME_BY_USER_NO_SECRET, userId.toString()))
    }

    /** Return gift for a given user, secret gift will be filter out */
    fun getUserGifts(userId: Long) : List<DbGift> {
        return getGifts(userId, false)
    }

    /** Return gift for a given friend, secret gift will be returned */
    fun getFriendGifts(friendId: Long): List<DbGift> {
        return getGifts(friendId, true)
    }

    fun modifyGift(userId: Long, giftId: Long, gift: RestGift) {
        conn.safeExecute(UPDATE, {
            with(it) {
                setLong(1, userId)
                setString(2, gift.name)
                setString(3, gift.description ?: "")
                setString(4, gift.price ?: "")
                setString(5, gift.whereToBuy ?: "")
                setLong(6, gift.categoryId!!)
                setString(7, gift.picture ?: "")
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, userId.toString(), gift.toString()))
    }

    fun removeGift(giftId: Long) {
        conn.safeExecute(DELETE, {
            with(it) {
                setLong(1, giftId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(DELETE, giftId.toString()))
    }

    fun giftExists(giftId: Long): Boolean {
        return conn.safeExecute(SELECT_BY_ID,{
            with(it) {
                setLong(1, giftId)
                return@with executeQuery().next()
            }
        }, errorMessage(DELETE, giftId.toString()))
    }

    fun giftBelongToUser(userId: Long, giftId: Long): Boolean {
        return conn.safeExecute(SELECT_BY_ID_AND_USER, {
            with(it) {
                setLong(1, giftId)
                setLong(2, userId)
                return@with executeQuery().next()
            }
        }, errorMessage(SELECT_BY_ID_AND_USER, giftId.toString(), userId.toString()))
    }

    fun giftIsSecret(giftId: Long): Boolean {
        return conn.safeExecute(IS_SECRET, {
            with(it) {
                setLong(1, giftId)
                return@with executeQuery().next()
            }
        }, errorMessage(IS_SECRET, giftId.toString()))
    }
}