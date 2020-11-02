package org.aponcet.mygift.dbmanager.maintenance

import org.aponcet.mygift.dbmanager.*
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.collections.HashSet

data class UserBeforeMigration(val id: Long, val name: String, val password: String, val picture: String)

/** Adapt table used only ad-hoc when needed **/
class AdaptTable(dbPath: String) {
    private val conn = DbConnection("sqlite", dbPath)

    enum class STEP { ADD_RANK_TO_CATEGORY, ADD_RANK_TO_GIFT, ADD_SALT_TO_USER }

    fun execute(step: STEP) {
        when (step) {
            STEP.ADD_RANK_TO_CATEGORY -> addRankToCategory()
            STEP.ADD_RANK_TO_GIFT -> addRankToGift()
            STEP.ADD_SALT_TO_USER -> addSaltToUser()
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
            println("Column rank exists, will adapt rank values")
        } catch (e: Exception) {
            println("Column rank does not exist, add it")
            conn.executeUpdate("ALTER TABLE categories ADD COLUMN 'rank' INTEGER NOT NULL DEFAULT 1")
        }

        val userIds = HashSet<Long>()
        val rs = conn.executeQuery("SELECT userId FROM categories")
        while (rs.next()) {
            userIds.add(rs.getLong("userId"))
        }

        println("Will update category of users $userIds")

        val categoryAccessor = CategoryAccessor(conn)
        for (userId in userIds) {
            val categories = categoryAccessor.getUserCategories(userId)
            println("User $userId has categories $categories")
            categories.sortedBy { c -> c.rank }
            println("User $userId has sorted categories $categories")

            var newRank = 1L
            val newCategories = categories.stream().map { c -> c.copy(rank = newRank++) }
            println("User $userId will now have categories as $newCategories")

            for (category in newCategories) {
                categoryAccessor.modifyCategory(category.id, Category(category.name, category.rank))
            }
            println("User $userId done")
        }

        println("Done!")
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
            println("Column rank exists, will adapt rank values")
        } catch (e: Exception) {
            println("Column rank does not exist, add it")
            conn.executeUpdate("ALTER TABLE gifts ADD COLUMN 'rank' INTEGER NOT NULL DEFAULT 1")
        }

        val userIds = HashSet<Long>()
        val rs = conn.executeQuery("SELECT userId FROM gifts")
        while (rs.next()) {
            userIds.add(rs.getLong("userId"))
        }

        println("Will update gifts of users $userIds")

        val giftAccessor = GiftAccessor(conn)
        for (userId in userIds) {
            val allGifts = giftAccessor.getUserGifts(userId)
            val categoryIds = allGifts.map { g -> g.categoryId }.toSet()
            println("User $userId has gift on categories $categoryIds")
            for (category in categoryIds) {
                val gifts = allGifts.filter { g -> g.categoryId == category }
                println("\tCategory $category has gifts ${gifts.map { g -> "(${g.id}, ${g.rank})" }}")
                gifts.sortedBy { g -> g.rank }
                println("\tCategory $category has sorted gifts ${gifts.map { g -> "(${g.id}, ${g.rank})" }}")

                var newRank = 1L
                val newGifts = gifts.map { g -> g.copy(rank = newRank++) }
                println("\tCategory $category will now have gifts ${newGifts.map { g -> "(${g.id}, ${g.rank})" }}")

                for (gift in newGifts) {
                    giftAccessor.modifyGift(gift.id, Gift(gift.name, gift.description, gift.price,
                        gift.whereToBuy, gift.categoryId, gift.picture, gift.rank)
                    )
                }
                println("\tCategory $category done\n")
            }
        }

        println("Done!")
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
            println("Column salt exists, stop")
            return
        } catch (e: Exception) {
            println("Column salt does not exist, run maintenance")
        }

        //Get users
        val users = HashSet<UserBeforeMigration>()
        val rs = conn.executeQuery("SELECT id, name, password, picture FROM users")
        while (rs.next()) {
            users.add(UserBeforeMigration(rs.getLong("id"), rs.getString("name"), rs.getString("password"), rs.getString("picture")))
        }

        //Disable foreign keys
        conn.execute("PRAGMA foreign_keys=off")
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
                }, "Insert user ${user.name} generated an error")
        }

        //Clean DB
        conn.execute("DROP TABLE users_bck")
        conn.execute("PRAGMA foreign_keys=on")
    }

    private fun hash(password : String, salt: ByteArray): ByteArray {
        val passwordChar = password.toCharArray()
        val spec = PBEKeySpec(passwordChar, salt, 10000, 256)
        Arrays.fill(passwordChar, Char.MIN_VALUE)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return skf.generateSecret(spec).encoded
    }
}