package org.aponcet.mygift

//TODO: remove linked to db model?
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.aponcet.authserver.*
import org.aponcet.mygift.dbmanager.*
import org.aponcet.mygift.model.AuthServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.*
import java.time.Month
import java.util.stream.Collectors
import kotlin.collections.set

/** RETURN CLASSES **/
data class User(val token: String, val session: String, val name: String, val picture: String?, val dateOfBirth: Long?)
data class Friend(val name: String, val picture: String?, val dateOfBirth: Long?)
data class CleanGift(
    val id: Long,
    val name: String,
    val description: String?,
    val price: String?,
    val whereToBuy: String?,
    val categoryId: Long,
    val picture: String?,
    val heart: Boolean
)

data class CleanCategory(val id: Long, val name: String, val share: Set<String>)
data class CatAndGift(val category: CleanCategory, val gifts: List<CleanGift>)
data class FriendGift(
    val gift: CleanGift,
    val reservedBy: List<String>,
    val secret: Boolean
)

data class CatAndFriendGift(val category: CleanCategory, val gifts: List<FriendGift>)
data class FriendRequest(val id: Long, val otherUser: Friend)
data class PendingFriendRequest(val sent: List<FriendRequest>, val received: List<FriendRequest>)

data class BuyListByFriend(val friendName: String, val gifts: List<FriendGift>, val deletedGifts: List<DeletedGifts>)
data class DeletedGifts(
    val id: Long,
    val name: String,
    val description: String?,
    val price: String?,
    val whereToBuy: String?,
    val picture: String?,
    val status: Status
)

data class ResetPassword(val userId: Long, val uuid: String, val expiry: LocalDateTime)

enum class EventKind { BIRTHDAY, CHRISTMAS, MOTHER_DAY, FATHER_DAY }
data class Event(val kind: EventKind, val date: Long, val name: String?, val picture: String?, val birth: Long?)

/** INPUT CLASSES **/
data class UserModification(val name: String?, val picture: String?, val dateOfBirth: Long?)
data class RestGift(
    val name: String?,
    val description: String?,
    val price: String?,
    val whereToBuy: String?,
    val categoryId: Long?,
    val picture: String?,
)

data class RestCategory(val name: String?, val share: List<String>?)
data class RestCreateFriendRequest(val name: String?)
enum class RankAction { DOWN, UP }
enum class HeartAction { LIKE, UNLIKE }

/** Exceptions **/
class BadParamException(val error: String) : Exception("Bad parameter $error")
class ConnectionException(val error: String) : Exception("Unable to connect. Cause: $error")
class CreateUserException(val error: String) : Exception("Unable to create user. Cause: $error")
class UpdateUserException(val error: String) : Exception("Unable to update user. Cause: $error")

/**
 * No cleaning notion, take care of its validity
 */
class DummyUserCache(private val databaseManager: DatabaseManager) {
    private val cacheIds = HashMap<Long, String?>()

    fun queryName(userId: Long): String? {
        val name = cacheIds[userId]
        return if (name != null) name
        else {
            cacheIds[userId] = databaseManager.getUser(userId)?.name
            cacheIds[userId]
        }
    }
}


