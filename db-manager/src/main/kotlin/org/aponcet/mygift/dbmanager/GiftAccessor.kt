package org.aponcet.mygift.dbmanager

data class NewGift(val name: String, val description: String? = null, val price: String? = null, val whereToBuy: String? = null, val categoryId: Long, val picture: String? = null)
data class Gift(val name: String, val description: String? = null, val price: String? = null, val whereToBuy: String? = null, val categoryId: Long, val picture: String? = null, val rank: Long)

class GiftAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO gifts (userId,name,description,price,whereToBuy,categoryId,picture,secret,rank) " +
                "VALUES (?,?,?,?,?,?,?,?,?)"
        const val SELECT_BY_ID = "SELECT * FROM gifts WHERE id=?"
        const val SELECT_SOME_BY_USER = "SELECT * FROM gifts WHERE userId=? ORDER BY rank ASC"
        const val SELECT_SOME_BY_USER_NO_SECRET = "SELECT * FROM gifts WHERE userId=? AND secret=0 ORDER BY rank ASC"
        const val SELECT_BY_ID_AND_USER = "SELECT * FROM gifts WHERE id=? AND userId=?"
        const val SELECT_MAX_RANK_OF_GIVEN_CATEGORY = "SELECT MAX(rank) FROM gifts WHERE userId=? AND categoryId=?"
        const val UPDATE = "UPDATE gifts SET name=?, description=?, price=?, whereToBuy=?, categoryId=?, picture=?, rank=? WHERE id=?"
        const val DELETE = "DELETE FROM gifts WHERE id=?"
        const val IS_SECRET = "SELECT * FROM gifts WHERE id=? AND secret=1"

        const val SELECT_NO_SECRET_GIFT_WITH_SMALLER_RANK = "SELECT * FROM gifts WHERE userId=? AND categoryId=? AND rank=(SELECT MAX(rank) FROM gifts WHERE userId=? AND categoryId=? AND rank<? AND secret=0)"
        const val SELECT_NO_SECRET_GIFT_WITH_HIGHER_RANK = "SELECT * FROM gifts WHERE userId=? AND categoryId=? AND rank=(SELECT MIN(rank) FROM gifts WHERE userId=? AND categoryId=? AND rank>? AND secret=0)"

    }

    override fun getTableName(): String {
        return "gifts"
    }

    override fun createIfNotExists() {
        conn.execute("CREATE TABLE IF NOT EXISTS gifts (" +
            "id             INTEGER PRIMARY KEY ${conn.autoIncrement}, " +
            "userId         INTEGER NOT NULL, " +
            "name           TEXT NOT NULL, " +
            "description    TEXT, " +
            "price          TEXT, " +
            "whereToBuy     TEXT, " +
            "categoryId     INTEGER NOT NULL, " +
            "picture        TEXT, " +
            "secret         INTEGER NOT NULL, " +
            "rank           INTEGER NOT NULL," +
            "FOREIGN KEY(userId) REFERENCES users(id), " +
            "FOREIGN KEY(categoryId) REFERENCES categories(id))")
    }

    fun addGift(userId: Long, gift: NewGift, secret: Boolean) {
        val maxId = getCurrentMaxRankOfGivenCategory(userId, gift.categoryId)
        conn.safeExecute(INSERT, {
            with(it) {
                setLong(1, userId)
                setString(2, gift.name)
                setString(3, gift.description)
                setString(4, gift.price)
                setString(5, gift.whereToBuy)
                setLong(6, gift.categoryId)
                setString(7, gift.picture)
                setBoolean(8, secret)
                setLong(9, maxId + 1)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, userId.toString(), gift.toString(), secret.toString(), (maxId + 1).toString()))
    }

    fun getGift(giftId: Long) : DbGift? {
        return conn.safeExecute(SELECT_BY_ID, {
            with(it) {
                setLong(1, giftId)
                val res = executeQuery()
                return@with if (res.next()) {
                    DbGift(
                        res.getLong("id"),
                        res.getLong("userId"),
                        res.getString("name"),
                        res.getString("description"),
                        res.getString("price"),
                        res.getString("whereToBuy"),
                        res.getLong("categoryId"),
                        res.getString("picture"),
                        res.getBoolean("secret"),
                        res.getLong("rank")
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
                    gifts.add(
                        DbGift(
                            res.getLong("id"),
                            res.getLong("userId"),
                            res.getString("name"),
                            res.getString("description"),
                            res.getString("price"),
                            res.getString("whereToBuy"),
                            res.getLong("categoryId"),
                            res.getString("picture"),
                            res.getBoolean("secret"),
                            res.getLong("rank")
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

    fun modifyGift(giftId: Long, gift: Gift) {
        conn.safeExecute(UPDATE, {
            with(it) {
                setString(1, gift.name)
                setString(2, gift.description)
                setString(3, gift.price)
                setString(4, gift.whereToBuy)
                setLong(5, gift.categoryId)
                setString(6, gift.picture)
                setLong(7, gift.rank)
                setLong(8, giftId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE, giftId.toString(), gift.toString()))
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

    private fun getCurrentMaxRankOfGivenCategory(userId: Long, categoryId: Long) : Long {
        return conn.safeExecute(SELECT_MAX_RANK_OF_GIVEN_CATEGORY, {
            with(it) {
                setLong(1, userId)
                setLong(2, categoryId)
                val res = executeQuery()
                if (res.next()) {
                    return@with res.getLong(1)
                }
                return@with 0
            }
        }, errorMessage(SELECT_MAX_RANK_OF_GIVEN_CATEGORY, userId.toString(), categoryId.toString()))
    }

    fun rankDownGift(userId: Long, giftId: Long) {
        val gift = getGift(giftId)!!
        val otherGift = getOtherGift(userId, gift,
            SELECT_NO_SECRET_GIFT_WITH_SMALLER_RANK
        )
            ?: throw Exception("There is no gift with smaller rank, could not proceed.")

        switchGift(gift, otherGift)
    }

    fun rankUpGift(userId: Long, giftId: Long) {
        val gift = getGift(giftId)!!
        val otherGift = getOtherGift(userId, gift,
            SELECT_NO_SECRET_GIFT_WITH_HIGHER_RANK
        )
            ?: throw Exception("There is no gift with higher rank, could not proceed.")

        switchGift(gift, otherGift)
    }

    private fun getOtherGift(userId: Long, gift: DbGift, query: String): DbGift? {
        return conn.safeExecute(query, {
            with(it) {
                setLong(1, userId)
                setLong(2, gift.categoryId)
                setLong(3, userId)
                setLong(4, gift.categoryId)
                setLong(5, gift.rank)
                val rs = executeQuery()
                if (!rs.next()) return@with null
                return@with DbGift(
                    rs.getLong("id"),
                    rs.getLong("userId"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("price"),
                    rs.getString("whereToBuy"),
                    rs.getLong("categoryId"),
                    rs.getString("picture"),
                    rs.getBoolean("secret"),
                    rs.getLong("rank")
                )
            }
        }, errorMessage(query, userId.toString(), userId.toString(), gift.rank.toString()))
    }

    private fun switchGift(gift: DbGift, otherGift: DbGift) {
        modifyGift(gift.id, to(gift, otherGift.rank))
        try {
            modifyGift(otherGift.id, to(otherGift, gift.rank))
        } catch (e: DbException) {
            //Try to reverse first switch
            modifyGift(gift.id, to(gift, gift.rank))
            throw DbException("No change applied", e)
        }
    }

    private fun to(gift: DbGift, newRank: Long): Gift {
        return Gift(gift.name, gift.description, gift.price, gift.whereToBuy, gift.categoryId, gift.picture, newRank)
    }
}