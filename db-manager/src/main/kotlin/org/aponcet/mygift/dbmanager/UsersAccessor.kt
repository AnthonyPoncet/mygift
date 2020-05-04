package org.aponcet.mygift.dbmanager

class UsersAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO users (name,password,picture) VALUES (?, ?, ?)"
        const val SELECT_BY_NAME = "SELECT * FROM users WHERE name= ?"
        const val SELECT_BY_ID = "SELECT * FROM users WHERE id= ?"
        const val UPDATE = "UPDATE users SET name = ?, picture = ? WHERE id = ?"
    }

    override fun getTableName(): String {
        return "users"
    }

    override fun createIfNotExists() {
        conn.execute("CREATE TABLE IF NOT EXISTS users (" +
            "id         INTEGER PRIMARY KEY ${conn.autoIncrement}, " +
            "name       TEXT NOT NULL, " +
            "password   TEXT NOT NULL, " +
            "picture    TEXT)")
    }

    fun addUser(userName: String, password: String, picture: String): DbUser {
        val nextUserId = conn.safeExecute(
            INSERT, {
            with(it) {
                setString(1, userName)
                setString(2, password)
                setString(3, picture)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                if (generatedKeys.next()) {
                    return@with generatedKeys.getLong(1)
                } else {
                    throw Exception("executeUpdate, no key generated")
                }
            }
        },
        errorMessage(INSERT, userName, password, picture))

        return DbUser(nextUserId, userName, password, picture)
    }

    fun getUser(userName: String): DbUser? {
        return conn.safeExecute(SELECT_BY_NAME, {
            with(it) {
                setString(1, userName)
                val res = executeQuery()
                return@with if (res.next()) {
                    val picture = res.getString("picture")
                    DbUser(
                        res.getLong("id"),
                        res.getString("name"),
                        res.getString("password"),
                        if (picture.isEmpty()) null else picture
                    )
                } else {
                    null
                }
            }
        }, errorMessage(SELECT_BY_NAME, userName))
    }

    fun getUser(userId: Long): NakedUser? {
        return conn.safeExecute(SELECT_BY_ID, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                return@with if(res.next()) {
                    val picture = res.getString("picture")
                    NakedUser(
                        res.getString("name"),
                        if (picture.isEmpty()) null else picture
                    )
                } else {
                    null
                }
            }
        }, errorMessage(SELECT_BY_ID, userId.toString()))
    }

    fun modifyUser(userId: Long, name: String, picture: String) {
        conn.safeExecute(UPDATE, {
            with(it) {
                setString(1, name)
                setString(2, picture)
                setLong(3, userId)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE, name, picture, userId.toString()))
    }

    fun userExists(userId: Long): Boolean {
        return conn.safeExecute(SELECT_BY_ID,{
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                return@with res.next()
            }
        }, errorMessage(SELECT_BY_ID, userId.toString()))
    }
}