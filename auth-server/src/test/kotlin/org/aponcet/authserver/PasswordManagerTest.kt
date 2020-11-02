package org.aponcet.authserver

import io.kotlintest.Description
import io.kotlintest.TestCaseOrder
import io.kotlintest.TestResult
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.aponcet.mygift.dbmanager.DbConnection
import org.aponcet.mygift.dbmanager.DbUser
import org.aponcet.mygift.dbmanager.UsersAccessor

class PasswordManagerTest : StringSpec() {
    private val conn = DbConnection("h2", "mem:test")
    private val usersAccessor = UsersAccessor(conn)

    override fun isInstancePerTest(): Boolean {
        return true
    }

    private fun deleteTable(tables: List<String>) {
        for (table in tables) {
            conn.safeExecute("DELETE FROM $table", { it.executeUpdate() }, "Could not clean $table")
            conn.safeExecute("ALTER TABLE $table ALTER COLUMN id RESTART WITH 1", { it.executeUpdate() }, "Could not reset sequence id $table")
        }
    }

    override fun beforeTest(description: Description) {
        usersAccessor.createIfNotExists()
        deleteTable(listOf(usersAccessor.getTableName()))
    }

    override fun afterTest(description: Description, result: TestResult) {
        deleteTable(listOf(usersAccessor.getTableName())) //order matter due to foreign key
    }

    override fun testCaseOrder(): TestCaseOrder? {
        return TestCaseOrder.Random
    }

    init {
        "Generate password with salt and compare ok" {
            val generateEncodedPassword = PasswordManager.generateEncodedPassword("Toto")
            assert(PasswordManager.isPasswordOk("Toto", generateEncodedPassword.salt, generateEncodedPassword.encodedPassword))
        }

        "Generate password with salt and compare nok" {
            val generateEncodedPassword = PasswordManager.generateEncodedPassword("Toto")
            assert(!PasswordManager.isPasswordOk("toto", generateEncodedPassword.salt, generateEncodedPassword.encodedPassword))
        }

        "Could save generated password to DB" {
            val encodedPassword = PasswordManager.generateEncodedPassword("Toto")
            usersAccessor.addUser("name", encodedPassword.encodedPassword, encodedPassword.salt, "") shouldBe
                    DbUser(1L, "name", encodedPassword.encodedPassword, encodedPassword.salt, "" )
        }

        "Compare password to saved one work" {
            val encodedPassword = PasswordManager.generateEncodedPassword("Toto")
            usersAccessor.addUser("name", encodedPassword.encodedPassword, encodedPassword.salt, "")
            val user = usersAccessor.getUser("name")
            assert(PasswordManager.isPasswordOk("Toto", encodedPassword.salt, encodedPassword.encodedPassword))
        }
    }
}