package org.aponcet.authserver

data class User(val id: Long, val name: String, val password: String)

/**
 * Provide a user (name and password) from a given name
 */
interface UserProvider {
    /**
     * Return an User from a given name
     *
     * @param name name of the user
     * @return the user (name and password) if exist. return null else
     */
    fun getUser(name: String) : User?
}