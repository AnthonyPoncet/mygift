package org.aponcet.mygift.dbmanager.maintenance

import org.aponcet.mygift.dbmanager.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class CleanDataNotUsed(dbPath: String, uploadPath: String) {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(CleanDataNotUsed::class.java)
    }

    private val conn = DbConnection("sqlite", dbPath)
    private val uploadsFile = File(uploadPath)

    enum class DATA { PICTURES, USER }

    fun execute(step: DATA) {
        when (step) {
            DATA.PICTURES -> cleanPictures()
            DATA.USER -> cleanUser()
        }
    }

    private fun cleanPictures() {
        /** Get pictures from DB **/
        val pictures = conn.safeExecute("SELECT picture FROM users", {
            with(it) {
                val res = executeQuery()
                val userPictures = HashSet<String>()
                while (res.next()) {
                    val element = res.getString("picture")
                    if (element.isNotBlank() && element.isNotEmpty()) userPictures.add(element)
                }
                return@with userPictures
            }
        }, "Execution of 'SELECT picture FROM users' throw an exception")

        pictures.addAll(conn.safeExecute("SELECT picture FROM gifts", {
            with(it) {
                val res = executeQuery()
                val giftPictures = HashSet<String>()
                while (res.next()) {
                    val element = res.getString("picture")
                    if (element != null && element.isNotBlank() && element.isNotEmpty()) giftPictures.add(element)
                }
                return@with giftPictures
            }
        }, "Execution of 'SELECT picture FROM gifts' throw an exception"))

        pictures.addAll(conn.safeExecute("SELECT picture FROM toDeleteGifts", {
            with(it) {
                val res = executeQuery()
                val giftPictures = HashSet<String>()
                while (res.next()) {
                    val element = res.getString("picture")
                    if (element != null && element.isNotBlank() && element.isNotEmpty()) giftPictures.add(element)
                }
                return@with giftPictures
            }
        }, "Execution of 'SELECT picture FROM toDeleteGifts' throw an exception"))

        LOGGER.info("Pictures in DB: ${pictures.size} -> $pictures")

        val filesToKeep = ArrayList<String>()
        val filesToRemove = ArrayList<File>()
        uploadsFile.walk().forEach { file ->
            if (!file.isFile) return@forEach

            if (pictures.find { file.name == it } == null) {
                filesToRemove.add(file.absoluteFile)
            } else {
                filesToKeep.add(file.name)
            }
        }

        LOGGER.info("Pictures in folder to keep/remove ${filesToKeep.size}/${filesToRemove.size}")
        LOGGER.info("Pictures in folder not in DB: $filesToRemove")

        val notDeleted = ArrayList<File>()
        var deleted = 0
        filesToRemove.forEach {
            if (it.delete()) deleted++ else notDeleted.add(it)
        }
        LOGGER.info("Report deleted/numberToDelete $deleted/${filesToRemove.size}")
        LOGGER.info("Report not deleted $notDeleted")
    }

    private fun cleanUser() {
        LOGGER.info("User name to remove?")
        val name = readLine()
        if (name == null) {
            LOGGER.error("Please provide a name")
            return
        }
        LOGGER.info("Will remove user '$name'")

        val usersAccessor = UsersAccessor(conn)
        val user = usersAccessor.getUser(name)
        if (user == null) {
            LOGGER.error("User '$name' does not exist")
            return
        }

        /** Remove this user from all friend **/
        val friendRequests = conn.safeExecute(
            "SELECT id FROM friendRequest WHERE userOne=? OR userTwo=?",
            {
                with(it) {
                    setLong(1, user.id)
                    setLong(2, user.id)
                    val res = executeQuery()
                    val requests = HashSet<Long>()
                    while (res.next()) {
                        requests.add(res.getLong("id"))
                    }
                    return@with requests
                }
            },
            "Execution of 'SELECT id FROM friendRequest WHERE userOne=${user.id} OR userTwo=${user.id}' throw an exception"
        )

        if (friendRequests.isEmpty()) {
            LOGGER.info("No friend requests to remove")
        } else {
            LOGGER.info("Will remove ${friendRequests.size} friend requests")
            val friendRequestAccessor = FriendRequestAccessor(conn)
            friendRequests.forEach { friendRequestAccessor.deleteFriendRequest(it) }
        }

        /** Remove potential reset_password **/
        val resetPasswordAccessor = ResetPasswordAccessor(conn)
        val entries = resetPasswordAccessor.getEntries(user.id)

        if (entries.isEmpty()) {
            LOGGER.info("No reset_password to remove")
        } else {
            LOGGER.info("Will remove ${entries.size} reset_password")
            entries.forEach { resetPasswordAccessor.delete(it.userId, it.uuid) }
        }

        /** Remove friend action **/
        val friendActionOnGiftAccessor = FriendActionOnGiftAccessor(conn)
        val friendActionOnGiftsUserHasActionOn =
            friendActionOnGiftAccessor.getFriendActionOnGiftsUserHasActionOn(user.id)
        if (friendActionOnGiftsUserHasActionOn.isEmpty()) {
            LOGGER.info("User has no action on any gift")
        } else {
            LOGGER.info("Will remove ${friendActionOnGiftsUserHasActionOn.size} action on gift")
            friendActionOnGiftsUserHasActionOn.forEach { friendActionOnGiftAccessor.delete(it.giftId, it.userId) }
        }

        val giftAccessor = GiftAccessor(conn)
        val userGifts = giftAccessor.getUserGifts(user.id)
        if (userGifts.isEmpty()) {
            LOGGER.info("User has no gift so nobody can have action on")
        } else {
            val actions =
                userGifts.map { friendActionOnGiftAccessor.getFriendActionOnGift(it.id) }.flatMap { it.toList() }
            if (actions.isEmpty()) {
                LOGGER.info("No action from friend on gift to remove")
            } else {
                LOGGER.info("Will remove ${actions.size} actions from friend on gift")
                actions.forEach { friendActionOnGiftAccessor.delete(it.giftId, it.userId) }
            }
        }

        /** Remove gift **/
        if (userGifts.isEmpty()) {
            LOGGER.info("No gift to remove")
        } else {
            LOGGER.info("Will remove ${userGifts.size} gifts")
            userGifts.forEach { giftAccessor.removeGift(it.id, Status.NOT_WANTED) }
        }

        /** Remove categories **/
        val categoryAccessor = CategoryAccessor(conn)
        val userCategories = categoryAccessor.getUserCategories(user.id)

        if (userCategories.isEmpty()) {
            LOGGER.info("No categories to remove")
        } else {
            LOGGER.info("Will remove ${userCategories.size} categories")
            userCategories.forEach { categoryAccessor.removeCategory(it.id) }
        }

        /** Remove to delete gifts **/
        val toDeleteGiftsAccessor = ToDeleteGiftsAccessor(conn)
        val deletedGiftsWhereUserHasActionOn = toDeleteGiftsAccessor.getDeletedGiftsWhereUserHasActionOn(user.id)
        if (deletedGiftsWhereUserHasActionOn.isEmpty()) {
            LOGGER.info("User has no buy gift that are pending a delete")
        } else {
            LOGGER.info("User has ${deletedGiftsWhereUserHasActionOn.size} buy gift that are pending a delete")
            deletedGiftsWhereUserHasActionOn.forEach { toDeleteGiftsAccessor.deleteDeletedGift(it.giftId, it.friendId) }
        }

        val ownToDeletedGifts = conn.safeExecute("SELECT giftId,friendId FROM toDeleteGifts WHERE giftUserId=?", {
            with(it) {
                setLong(1, user.id)
                val res = executeQuery()
                val ownToDeletedGifts = HashSet<Pair<Long, Long>>()
                while (res.next()) {
                    ownToDeletedGifts.add(Pair(res.getLong(1), res.getLong(2)))
                }
                return@with ownToDeletedGifts
            }
        }, "Execution of 'SELECT giftId,friendId FROM toDeleteGifts WHERE giftUserId=${user.id}' throw an exception")

        if (ownToDeletedGifts.isEmpty()) {
            LOGGER.info("User has no gift that have been bought that are pending a delete")
        } else {
            LOGGER.info("User has ${ownToDeletedGifts.size} gift that have been bought that are pending a delete")
            ownToDeletedGifts.forEach { toDeleteGiftsAccessor.deleteDeletedGift(it.first, it.second) }
        }

        /** Remove user **/
        LOGGER.info("Remove user")
        conn.safeExecute("DELETE FROM users WHERE id=?", {
            with(it) {
                setLong(1, user.id)
                return@with executeUpdate()
            }
        }, "Execution of 'DELETE FROM users WHERE id=${user.id}' throw an exception")
    }
}