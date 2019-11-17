package dao

import java.sql.ResultSet

class FriendRequestAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO friendRequest(userOne,userTwo,status) VALUES (?, ?, ?)"
        const val SELECT_BY_USERONE = "SELECT * FROM friendRequest WHERE userOne=?"
        const val SELECT_BY_USERTWO = "SELECT * FROM friendRequest WHERE userTwo=?"
        const val SELECT_BY_BOTH_USER = "SELECT * FROM friendRequest WHERE userOne = ? AND userTwo = ?"
        const val SELECT_BY_ID_AND_USERONE = "SELECT * FROM friendRequest WHERE id=? AND userOne=?"
        const val SELECT_BY_ID_AND_USERTWO = "SELECT * FROM friendRequest WHERE id=? AND userTwo=?"
        const val UPDATE = "UPDATE friendRequest SET status = ? WHERE id = ?"
        const val DELETE = "DELETE FROM friendRequest WHERE id = ?"
    }

    override fun getTableName(): String {
        return "friendRequest"
    }

    override fun createIfNotExists() {
        conn.execute("CREATE TABLE IF NOT EXISTS friendRequest (" +
            "id         INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "userOne    INTEGER NOT NULL, " +
            "userTwo    INTEGER NOT NULL, " +
            "status     TEXT NOT NULL, " +
            "FOREIGN KEY(userOne) REFERENCES users(id), " +
            "FOREIGN KEY(userTwo) REFERENCES users(id))")
    }

    fun createFriendRequest(userOne: Long, userTwo: Long) {
        conn.safeExecute(INSERT, {
            with(it) {
                setLong(1, userOne)
                setLong(2, userTwo)
                setString(3, RequestStatus.PENDING.name)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(INSERT, userOne.toString(), userTwo.toString(), RequestStatus.PENDING.name))
    }

    private fun convertOneRes(res: ResultSet): DbFriendRequest {
        return DbFriendRequest(
            res.getLong("id"),
            res.getLong("userOne"),
            res.getLong("userTwo"),
            RequestStatus.valueOf(res.getString("status"))
        )
    }

    fun getInitiatedFriendRequests(userId: Long) : List<DbFriendRequest> {
        return conn.safeExecute(SELECT_BY_USERONE, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                val requests = arrayListOf<DbFriendRequest>()
                while (res.next()) requests.add(convertOneRes(res))
                return@with requests
            }
        }, errorMessage(SELECT_BY_USERONE, userId.toString()))
    }

    fun getReceivedFriendRequests(userId: Long) : List<DbFriendRequest> {
        return conn.safeExecute(SELECT_BY_USERTWO, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                val requests = arrayListOf<DbFriendRequest>()
                while (res.next()) requests.add(convertOneRes(res))
                return@with requests
            }
        }, errorMessage(SELECT_BY_USERTWO, userId.toString()))
    }

    fun deleteFriendRequest(friendRequestId: Long) {
        conn.safeExecute(DELETE, {
            with(it) {
                setLong(1, friendRequestId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(DELETE, friendRequestId.toString()))
    }

    private fun update(requestStatus: RequestStatus, friendRequestId: Long) {
        conn.safeExecute(UPDATE, {
            with(it) {
                setString(1, requestStatus.name)
                setLong(2, friendRequestId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE, requestStatus.name, friendRequestId.toString()))
    }

    fun acceptFriendRequest(friendRequestId: Long) {
        update(RequestStatus.ACCEPTED, friendRequestId)
    }

    fun declineFriendRequest(friendRequestId: Long, blockUser: Boolean) {
        if (blockUser) {
            update(RequestStatus.REJECTED, friendRequestId)
        } else {
            deleteFriendRequest(friendRequestId)
        }
    }

    fun getFriendRequest(userOne: Long, userTwo: Long) : DbFriendRequest? {
        return conn.safeExecute(SELECT_BY_BOTH_USER, {
            with(it) {
                setLong(1, userOne)
                setLong(2, userTwo)
                val res = executeQuery()
                return@with if (res.next()) convertOneRes(res) else null
            }
        }, errorMessage(SELECT_BY_BOTH_USER, userOne.toString(), userTwo.toString()))
    }

    fun friendRequestBelongToUser(userId: Long, friendRequestId: Long): Boolean {
        return conn.safeExecute(SELECT_BY_ID_AND_USERONE, {
            with(it) {
                setLong(1, friendRequestId)
                setLong(2, userId)
                return@with executeQuery().next()
            }
        }, errorMessage(SELECT_BY_ID_AND_USERONE, friendRequestId.toString(), userId.toString()))
    }

    fun friendRequestIsNotForUser(userId: Long, friendRequestId: Long): Boolean {
        return conn.safeExecute(SELECT_BY_ID_AND_USERTWO, {
            with(it) {
                setLong(1, friendRequestId)
                setLong(2, userId)
                return@with executeQuery().next()
            }
        }, errorMessage(SELECT_BY_ID_AND_USERTWO, friendRequestId.toString(), userId.toString()))
    }
}