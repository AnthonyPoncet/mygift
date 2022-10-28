package org.aponcet.mygift.dbmanager

data class NewSession(
    val session: String,
    val userId: Long,
)

class SessionAccessor(private val conn: DbConnection) : DaoAccessor() {
    companion object {
        const val INSERT = "INSERT INTO sessions (session,userId) VALUES (?,?)"
        const val SELECT_USER_BY_SESSION = "SELECT userId FROM sessions where session=?"
        const val DELETE = "DELETE FROM sessions WHERE session=? AND userId=?"
    }

    override fun getTableName(): String {
        return "sessions"
    }

    override fun createIfNotExists() {
        conn.execute(
            "CREATE TABLE IF NOT EXISTS sessions (" +
                    "session    TEXT NOT NULL, " +
                    "userId     INTEGER NOT NULL, " +
                    "UNIQUE(session, userId), " +
                    "FOREIGN KEY(userId) REFERENCES users(id))"
        )
    }

    fun addSession(newSession: NewSession) {
        conn.safeExecute(INSERT, {
            with(it) {
                setString(1, newSession.session)
                setLong(2, newSession.userId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, newSession.toString()))
    }

    fun getUsersOfSession(session: String): List<Long> {
        return conn.safeExecute(SELECT_USER_BY_SESSION, {
            with(it) {
                setString(1, session)
                val res = executeQuery()
                val out = arrayListOf<Long>()
                while (res.next()) {
                    out.add(res.getLong("userId"))
                }
                out
            }
        }, errorMessage(SELECT_USER_BY_SESSION, session))
    }

    fun deleteSession(session: String, userId: Long) {
        conn.safeExecute(DELETE, {
            with(it) {
                setString(1, session)
                setLong(2, userId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, session, userId.toString()))
    }
}