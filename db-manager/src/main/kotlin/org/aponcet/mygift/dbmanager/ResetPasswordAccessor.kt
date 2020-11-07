package org.aponcet.mygift.dbmanager

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

data class DbResetPassword(val userId: Long, val uuid: String, val expiry: LocalDateTime)
data class DbResetPasswordException(override val message: String) : Exception(message)

class ResetPasswordAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        val LIFETIME: Duration = Duration.of(1, ChronoUnit.HOURS)
        val ZONE_OFFSET: ZoneOffset = ZoneOffset.UTC

        const val INSERT = "INSERT INTO reset_password (userId,uuid,expiry) VALUES (?, ?, ?)"
        const val SELECT_BY_USER_ID = "SELECT * FROM reset_password WHERE userId=?"
        const val SELECT_BY_UUID = "SELECT * FROM reset_password WHERE uuid=?"
        const val SELECT_BY_USER_ID_AND_UUID = "SELECT * FROM reset_password WHERE userId=? AND uuid=?"
        const val DELETE = "DELETE FROM reset_password WHERE userId=? AND uuid=?"
    }

    override fun getTableName(): String {
        return "reset_password"
    }

    override fun createIfNotExists() {
        conn.execute("CREATE TABLE IF NOT EXISTS ${getTableName()} (" +
                "userId  INTEGER NOT NULL, " +
                "uuid    TEXT NOT NULL, " +
                "expiry  INTEGER NOT NULL," +
                "FOREIGN KEY(userId) REFERENCES users(id))")
    }

    fun addEntry(userId: Long): DbResetPassword {
        cleanExpired(userId)
        if (getEntries(userId).isNotEmpty()) {
            throw DbResetPasswordException("There is already password reset on-going for this user id.")
        }

        val uuid = UUID.randomUUID()
        val expiry = LocalDateTime.now().plus(LIFETIME).toEpochSecond(ZONE_OFFSET)
        conn.safeExecute(
            INSERT, {
                with(it) {
                    setLong(1, userId)
                    setString(2, uuid.toString())
                    setLong(3, expiry)
                    val rowCount = executeUpdate()
                    if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                }
            },
            errorMessage(INSERT, userId.toString(), uuid.toString(), expiry.toString()))

        return DbResetPassword(userId, uuid.toString(), LocalDateTime.ofEpochSecond(expiry, 0, ZONE_OFFSET))
    }

    fun getEntry(userId: Long, uuid: String): DbResetPassword? {
        return conn.safeExecute(SELECT_BY_USER_ID_AND_UUID, {
            with(it) {
                setLong(1, userId)
                setString(2, uuid)
                val res = executeQuery()
                return@with if (res.next()) {
                    val expiry = LocalDateTime.ofEpochSecond(res.getLong("expiry"), 0, ZONE_OFFSET)
                    DbResetPassword(
                        res.getLong("userId"),
                        res.getString("uuid"),
                        expiry
                    )
                } else {
                    null
                }
            }
        }, errorMessage(SELECT_BY_USER_ID_AND_UUID, userId.toString(), uuid))
    }

    fun getEntry(uuid: String): DbResetPassword? {
        return conn.safeExecute(SELECT_BY_UUID, {
            with(it) {
                setString(1, uuid)
                val res = executeQuery()
                return@with if (res.next()) {
                    val expiry = LocalDateTime.ofEpochSecond(res.getLong("expiry"), 0, ZONE_OFFSET)
                    DbResetPassword(
                        res.getLong("userId"),
                        res.getString("uuid"),
                        expiry
                    )
                } else {
                    null
                }
            }
        }, errorMessage(SELECT_BY_UUID, uuid))
    }

    internal fun getEntries(userId: Long): List<DbResetPassword> {
        val resetPasswords = arrayListOf<DbResetPassword>()
        return conn.safeExecute(SELECT_BY_USER_ID, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                while (res.next()) {
                    val expiry = LocalDateTime.ofEpochSecond(res.getLong("expiry"), 0, ZONE_OFFSET)
                    resetPasswords.add(
                        DbResetPassword(
                            res.getLong("userId"),
                            res.getString("uuid"),
                            expiry
                        )
                    )
                }
                return@with resetPasswords
            }
        }, errorMessage(SELECT_BY_USER_ID, userId.toString()))
    }

    private fun cleanExpired(userId: Long) {
        val now = LocalDateTime.now()
        val entries = getEntries(userId)
        for (entry in entries) {
            if (entry.expiry.isBefore(now)) {
                delete(entry.userId, entry.uuid)
            }
        }
    }

    fun delete(userId: Long, uuid: String) {
        conn.safeExecute(DELETE, {
            with(it) {
                setLong(1, userId)
                setString(2, uuid)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(DELETE, userId.toString(), uuid))
    }
}