package org.aponcet.mygift

import org.aponcet.authserver.PasswordManagerException
import org.aponcet.mygift.dbmanager.DatabaseManager
import org.aponcet.mygift.dbmanager.NewCategory
import org.aponcet.mygift.dbmanager.NewGift
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class DbInitializerForTest(databaseManager: DatabaseManager) {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(DbInitializerForTest::class.java)
    }

    init {
        //Clean db
        databaseManager.cleanTables()

        //Create User
        val aze = databaseManager.addUser(
            "aze",
            hash("aze", "salt1".toByteArray()),
            "salt1".toByteArray(),
            "black_cat.png"
        ).id
        val azeDCat = databaseManager.getUserCategories(aze)[0].id
        LOGGER.info("username: aze, pwd: aze ==> id: $aze - Default category id: $azeDCat")
        val eza =
            databaseManager.addUser("eza", hash("eza", "salt2".toByteArray()), "salt2".toByteArray(), "red_cat.png").id
        databaseManager.addCategory(NewCategory("Second catégorie"), listOf(eza))
        val ezaCats = databaseManager.getUserCategories(eza)
        val ezaDCat = ezaCats[0].id
        val ezaSCat = ezaCats[1].id
        LOGGER.info("username: eza, pwd: eza ==> id: $eza - Default category id: $eza, \"Second catégorie\" id: $ezaSCat")
        val other =
            databaseManager.addUser("other", hash("other", "salt3".toByteArray()), "salt3".toByteArray(), null).id
        LOGGER.info("usernamme: other, pwd; other ==> id: $other")

        //Fill gift
        databaseManager.addGift(
            aze,
            NewGift(
                "One",
                "First description with spécial char and ' ",
                "10€",
                "http://mysite.com",
                azeDCat,
                null
            ), false
        )
        databaseManager.addGift(
            aze,
            NewGift("No desc", null, "20$", "a place", azeDCat, null), false
        )
        databaseManager.addGift(
            aze,
            NewGift(
                "No price",
                "There is no price",
                null,
                "http://mysite.com or ici, 75000 Paris",
                azeDCat,
                null
            ), false
        )
        databaseManager.addGift(
            aze,
            NewGift(
                "No where to buy",
                "There is no where to buy",
                "30 - 40£",
                null,
                azeDCat,
                null
            ), false
        )
        databaseManager.addGift(
            aze,
            NewGift("Only mandatory", null, null, null, azeDCat, "pc.png"), true
        )
        LOGGER.info("5 gifts added to aze")

        databaseManager.addGift(
            eza,
            NewGift("A first one", null, null, null, ezaDCat, "pc.png"), false
        )
        databaseManager.addGift(
            eza,
            NewGift("A second one", null, null, null, ezaDCat, null), false
        )
        databaseManager.addGift(
            eza,
            NewGift(
                "One in another cat",
                null,
                null,
                null,
                ezaSCat,
                "book.png"
            ), false
        )
        LOGGER.info("3 gift added to eza")

        //They are friend
        databaseManager.createFriendRequest(aze, eza)
        var fr = databaseManager.getReceivedFriendRequests(eza)[0].id
        databaseManager.acceptFriendRequest(eza, fr)
        LOGGER.info("aze and eza are now friend")
        databaseManager.createFriendRequest(other, aze)
        fr = databaseManager.getReceivedFriendRequests(aze)[0].id
        databaseManager.acceptFriendRequest(aze, fr)
        LOGGER.info("other and aze are now friend")
    }

    private fun hash(password: String, salt: ByteArray): ByteArray {
        val passwordChar = password.toCharArray()
        val spec = PBEKeySpec(passwordChar, salt, 10000, 256)
        Arrays.fill(passwordChar, Char.MIN_VALUE)
        try {
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            return skf.generateSecret(spec).encoded
        } catch (e: NoSuchAlgorithmException) {
            throw PasswordManagerException("No such algorithm", e)
        } catch (e: InvalidKeySpecException) {
            throw PasswordManagerException("Invalid Key spec algorithm", e)
        } finally {
            spec.clearPassword()
        }
    }
}