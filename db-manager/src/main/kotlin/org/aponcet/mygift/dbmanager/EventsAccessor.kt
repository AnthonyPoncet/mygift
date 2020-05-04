package org.aponcet.mygift.dbmanager

import java.sql.ResultSet
import java.time.LocalDate

class EventsAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO events(name,creatorId,description,endDate,target) VALUES (?, ?, ?, ?, ?)"
        const val SELECT_BY_ID = "SELECT * FROM events WHERE id=?"
        const val SELECT_BY_CREATOR = "SELECT * FROM events WHERE creatorId=?"
        const val SELECT_BY_NAME = "SELECT * FROM events WHERE name=?"
        const val DELETE = "DELETE FROM events WHERE id = ?"
    }

    override fun getTableName(): String {
        return "events"
    }

    override fun createIfNotExists() {
        conn.execute("CREATE TABLE IF NOT EXISTS events (" +
            "id             INTEGER PRIMARY KEY ${conn.autoIncrement}, " +
            "name           TEXT NOT NULL, " +
            "creatorId      INTEGER NOT NULL, " +
            "description    TEXT, " +
            "endDate        INTEGER NOT NULL, " +
            "target         INTEGER NOT NULL)")
    }

    fun insertEvent(name: String, creatorId: Long, description: String?, endDate: LocalDate, target: Long): Long {
        return conn.safeExecute(INSERT, {
            with(it) {
                setString(1, name)
                setLong(2, creatorId)
                setString(3, description ?: "")
                setLong(4, endDate.toEpochDay())
                setLong(5, target)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                if (generatedKeys.next()) {
                    return@with generatedKeys.getLong(1)
                } else {
                    throw Exception("executeUpdate, no key generated")
                }
            }
        }, errorMessage(INSERT, name, creatorId.toString(), description ?: "", endDate.toEpochDay().toString(), target.toString()))
    }

    fun deleteEvent(eventId: Long) {
        conn.safeExecute(DELETE, {
            with(it) {
                setLong(1, eventId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(DELETE, eventId.toString()))
    }

    fun getEventsById(eventId: Long) : DbEvent? {
        return conn.safeExecute(SELECT_BY_ID, {
            with(it) {
                setLong(1, eventId)
                val res = executeQuery()
                val resToDbEvents = resToDbEvents(res)
                return@with if (resToDbEvents.isEmpty()) null else resToDbEvents[0]
            }
        }, errorMessage(SELECT_BY_ID, eventId.toString()))
    }

    fun getEventsCreateBy(userId: Long) : List<DbEvent> {
        return conn.safeExecute(SELECT_BY_CREATOR, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                return@with resToDbEvents(res)
            }
        }, errorMessage(SELECT_BY_CREATOR, userId.toString()))
    }

    fun getEventsNamed(name: String) : List<DbEvent> {
        return conn.safeExecute(SELECT_BY_NAME, {
            with(it) {
                setString(1, name)
                val res = executeQuery()
                return@with resToDbEvents(res)
            }
        }, errorMessage(SELECT_BY_NAME, name))
    }

    private fun resToDbEvents(res: ResultSet): ArrayList<DbEvent> {
        val events = arrayListOf<DbEvent>()
        while (res.next()) {
            events.add(
                DbEvent(
                    res.getLong("id"),
                    res.getString("name"),
                    res.getLong("creatorId"),
                    res.getString("description"),
                    LocalDate.ofEpochDay(res.getLong("endDate")),
                    if (res.getLong("target") == -1L) null else res.getLong("target")
                )
            )
        }

        return events
    }

    fun eventExists(eventId: Long): Boolean {
        return conn.safeExecute(SELECT_BY_ID, {
            with(it) {
                setLong(1, eventId)
                return@with executeQuery().next()
            }
        }, errorMessage(SELECT_BY_ID, eventId.toString()))
    }
}