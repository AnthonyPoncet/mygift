package org.aponcet.mygift.dbmanager

import java.sql.*

/**
 * Manage SQL execution
 */
class DbConnection(driver: String, dbPath: String) {
    private val conn: Connection
    val autoIncrement: String
    val conflict_ignore: String

    init {
        val url = "jdbc:$driver:$dbPath"
        conn = DriverManager.getConnection(url)
        autoIncrement = when (driver) {
            "sqlite" -> "AUTOINCREMENT"
            "h2" -> "AUTO_INCREMENT"
            else -> throw IllegalArgumentException("Not supported driver: $driver")
        }
        conflict_ignore = when (driver) {
            "sqlite" -> "ON CONFLICT IGNORE"
            "h2" -> ""
            else -> throw IllegalArgumentException("Not supported driver: $driver")
        }
    }

    fun <T> safeExecute(queryString: String, function: (PreparedStatement) -> T, message: String): T {
        val query = prepare(queryString)
        try {
            return function(query)
        } catch (e: Exception) {
            throw DbException(message, e)
        } finally {
            query.close()
        }
    }

    private fun prepare(query: String): PreparedStatement {
        try {
            return conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
                ?: throw IllegalStateException("prepareStatement returned null")
        } catch (e: Exception) {
            throw DbException("Unable to prepare query: $query", e)
        }
    }

    //TODO: 3 functions bellow should be removed

    fun execute(query: String) {
        val statement = conn.createStatement()
        statement.execute(query)
        statement.close()
    }

    fun executeQuery(query: String): ResultSet {
        val statement = conn.createStatement()
        return statement.executeQuery(query)
    }

    fun executeUpdate(query: String) {
        val statement = conn.createStatement()
        statement.executeUpdate(query)
    }

    fun close() {
        conn.close()
    }
}