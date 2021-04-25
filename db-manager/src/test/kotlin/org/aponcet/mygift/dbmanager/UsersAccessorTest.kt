package org.aponcet.mygift.dbmanager

import io.kotlintest.Description
import io.kotlintest.TestCaseOrder
import io.kotlintest.TestResult
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class UsersAccessorTest : StringSpec() {
    private val conn = DbConnection("h2", "mem:test")
    private val usersAccessor = UsersAccessor(conn)

    override fun isInstancePerTest(): Boolean {
        return true
    }

    private fun deleteTable(tables: List<String>) {
        for (table in tables) {
            conn.safeExecute("DELETE FROM $table", { it.executeUpdate() }, "Could not clean $table")
            conn.safeExecute(
                "ALTER TABLE $table ALTER COLUMN id RESTART WITH 1",
                { it.executeUpdate() },
                "Could not reset sequence id $table"
            )
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
        "Add user" {
            val expected = DbUser(1L, "name", "pwd".toByteArray(), "azerty".toByteArray(), "pic.jpg")

            usersAccessor.addUser("name", "pwd".toByteArray(), "azerty".toByteArray(), "pic.jpg") shouldBe expected

            usersAccessor.getUser("name") shouldBe expected
            usersAccessor.getUser(1L) shouldBe NakedUser("name", "pic.jpg")
        }

        "Unknown name return null" {
            usersAccessor.getUser("name") shouldBe null
        }

        "Unknown id return null" {
            usersAccessor.getUser(1L) shouldBe null
        }

        "Modify user" {
            val expected = DbUser(1L, "name", "pwd".toByteArray(), "azerty".toByteArray(), "pic.jpg")
            usersAccessor.addUser("name", "pwd".toByteArray(), "azerty".toByteArray(), "pic.jpg") shouldBe expected

            usersAccessor.modifyUser(1L, "other", "best_pic.jpg")
            usersAccessor.getUser("name") shouldBe null
            usersAccessor.getUser("other") shouldBe DbUser(
                1L,
                "other",
                "pwd".toByteArray(),
                "azerty".toByteArray(),
                "best_pic.jpg"
            )
            usersAccessor.getUser(1L) shouldBe NakedUser("other", "best_pic.jpg")
        }

        "User exists" {
            val expected = DbUser(1L, "name", "pwd".toByteArray(), "azerty".toByteArray(), "pic.jpg")
            usersAccessor.addUser("name", "pwd".toByteArray(), "azerty".toByteArray(), "pic.jpg") shouldBe expected

            usersAccessor.userExists(1L) shouldBe true
            usersAccessor.userExists(2L) shouldBe false
        }
    }
}