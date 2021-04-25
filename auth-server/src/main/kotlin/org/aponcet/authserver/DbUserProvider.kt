package org.aponcet.authserver

import org.aponcet.mygift.dbmanager.DbConnection
import org.aponcet.mygift.dbmanager.UsersAccessor

class DbUserProvider(dbPath: String) : UserProvider {
    private val conn = DbConnection("sqlite", dbPath)
    private val usersAccessor = UsersAccessor(conn)

    init {
        usersAccessor.createIfNotExists()
    }

    override fun addUser(name: String, password: ByteArray, salt: ByteArray, picture: String) {
        usersAccessor.addUser(name, password, salt, picture)
    }

    override fun getUser(name: String): User? {
        val user = usersAccessor.getUser(name)
        return if (user == null) null else User(user.id, user.name, EncodedPasswordAndSalt(user.password, user.salt))
    }

    override fun modifyUser(name: String, password: ByteArray, salt: ByteArray) {
        usersAccessor.modifyPassword(name, password, salt)
    }
}