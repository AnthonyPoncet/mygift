package org.aponcet.authserver

import org.aponcet.mygift.dbmanager.DbConnection
import org.aponcet.mygift.dbmanager.NewSession
import org.aponcet.mygift.dbmanager.SessionAccessor
import org.aponcet.mygift.dbmanager.UsersAccessor

class DbUserProvider(dbPath: String) : UserProvider {
    private val conn = DbConnection("sqlite", dbPath)
    private val usersAccessor = UsersAccessor(conn)
    private val sessionAccessor = SessionAccessor(conn)

    init {
        usersAccessor.createIfNotExists()
        sessionAccessor.createIfNotExists()
    }

    override fun addUser(name: String, password: ByteArray, salt: ByteArray, picture: String, dateOfBirth: Long?) {
        usersAccessor.addUser(name, password, salt, picture, dateOfBirth)
    }

    override fun getUser(name: String): User? {
        val user = usersAccessor.getUser(name)
        return if (user == null) null else User(user.id, user.name, EncodedPasswordAndSalt(user.password, user.salt))
    }

    override fun modifyUser(name: String, password: ByteArray, salt: ByteArray) {
        usersAccessor.modifyPassword(name, password, salt)
    }

    override fun addSession(session: String, userId: Long) {
        sessionAccessor.addSession(NewSession(session, userId))
    }
}