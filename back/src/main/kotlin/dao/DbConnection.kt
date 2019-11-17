package dao

import java.sql.*

class DbConnection(dbPath: String) {
    private var conn: Connection
    init {
        val url = "jdbc:sqlite:$dbPath"
        conn = DriverManager.getConnection(url)
    }

    fun prepare(query: String): PreparedStatement {
        try {
            return conn.prepareStatement(query) ?: throw IllegalStateException("prepareStatement returned null")
        } catch (e: Exception) {
            throw DbException("Unable to prepare query: $query", e)
        }
    }

    fun <T> safeExecute(queryString: String, function: (PreparedStatement)->T, message: String) : T {
        val query = prepare(queryString)
        try {
            return function(query)
        } catch (e: Exception) {
            throw DbException(message, e)
        } finally {
            query.close()
        }
    }

    fun execute(query: String) {
        val statement = conn.createStatement()
        statement.execute(query)
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