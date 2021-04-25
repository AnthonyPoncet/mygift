package org.aponcet.mygift.dbmanager

import java.io.ByteArrayInputStream

class UsersAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO users (name,password,salt,picture) VALUES (?, ?, ?, ?)"
        const val SELECT_BY_NAME = "SELECT * FROM users WHERE name= ?"
        const val SELECT_BY_ID = "SELECT * FROM users WHERE id= ?"
        const val UPDATE = "UPDATE users SET name = ?, picture = ? WHERE id = ?"
        const val UPDATE_PWD = "UPDATE users SET password = ?, salt = ? WHERE name = ?"
    }

    override fun getTableName(): String {
        return "users"
    }

    override fun createIfNotExists() {
        conn.execute(
            "CREATE TABLE IF NOT EXISTS users (" +
                    "id         INTEGER PRIMARY KEY ${conn.autoIncrement}, " +
                    "name       TEXT NOT NULL, " +
                    "password   BLOB NOT NULL, " +
                    "salt       BLOB NOT NULL, " +
                    "picture    TEXT)"
        )
    }

    fun addUser(userName: String, password: ByteArray, salt: ByteArray, picture: String): DbUser {
        val nextUserId = conn.safeExecute(
            INSERT, {
                with(it) {
                    setString(1, userName)
                    setBinaryStream(2, ByteArrayInputStream(password), password.size)
                    setBinaryStream(3, ByteArrayInputStream(salt), salt.size)
                    setString(4, picture)
                    val rowCount = executeUpdate()
                    if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                    if (generatedKeys.next()) {
                        return@with generatedKeys.getLong(1)
                    } else {
                        throw Exception("executeUpdate, no key generated")
                    }
                }
            },
            errorMessage(INSERT, userName, password.toString(), salt.toString(), picture)
        )

        return DbUser(nextUserId, userName, password, salt, picture)
    }

    fun getUser(userName: String): DbUser? {
        return conn.safeExecute(SELECT_BY_NAME, {
            with(it) {
                setString(1, userName)
                val res = executeQuery()
                return@with if (res.next()) {
                    val picture = res.getString("picture")
                    val password = res.getBinaryStream("password")
                    val salt = res.getBinaryStream("salt")
                    DbUser(
                        res.getLong("id"),
                        res.getString("name"),
                        password.readBytes(),
                        salt.readBytes(),
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
                return@with if (res.next()) {
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
        return conn.safeExecute(SELECT_BY_ID, {
            with(it) {
                setLong(1, userId)
                val res = executeQuery()
                return@with res.next()
            }
        }, errorMessage(SELECT_BY_ID, userId.toString()))
    }

    fun modifyPassword(name: String, password: ByteArray, salt: ByteArray) {
        return conn.safeExecute(UPDATE_PWD, {
            with(it) {
                setBinaryStream(1, ByteArrayInputStream(password), password.size)
                setBinaryStream(2, ByteArrayInputStream(salt), salt.size)
                setString(3, name)
                val rowCount = executeUpdate()
                if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
            }
        }, errorMessage(UPDATE_PWD, password.toString(), salt.toString(), name))
    }
}