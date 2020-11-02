package org.aponcet.authserver

data class User(val id: Long, val name: String, val encodedPasswordAndSalt: EncodedPasswordAndSalt)

/**
 * Provide a user (name and password) from a given name
 */
interface UserProvider {

    /**
     * Add a new user
     *
     * @return success
     */
    fun addUser(name: String, password: ByteArray, salt: ByteArray, picture: String)

    /**
     * Return an User from a given name
     *
     * @param name name of the user
     * @return the user (name and password) if exist. return null else
     */
    fun getUser(name: String) : User?
}