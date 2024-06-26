package org.aponcet.authserver

data class User(val id: Long, val name: String, val encodedPasswordAndSalt: EncodedPasswordAndSalt)

/**
 * Provide a user (name and password) from a given name
 */
interface UserProvider {

    /**
     * Add a new user
     */
    fun addUser(name: String, password: ByteArray, salt: ByteArray, picture: String, dateOfBirth: Long?)

    /**
     * Return an User from a given name
     *
     * @param name name of the user
     * @return the user (name and password) if exist. return null else
     */
    fun getUser(name: String): User?

    /**
     * Modify password for an existing user
     */
    fun modifyUser(name: String, password: ByteArray, salt: ByteArray)

    /**
     * Add a session for a user
     */
    fun addSession(session: String, userId: Long)
}