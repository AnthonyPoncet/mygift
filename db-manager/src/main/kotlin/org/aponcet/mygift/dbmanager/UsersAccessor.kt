package org.aponcet.mygift.dbmanager

import java.io.ByteArrayInputStream

class UsersAccessor(private val conn: DbConnection) : DaoAccessor() {

    companion object {
        const val INSERT = "INSERT INTO users (name,password,salt,picture,dateOfBirth) VALUES (?, ?, ?, ?, ?)"
        const val SELECT_BY_NAME = "SELECT id,name,password,salt,picture,dateOfBirth FROM users WHERE name= ?"
        const val SELECT_BY_ID = "SELECT id,name,password,salt,picture,dateOfBirth FROM users WHERE id= ?"
        const val UPDATE = "UPDATE users SET name = ?, picture = ?, dateOfBirth = ? WHERE id = ?"
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
                    "picture    TEXT, " +
                    "dateOfBirth LONG)"
        )
    }

    fun addUser(userName: String, password: ByteArray, salt: ByteArray, picture: String, dateOfBirth: Long?): DbUser {
        val nextUserId = conn.safeExecute(
            INSERT, {
                with(it) {
                    setString(1, userName)
                    setBinaryStream(2, ByteArrayInputStream(password), password.size)
                    setBinaryStream(3, ByteArrayInputStream(salt), salt.size)
                    setString(4, picture)
                    if (dateOfBirth != null) setLong(5, dateOfBirth) else setNull(5, 5)
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

        return DbUser(nextUserId, userName, password, salt, picture, dateOfBirth)
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
                    val dateOfBirth = res.getLong("dateOfBirth")
                    DbUser(
                        res.getLong("id"),
                        res.getString("name"),
                        password.readBytes(),
                        salt.readBytes(),
                        picture.ifEmpty { null },
                        if (dateOfBirth == 0L) null else dateOfBirth
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
                    val dateOfBirth = res.getLong("dateOfBirth")
                    NakedUser(
                        res.getString("name"),
                        picture.ifEmpty { null },
                        if (dateOfBirth == 0L) null else dateOfBirth
                    )
                } else {
                    null
                }
            }
        }, errorMessage(SELECT_BY_ID, userId.toString()))
    }

    fun modifyUser(userId: Long, name: String, picture: String, dateOfBirth: Long?) {
        conn.safeExecute(UPDATE, {
            with(it) {
                setString(1, name)
                setString(2, picture)
                if (dateOfBirth != null) setLong(3, dateOfBirth) else setNull(3, 3)
                setLong(4, userId)
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