class UserManager(private val databaseManager: DatabaseManager, private val authServer: AuthServer) {

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(UserManager::class.java)
        val ZONE_ID: ZoneId = ZoneId.of("GMT")
    }

    suspend fun connect(userJson: UserJson): User {
        if (userJson.name == null) throw BadParamException("Username could not be null")
        if (userJson.password == null) throw BadParamException("Password could not be null")

        val client = HttpClient(Apache)
        try {
            val httpResponse = client.post {
                url("http://${authServer.host}:${authServer.port}/login")
                setBody(Gson().toJson(userJson))
            }
            if (httpResponse.status == HttpStatusCode.Unauthorized) {
                val error = Gson().fromJson(httpResponse.bodyAsText(), ErrorResponse::class.java).error
                LOGGER.error("Error while authenticate '${error}'")
                throw ConnectionException(error)
            }

            val tokenResponse = Gson().fromJson(httpResponse.bodyAsText(), TokenResponse::class.java)

            val name = userJson.name!!
            val user = databaseManager.getUser(name)!! //to get picture
            return User(tokenResponse.jwt, tokenResponse.session, user.name, user.picture, user.dateOfBirth)
        } catch (e: ResponseException) {
            val response = e.response

            if (response.status == HttpStatusCode.Unauthorized) {
                val error = Gson().fromJson(response.bodyAsText(), ErrorResponse::class.java).error
                LOGGER.error("Error while authenticate '${error}'", e)
                throw ConnectionException(error)
            }

            LOGGER.error("Error while creating user '${e.message}'", e)
            throw IllegalStateException(e)
        }
    }

    suspend fun addUser(userAndPictureJson: UserAndPictureJson): User {
        val client = HttpClient(Apache)
        try {
            client.put {
                url("http://${authServer.host}:${authServer.port}/create")
                setBody(Gson().toJson(userAndPictureJson))
            }

            return connect(UserJson(userAndPictureJson.name, userAndPictureJson.password, null))
        } catch (e: ResponseException) {
            val response = e.response

            if (response.status == HttpStatusCode.Unauthorized) {
                val error = Gson().fromJson(response.bodyAsText(), ErrorResponse::class.java).error
                LOGGER.error("Error while creating user '${error}'", e)
                throw ConnectionException(error)
            }
            if (response.status == HttpStatusCode.Conflict) {
                val readText = response.bodyAsText()
                val error = Gson().fromJson(readText, ErrorResponse::class.java).error
                LOGGER.error("Error while creating user '${error}'", e)
                throw CreateUserException(error)
            }

            LOGGER.error("Error while creating user '${e.message}'", e)
            throw IllegalStateException(e)
        }
    }

    fun modifyUser(userId: Long, userModification: UserModification): User {
        if (userModification.name == null) throw BadParamException("Name could not be null")
        if (userModification.name.isEmpty()) throw BadParamException("Name could not be empty")
        val user = databaseManager.getUser(userId) ?: throw BadParamException("User does not exists")
        if (user.name != userModification.name && databaseManager.getUser(userModification.name) != null) throw UpdateUserException("User with this name already exist")

        databaseManager.modifyUser(userId, userModification.name, userModification.picture, userModification.dateOfBirth)
        return User("", "", userModification.name, userModification.picture, userModification.dateOfBirth)
    }

    private fun toGift(g: DbGift): CleanGift {
        return CleanGift(
            g.id,
            g.name,
            g.description,
            g.price,
            g.whereToBuy,
            g.categoryId,
            g.picture,
            g.heart
        )
    }

    fun getUserGifts(userId: Long): List<CatAndGift> {
        val categories = databaseManager.getUserCategories(userId)
        val gifts = databaseManager.getUserGifts(userId)
        return categories.map { c ->
            CatAndGift(
                CleanCategory(
                    c.id,
                    c.name,
                    c.share.map { u -> databaseManager.getUser(u)!!.name }.toCollection(hashSetOf())
                ), gifts.filter { g -> g.categoryId == c.id }.map { g -> toGift(g) })
        }
    }

    //Not optimal at all!
    fun getFriendGifts(userId: Long, friendName: String): List<CatAndFriendGift> {
        val categories = databaseManager.getFriendCategories(userId, friendName)
        val gifts = databaseManager.getFriendGifts(userId, friendName)

        val dummyUserCache = DummyUserCache(databaseManager) //Cache only by call
        return categories.map { c ->
            CatAndFriendGift(
                CleanCategory(
                    c.id,
                    c.name,
                    hashSetOf()
                ), gifts.filter { g -> g.categoryId == c.id }.map { g ->
                    val actions = databaseManager.getFriendActionOnGift(g.id)
                    FriendGift(toGift(g),
                        actions.filter { dummyUserCache.queryName(it.userId) != null }.map {
                            dummyUserCache.queryName(it.userId)!!
                        },
                        g.secret
                    )
                })
        }
    }

    fun getBuyList(userId: Long): List<BuyListByFriend> {
        val friendActionOnGiftsUserHasActionOn = databaseManager.getFriendActionOnGiftsUserHasActionOn(userId)

        val dbGifts = friendActionOnGiftsUserHasActionOn.mapNotNull { g -> databaseManager.getGift(g.giftId) }

        val giftsAndUser = ArrayList<Pair<Long, DbGift>>()
        for (gift in dbGifts) {
            val usersFromCategory = databaseManager.getUsersFromCategory(gift.categoryId)
            usersFromCategory.forEach { giftsAndUser.add(Pair(it, gift)) }
        }

        val gifts = giftsAndUser.groupBy({ it.first }, { it.second })

        val dummyUserCache = DummyUserCache(databaseManager) //Cache only by call

        val deletedGiftsUserHasActionOn = databaseManager.getDeletedGiftsUserHasActionOn(userId)
        val deletedGiftByFriend = deletedGiftsUserHasActionOn.groupBy({ it.giftUserId },
            {
                DeletedGifts(
                    it.giftId,
                    it.name,
                    it.description,
                    it.price,
                    it.whereToBuy,
                    it.picture,
                    it.giftUserStatus
                )
            })

        val map = (gifts.keys + deletedGiftByFriend.keys).associateWith {
            Pair(
                if (gifts[it] == null) emptyList() else gifts[it]!!,
                if (deletedGiftByFriend[it] == null) emptyList() else deletedGiftByFriend[it]!!
            )
        }

        return map
            .filter { dummyUserCache.queryName(it.key) != null }
            .map { m ->
                BuyListByFriend(
                    dummyUserCache.queryName(m.key)!!,
                    m.value.first.map { g ->
                        val actions = databaseManager.getFriendActionOnGift(g.id)
                        FriendGift(toGift(g),
                            actions.filter { dummyUserCache.queryName(it.userId) != null }
                                .map { dummyUserCache.queryName(it.userId)!! },
                            g.secret
                        )
                    }.sortedBy { it.gift.id },
                    m.value.second.sortedBy { it.name })
            }.sortedBy { it.friendName }
    }

    fun deleteDeletedGift(giftId: Long, friendId: Long) {
        return databaseManager.deleteDeletedGift(giftId, friendId)
    }

    fun addGift(userId: Long, gift: RestGift) {
        databaseManager.addGift(userId, toNewGift(gift), false)
    }

    fun addSecretGift(userId: Long, friendName: String, gift: RestGift) {
        val initiatedRequest = getInitiatedRequest(userId, RequestStatus.ACCEPTED)
        val receivedRequest = getReceivedRequest(userId, RequestStatus.ACCEPTED)

        if (initiatedRequest.none { it.otherUser.name == friendName } && receivedRequest.none { it.otherUser.name == friendName }) {
            throw Exception("You are not friend with $friendName.")
        }

        val friendUserId = (databaseManager.getUser(friendName) ?: throw Exception("No user named $friendName.")).id
        databaseManager.addGift(friendUserId, toNewGift(gift), true)
    }

    private fun validateRestGift(restGift: RestGift) {
        if (restGift.name == null) {
            throw BadParamException("Gift JSON need to contain 'name' node")
        }
        if (restGift.categoryId == null) {
            throw BadParamException("Gift JSON need to contain 'categoryId' node")
        }
    }

    private fun toNewGift(restGift: RestGift): NewGift {
        validateRestGift(restGift)
        return NewGift(
            restGift.name!!,
            restGift.description,
            restGift.price,
            restGift.whereToBuy,
            restGift.categoryId!!,
            restGift.picture
        )
    }

    fun modifyGift(userId: Long, giftId: Long, gift: RestGift) {
        databaseManager.modifyGift(userId, giftId, toNewGift(gift))
    }

    fun removeGift(userId: Long, giftId: Long, status: Status) {
        databaseManager.removeGift(userId, giftId, status)
    }

    fun changeGiftRank(userId: Long, giftId: Long, rankAction: RankAction) {
        when (rankAction) {
            RankAction.DOWN -> databaseManager.rankDownGift(userId, giftId)
            RankAction.UP -> databaseManager.rankUpGift(userId, giftId)
        }
    }

    fun changeGiftHeart(userId: Long, giftId: Long, heartAction: HeartAction) {
        val heart = when (heartAction) {
            HeartAction.LIKE -> true
            HeartAction.UNLIKE -> false
        }

        databaseManager.updateHeart(userId, giftId, heart)
    }

    fun changeReserve(giftId: Long, userId: Long, reserve: Boolean) {
        databaseManager.changeReserve(giftId, userId, reserve)
    }

    fun addCategory(userId: Long, name: String, shareWith: List<String>) {
        val users = shareWith.map { databaseManager.getUser(it) }
        if (users.any { it == null }) throw BadParamException("At least one passed user does not exist")
        val userIds = users.map { it!!.id }
        databaseManager.addCategory(name, userIds.plus(userId))
    }

    fun modifyCategory(userId: Long, categoryId: Long, name: String) {
        databaseManager.modifyCategory(userId, categoryId, name)
    }

    fun removeCategory(userId: Long, categoryId: Long) {
        databaseManager.removeCategory(userId, categoryId)
    }

    fun changeCategoryRank(userId: Long, categoryId: Long, rankAction: RankAction) {
        when (rankAction) {
            RankAction.DOWN -> databaseManager.rankDownCategory(userId, categoryId)
            RankAction.UP -> databaseManager.rankUpCategory(userId, categoryId)
        }
    }

    fun createFriendRequest(userId: Long, otherUser: RestCreateFriendRequest) {
        if (otherUser.name == null) throw BadParamException("Username to send request to could not be null")

        val user = databaseManager.getUser(otherUser.name) ?: throw Exception("User ${otherUser.name} does not exist.")

        databaseManager.createFriendRequest(userId, user.id)
    }

    fun getFriends(userId: Long): List<FriendRequest> {
        val initiatedFriendRequests =
            databaseManager.getInitiatedFriendRequests(userId).filter { it.status == RequestStatus.ACCEPTED }
        val receivedFriendRequests =
            databaseManager.getReceivedFriendRequests(userId).filter { it.status == RequestStatus.ACCEPTED }

        val ini = initiatedFriendRequests.map {
            FriendRequest(
                it.id,
                toFriend(it.userTwo)
            )
        }
        val rec = receivedFriendRequests.map {
            FriendRequest(
                it.id,
                toFriend(it.userOne)
            )
        }

        val mut: MutableList<FriendRequest> = mutableListOf()
        mut.addAll(ini)
        mut.addAll(rec)
        mut.sortBy { it.otherUser.name }
        return mut
    }

    private fun getInitiatedRequest(userId: Long, status: RequestStatus): List<FriendRequest> {
        return databaseManager.getInitiatedFriendRequests(userId)
            .filter { it.status == status }
            .map { FriendRequest(it.id, toFriend(it.userTwo)) }
    }

    private fun getReceivedRequest(userId: Long, status: RequestStatus): List<FriendRequest> {
        return databaseManager.getReceivedFriendRequests(userId)
            .filter { it.status == status }
            .map { FriendRequest(it.id, toFriend(it.userOne)) }
    }

    fun getPendingFriendRequests(userId: Long): PendingFriendRequest {
        val initiated = getInitiatedRequest(userId, RequestStatus.PENDING).sortedBy { it.otherUser.name }
        val received = getReceivedRequest(userId, RequestStatus.PENDING).sortedBy { it.otherUser.name }

        return PendingFriendRequest(initiated, received)
    }

    fun getReceivedBlockedFriendRequests(userId: Long): List<FriendRequest> {
        return getReceivedRequest(userId, RequestStatus.REJECTED)
    }

    fun deleteFriendRequest(userId: Long, friendRequestId: Long) {
        databaseManager.deleteFriendRequest(userId, friendRequestId)
    }

    fun acceptFriendRequest(userId: Long, friendRequestId: Long) {
        databaseManager.acceptFriendRequest(userId, friendRequestId)
    }

    fun declineFriendRequest(userId: Long, friendRequestId: Long, blockUser: Boolean) {
        databaseManager.declineFriendRequest(userId, friendRequestId, blockUser)
    }

    private fun toFriend(userId: Long): Friend {
        val user = databaseManager.getUser(userId)!!
        return Friend(user.name, user.picture, user.dateOfBirth)
    }

    fun getEntry(uuid: String): ResetPassword {
        val entry = databaseManager.getEntry(uuid)
        return ResetPassword(entry.userId, entry.uuid, entry.expiry)
    }

    suspend fun modifyPassword(userAndResetPassword: UserAndResetPassword, uuid: String) {
        if (userAndResetPassword.name == null) throw BadParamException("Username could not be null")
        if (userAndResetPassword.password == null) throw BadParamException("Password could not be null")

        val entry = databaseManager.getEntry(uuid)
        val user = databaseManager.getUser(userAndResetPassword.name!!)
        if (entry.userId != (user?.id ?: -1L)) {
            databaseManager.deleteEntry(entry.userId, uuid)
            throw Exception("This uuid does not belong to you. Reset password uuid have been deleted.")
        }

        //send modify to authserver
        val client = HttpClient(Apache)
        try {
            client.post {
                url("http://${authServer.host}:${authServer.port}/update")
                setBody(Gson().toJson(userAndResetPassword))
            }
        } catch (e: ResponseException) {
            LOGGER.error("Error while updating user: $e")
            throw IllegalStateException(e)
        }

        databaseManager.deleteEntry(entry.userId, uuid)
    }

    fun deleteSession(session: String, userId: Long) {
        return databaseManager.deleteSession(session, userId)
    }

    fun getUsersOfSession(currentUserId: Long, session: String): List<Long> {
        return databaseManager.getUsersOfSession(currentUserId, session)
    }

    private fun getNext(now: ZonedDateTime, month: Int, day: Int): ZonedDateTime {
        val potential = ZonedDateTime.of(now.year, month, day, 0, 0, 0, 0, ZONE_ID)
        return if (potential.isBefore(now)) {
            potential.plusYears(1)
        } else {
            potential
        }
    }

    private fun getNextBirthday(now: ZonedDateTime, epochSecond: Long): ZonedDateTime {
        val dateOfBirth = Instant.ofEpochSecond(epochSecond).atZone(ZONE_ID)
        return getNext(now, dateOfBirth.monthValue, dateOfBirth.dayOfMonth)
    }

    //In France: Last Sunday of May (in theory except if Pentecote)
    private fun getNextMotherDay(now: ZonedDateTime): ZonedDateTime {
        var dayOfMonth = 31
        val dayOfWeek = ZonedDateTime.of(now.year, Month.MAY.value, dayOfMonth, 0, 0, 0, 0, ZONE_ID).dayOfWeek
        dayOfMonth -= (7 - (DayOfWeek.SUNDAY.ordinal - dayOfWeek.ordinal))
        return ZonedDateTime.of(now.year, Month.MAY.value, dayOfMonth, 0, 0, 0, 0, ZONE_ID)
    }

    //3rd Sunday of june
    private fun getNextFatherDay(now: ZonedDateTime): ZonedDateTime {
        var dayOfMonth = 1
        val dayOfWeek = ZonedDateTime.of(now.year, Month.JUNE.value, dayOfMonth, 0, 0, 0, 0, ZONE_ID).dayOfWeek
        dayOfMonth += DayOfWeek.SUNDAY.ordinal - dayOfWeek.ordinal
        return ZonedDateTime.of(now.year, Month.JUNE.value, dayOfMonth + 14, 0, 0, 0, 0, ZONE_ID)
    }

    fun getEvents(userId: Long): List<Event> {
        val now = Instant.now().atZone(ZONE_ID)
        val inSixMonth = now.plusMonths(6).plusDays(1)

        val friends = getFriends(userId)
        val comingBirthdays = friends
            .asSequence()
            .filter { it.otherUser.dateOfBirth != null }
            .map { (it.otherUser to getNextBirthday(now, it.otherUser.dateOfBirth!!)) }
            .filter { it.second.isBefore(inSixMonth)  }
            .map { Event(EventKind.BIRTHDAY, it.second.toEpochSecond()*1000, it.first.name, it.first.picture, it.first.dateOfBirth!!*1000) }
            .toMutableList()

        val motherDay = getNextMotherDay(now)
        if (!motherDay.isBefore(now) && motherDay.isBefore(inSixMonth)) {
            comingBirthdays.add(Event(EventKind.MOTHER_DAY, motherDay.toEpochSecond()*1000, null, null, null))
        }

        val fatherDay = getNextFatherDay(now)
        if (!fatherDay.isBefore(now) && fatherDay.isBefore(inSixMonth)) {
            comingBirthdays.add(Event(EventKind.FATHER_DAY, fatherDay.toEpochSecond()*1000, null, null, null))
        }

        val christmas = getNext(now, Month.DECEMBER.value, 24)
        if (!christmas.isBefore(now) && christmas.isBefore(inSixMonth)) {
            comingBirthdays.add(Event(EventKind.CHRISTMAS, christmas.toEpochSecond()*1000, null, null, null))
        }

        return comingBirthdays.sortedBy { it.date }
    }
}
