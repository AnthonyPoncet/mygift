package org.aponcet.mygift.dbmanager

import java.sql.ResultSet

class ParticipantsAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO participants(eventId,userId,status) VALUES(?,?,?)"
        const val UPDATE = "UPDATE participants SET status = ? WHERE eventId = ? AND userId = ?"
        const val SELECT_SOME_BY_USER = "SELECT * FROM participants WHERE userId=?"
        const val SELECT_SOME_BY_EVENT = "SELECT * FROM participants WHERE eventId=?"
        const val SELECT_BY_EVENT_AND_USER = "SELECT * FROM participants WHERE eventId = ? AND userId = ?"
        const val DELETE = "DELETE FROM participants WHERE eventId = ? AND userId = ?"
    }

    override fun getTableName(): String {
        return "participants"
    }

    override fun createIfNotExists() {
        conn.execute("CREATE TABLE IF NOT EXISTS participants (" +
            "id         INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "eventId    INTEGER NOT NULL, " +
            "userId     INTEGER NOT NULL, " +
            "status     TEXT NOT NULL, " +
            "FOREIGN KEY(eventId) REFERENCES events(id), " +
            "FOREIGN KEY(userId) REFERENCES users(id))")
    }

    fun insert(eventId: Long, participantId: Long) {
        conn.safeExecute(INSERT, {
            with(it) {
                setLong(1, eventId)
                setLong(2, participantId)
                setString(3, RequestStatus.PENDING.name)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, eventId.toString(), participantId.toString(), RequestStatus.PENDING.name))
    }

    fun update(eventId: Long, participantId: Long, requestStatus: RequestStatus) {
        conn.safeExecute(UPDATE, {
            with(it) {
                setString(1, requestStatus.name)
                setLong(2, eventId)
                setLong(3, participantId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE, requestStatus.name, eventId.toString(), participantId.toString()))
    }

    fun delete(eventId: Long, participantId: Long) {
        conn.safeExecute(DELETE, {
            with(it) {
                setLong(1, eventId)
                setLong(2, participantId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(DELETE, eventId.toString(), participantId.toString()))
    }

    private fun convertOneRes(res: ResultSet) : DbParticipant {
        return DbParticipant(
            res.getLong("id"),
            res.getLong("eventId"),
            res.getLong("userId"),
            RequestStatus.valueOf(res.getString("status"))
        )
    }

    fun getEventsAsParticipants(userId: Long) : List<DbParticipant>{
        return conn.safeExecute(SELECT_SOME_BY_USER, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                val participants = arrayListOf<DbParticipant>()
                while (res.next()) {
                    participants.add(convertOneRes(res))
                }
                return@with participants
            }
        }, errorMessage(SELECT_SOME_BY_USER, userId.toString()))
    }

    fun getParticipants(eventId: Long) : List<DbParticipant>{
        return conn.safeExecute(SELECT_SOME_BY_EVENT, {
            with(it) {
                setLong(1, eventId)
                val res = executeQuery()
                val participants = arrayListOf<DbParticipant>()
                while (res.next()) {
                    participants.add(convertOneRes(res))
                }
                return@with participants
            }
        }, errorMessage(SELECT_SOME_BY_EVENT, eventId.toString()))
    }

    fun userInvitedToEvent(eventId: Long, userId: Long): Boolean {
        return conn.safeExecute(SELECT_BY_EVENT_AND_USER, {
            with(it) {
                setLong(1, eventId)
                setLong(2, userId)
                return@with executeQuery().next()
            }
        }, errorMessage(SELECT_BY_EVENT_AND_USER, eventId.toString(), userId.toString()))
    }
}