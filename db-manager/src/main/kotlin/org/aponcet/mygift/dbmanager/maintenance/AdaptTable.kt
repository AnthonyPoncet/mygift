package org.aponcet.mygift.dbmanager.maintenance

import org.aponcet.mygift.dbmanager.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class UserBeforeMigration(val id: Long, val name: String, val password: String, val picture: String)
data class CategoryBeforeMigration(val id: Long, val userId: Long, val name: String, val rank: Long)
data class GiftBeforeMigration(
    val id: Long,
    val userId: Long,
    val name: String,
    val description: String?,
    val price: String?,
    val whereToBuy: String?,
    val categoryId: Long,
    val picture: String?,
    val secret: Boolean,
    val rank: Long
)

data class FriendActionOnGiftBeforeMigration(
    val id: Long,
    val giftId: Long,
    val userId: Long,
    val interested: Boolean,
    val buy: String
)

/** Adapt table used only ad-hoc when needed **/
class AdaptTable(dbPath: String) {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(AdaptTable::class.java)
    }

    private val conn = DbConnection("sqlite", dbPath)

    enum class STEP { ADD_RANK_TO_CATEGORY, ADD_RANK_TO_GIFT, ADD_SALT_TO_USER, DELETE_EVENTS, COMMON_CATEGORIES, ADD_HEART_TO_GIFTS, ADD_DOB }

    fun execute(step: STEP) {
        when (step) {
            STEP.ADD_RANK_TO_CATEGORY -> addRankToCategory()
            STEP.ADD_RANK_TO_GIFT -> addRankToGift()
            STEP.ADD_SALT_TO_USER -> addSaltToUser()
            STEP.DELETE_EVENTS -> deleteEvents()
            STEP.COMMON_CATEGORIES -> commonCategories()
            STEP.ADD_HEART_TO_GIFTS -> addHeartToGifts()
            STEP.ADD_DOB -> addDateOfBirth()
        }
    }

    /**
     * 1. Will add rank column if needed
     * 2. Will reset rank from 1 to X keeping current rank order
     *   --> Allow to init rank to default value
     *   --> If rank is too high because too many categories have been added and deleted, could fix that.
     */
    private fun addRankToCategory() {
        try {
            conn.executeQuery("SELECT rank FROM categories")
            LOGGER.info("Column rank exists, will adapt rank values")
        } catch (e: Exception) {
            LOGGER.info("Column rank does not exist, add it")
            conn.executeUpdate("ALTER TABLE categories ADD COLUMN 'rank' INTEGER NOT NULL DEFAULT 1")
        }

        val userIds = HashSet<Long>()
        val rs = conn.executeQuery("SELECT userId FROM categories")
        while (rs.next()) {
            userIds.add(rs.getLong("userId"))
        }

        LOGGER.info("Will update category of users $userIds")

        val categoryAccessor = CategoryAccessor(conn)
        val joinUserAndCategoryAccessor = JoinUserAndCategoryAccessor(conn)
        for (userId in userIds) {
            val categories = categoryAccessor.getUserCategories(userId)
            LOGGER.info("User $userId has categories $categories")
            categories.sortedBy { c -> c.rank }
            LOGGER.info("User $userId has sorted categories $categories")

            var newRank = 1L
            val newCategories = categories.stream().map { c -> c.copy(rank = newRank++) }
            LOGGER.info("User $userId will now have categories as $newCategories")

            for (category in newCategories) {
                joinUserAndCategoryAccessor.modifyRank(userId, category.id, category.rank)
            }
            LOGGER.info("User $userId done")
        }

        LOGGER.info("Done!")
    }

    /**
     * 1. Will add rank column if needed
     * 2. Will reset rank from 1 to X keeping current rank order
     *   --> Allow to init rank to default value
     *   --> If rank is too high because too many gifts have been added and deleted, could fix that.
     */
    private fun addRankToGift() {
        try {
            conn.executeQuery("SELECT rank FROM gifts")
            LOGGER.info("Column rank exists, will adapt rank values")
        } catch (e: Exception) {
            LOGGER.info("Column rank does not exist, add it")
            conn.executeUpdate("ALTER TABLE gifts ADD COLUMN 'rank' INTEGER NOT NULL DEFAULT 1")
        }

        val userIds = HashSet<Long>()
        val rs = conn.executeQuery("SELECT userId FROM gifts")
        while (rs.next()) {
            userIds.add(rs.getLong("userId"))
        }

        LOGGER.info("Will update gifts of users $userIds")

        val giftAccessor = GiftAccessor(conn)
        for (userId in userIds) {
            val allGifts = giftAccessor.getUserGifts(userId)
            val categoryIds = allGifts.map { g -> g.categoryId }.toSet()
            LOGGER.info("User $userId has gift on categories $categoryIds")
            for (category in categoryIds) {
                val gifts = allGifts.filter { g -> g.categoryId == category }
                LOGGER.info("\tCategory $category has gifts ${gifts.map { g -> "(${g.id}, ${g.rank})" }}")
                gifts.sortedBy { g -> g.rank }
                LOGGER.info("\tCategory $category has sorted gifts ${gifts.map { g -> "(${g.id}, ${g.rank})" }}")

                var newRank = 1L
                val newGifts = gifts.map { g -> g.copy(rank = newRank++) }
                LOGGER.info("\tCategory $category will now have gifts ${newGifts.map { g -> "(${g.id}, ${g.rank})" }}")

                for (gift in newGifts) {
                    giftAccessor.updateRank(gift.id, gift.rank)
                }
                LOGGER.info("\tCategory $category done\n")
            }
        }

        LOGGER.info("Done!")
    }

    /**
     * 1. Will add salt column if needed
     * 2. Get all current users using old table definition
     * 3. Drop table
     * 4. Create users with encoded password and salt
     * 5. reset user ids
     */
    private fun addSaltToUser() {
        try {
            conn.executeQuery("SELECT salt FROM users")
            LOGGER.info("Column salt exists, stop")
            return
        } catch (e: Exception) {
            LOGGER.info("Column salt does not exist, run maintenance")
        }

        //Get users
        val users = HashSet<UserBeforeMigration>()
        val rs = conn.executeQuery("SELECT id, name, password, picture FROM users")
        while (rs.next()) {
            users.add(
                UserBeforeMigration(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("password"),
                    rs.getString("picture")
                )
            )
        }

        //Disable foreign keys
        conn.execute("PRAGMA foreign_keys = OFF")
        //Backup table
        conn.execute("ALTER TABLE users RENAME TO users_bck")

        //create new table
        val usersAccessor = UsersAccessor(conn)
        usersAccessor.createIfNotExists()

        //TODO: copy of PasswordManager but adding dependency will add cyclic --> needed dedicated migration jar?
        val secureRandom = SecureRandom()
        val insert = "INSERT INTO users (id,name,password,salt,picture) VALUES (?,?, ?, ?, ?)"
        for (user in users) {
            val salt = ByteArray(16)
            secureRandom.nextBytes(salt)
            val encodedPassword = hash(user.password, salt)

            conn.safeExecute(
                insert, {
                    with(it) {
                        setLong(1, user.id)
                        setString(2, user.name)
                        setBinaryStream(3, ByteArrayInputStream(encodedPassword), encodedPassword.size)
                        setBinaryStream(4, ByteArrayInputStream(salt), salt.size)
                        setString(5, user.picture)
                        val rowCount = executeUpdate()
                        if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                    }
                }, "Insert user ${user.name} generated an error"
            )
        }

        //Clean DB
        conn.execute("DROP TABLE users_bck")
        conn.execute("PRAGMA foreign_keys = ON")
    }

    private fun hash(password: String, salt: ByteArray): ByteArray {
        val passwordChar = password.toCharArray()
        val spec = PBEKeySpec(passwordChar, salt, 10000, 256)
        Arrays.fill(passwordChar, Char.MIN_VALUE)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return skf.generateSecret(spec).encoded
    }

    private fun deleteEvents() {
        try {
            conn.executeQuery("DROP TABLE events")
            LOGGER.info("Table 'events' deleted")
            return
        } catch (e: Exception) {
            LOGGER.info("Table 'events' does not exist", e)
        }

        try {
            conn.executeQuery("DROP TABLE participants")
            LOGGER.info("Table 'participants' deleted")
            return
        } catch (e: Exception) {
            LOGGER.info("Table 'participants' does not exist", e)
        }
    }

    private fun commonCategories() {
        updateCategories()
        updateGifts()
    }

    private fun updateCategories() {
        try {
            conn.executeQuery("SELECT userId FROM categories")
            LOGGER.info("Column userId exists, run maintenance")
        } catch (e: Exception) {
            LOGGER.info("Column userId does not exist, stop")
            return
        }

        //Get categories
        val categories = HashSet<CategoryBeforeMigration>()
        val rs = conn.executeQuery("SELECT id, userId, name, rank FROM categories")
        while (rs.next()) {
            categories.add(
                CategoryBeforeMigration(
                    rs.getLong("id"),
                    rs.getLong("userId"),
                    rs.getString("name"),
                    rs.getLong("rank")
                )
            )
        }

        //Clean DB
        //conn.execute("DROP TABLE categories") --> Not working as table locked
        conn.execute("ALTER TABLE categories RENAME TO c_to_delete")

        //create new table
        val categoryAccessor = CategoryAccessor(conn)
        categoryAccessor.createIfNotExists()

        val joinUserAndCategoryAccessor = JoinUserAndCategoryAccessor(conn)
        joinUserAndCategoryAccessor.createIfNotExists()

        val insert = "INSERT INTO categories (id,name) VALUES (?,?)"
        val insertJoin = "INSERT into joinUserAndCategory(userId,categoryId,rank) VALUES (?,?,?)"
        for (category in categories) {
            conn.safeExecute(insert, {
                with(it) {
                    setLong(1, category.id)
                    setString(2, category.name)
                    val rowCount = executeUpdate()
                    if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                }
            }, "Insert category ${category.name} generated an error")
            conn.safeExecute(insertJoin, {
                with(it) {
                    setLong(1, category.userId)
                    setLong(2, category.id)
                    setLong(3, category.rank)
                    val rowCount = executeUpdate()
                    if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                }
            }, "Insert join ${category.name} generated an error")
        }
    }

    private fun updateGifts() {
        try {
            conn.executeQuery("SELECT userId FROM gifts")
            LOGGER.info("Column userId exists, run maintenance")
        } catch (e: Exception) {
            LOGGER.info("Column userId does not exist, stop")
            return
        }

        //Get gifts
        val gifts = HashSet<GiftBeforeMigration>()
        val rs =
            conn.executeQuery("SELECT id,userId,name,description,price,whereToBuy,categoryId,picture,secret,rank FROM gifts")
        while (rs.next()) {
            gifts.add(
                GiftBeforeMigration(
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
            )
        }

        //Clean DB
        //conn.execute("DROP TABLE gifts") --> Not working as table locked
        conn.execute("ALTER TABLE gifts RENAME TO g_to_delete")

        //create new table
        GiftAccessor(conn).createIfNotExists()

        val insert = "INSERT INTO gifts (id,name,description,price,whereToBuy,categoryId,picture,secret,rank) " +
                "VALUES (?,?,?,?,?,?,?,?,?)"
        for (gift in gifts) {
            conn.safeExecute(
                insert, {
                    with(it) {
                        setLong(1, gift.id)
                        setString(2, gift.name)
                        setString(3, gift.description)
                        setString(4, gift.price)
                        setString(5, gift.whereToBuy)
                        setLong(6, gift.categoryId)
                        setString(7, gift.picture)
                        setBoolean(8, gift.secret)
                        setLong(9, gift.rank)
                        val rowCount = executeUpdate()
                        if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                    }
                }, "Insert gift ${gift.name} generated an error"
            )
        }
    }

    private fun addHeartToGifts() {
        try {
            conn.executeQuery("SELECT heart FROM gifts")
            LOGGER.info("Column heart exists, skip")
        } catch (e: Exception) {
            LOGGER.info("Column heart does not exist, add it")
            conn.executeUpdate("ALTER TABLE gifts ADD COLUMN 'heart' INTEGER NOT NULL DEFAULT 0")
        }

        try {
            conn.executeQuery("SELECT interested FROM friendActionOnGift")
            LOGGER.info("Column interested exists, run maintenance")
        } catch (e: Exception) {
            LOGGER.info("Column interested does not exist, skip")
        }

        //Get friend actions
        val faogs = HashSet<FriendActionOnGiftBeforeMigration>()
        val rs = conn.executeQuery("SELECT id,giftId,userId,interested,buy FROM friendActionOnGift")
        while (rs.next()) {
            faogs.add(
                FriendActionOnGiftBeforeMigration(
                    rs.getLong("id"),
                    rs.getLong("giftId"),
                    rs.getLong("userId"),
                    rs.getBoolean("interested"),
                    rs.getString("buy")
                )
            )
        }
        faogs.removeIf { faog -> faog.buy == "NONE" }
        conn.execute("ALTER TABLE friendActionOnGift RENAME TO faog_to_delete")

        FriendActionOnGiftAccessor(conn).createIfNotExists()
        val insert = "INSERT INTO friendActionOnGift (id,giftId,userId) VALUES (?,?,?)"
        for (faog in faogs) {
            conn.safeExecute(
                insert, {
                    with(it) {
                        setLong(1, faog.id)
                        setLong(2, faog.giftId)
                        setLong(3, faog.userId)
                        val rowCount = executeUpdate()
                        if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                    }
                }, "Insert friendActionOnGift ${faog.giftId} ${faog.userId} generated an error"
            )
        }

        // to delete gifts --> remove friendAction
        val tdgs = conn.safeExecute("SELECT * FROM toDeleteGifts", {
            val toDeleteGifts = arrayListOf<DbToDeleteGifts>()
            with(it) {
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
                            res.getLong("friendId")
                        )
                    )
                }
                return@with toDeleteGifts
            }
        }, "toDeleteGifts generated an error")

        conn.execute("ALTER TABLE toDeleteGifts RENAME TO tdg_to_delete")
        ToDeleteGiftsAccessor(conn).createIfNotExists()
        val insert_tdg = "INSERT INTO toDeleteGifts (giftId,giftUserId,name,description,price,whereToBuy,picture,giftUserStatus,friendId) VALUES (?,?,?,?,?,?,?,?,?)"
        for (tdg in tdgs) {
            conn.safeExecute(
                insert_tdg, {
                    with(it) {
                        setLong(1, tdg.giftId)
                        setLong(2, tdg.giftUserId)
                        setString(3, tdg.name)
                        setString(4, tdg.description)
                        setString(5, tdg.price)
                        setString(6, tdg.whereToBuy)
                        setString(7, tdg.picture)
                        setString(8, tdg.giftUserStatus.name)
                        setLong(9, tdg.friendId)
                        val rowCount = executeUpdate()
                        if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                    }
                }, "Insert toDeleteGifts generated an error"
            )
        }
    }

    private fun addDateOfBirth() {
        try {
            conn.executeQuery("SELECT dateOfBirth FROM users")
            LOGGER.info("Column dateOfBirth exists, skip")
        } catch (e: Exception) {
            LOGGER.info("Column dateOfBirth does not exist, add it")
            conn.executeUpdate("ALTER TABLE users ADD COLUMN 'dateOfBirth' INTEGER")
        }
    }
}