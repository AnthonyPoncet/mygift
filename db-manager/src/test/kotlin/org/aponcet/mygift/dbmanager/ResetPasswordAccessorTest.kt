package org.aponcet.mygift.dbmanager

import io.kotlintest.*
import io.kotlintest.specs.StringSpec
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertFailsWith

class ResetPasswordAccessorTest : StringSpec() {
    private val conn = DbConnection("h2", "mem:test")
    private val resetPasswordAccessor = ResetPasswordAccessor(conn)

    override fun isInstancePerTest(): Boolean {
        return true
    }

    private fun deleteTable(tables: List<String>) {
        for (table in tables) {
            conn.safeExecute("DELETE FROM $table", { it.executeUpdate() }, "Could not clean $table")
            if (table != resetPasswordAccessor.getTableName()) {
                conn.safeExecute(
                    "ALTER TABLE $table ALTER COLUMN id RESTART WITH 1",
                    { it.executeUpdate() },
                    "Could not reset sequence id $table"
                )
            }
        }
    }

    override fun beforeTest(description: Description) {
        val usersAccessor = UsersAccessor(conn)
        usersAccessor.createIfNotExists()
        resetPasswordAccessor.createIfNotExists()
        deleteTable(listOf(resetPasswordAccessor.getTableName(), usersAccessor.getTableName()))

        usersAccessor.addUser("name1", "pwd".toByteArray(), "azerty".toByteArray(), "")
    }

    override fun afterTest(description: Description, result: TestResult) {
        deleteTable(
            listOf(
                resetPasswordAccessor.getTableName(),
                UsersAccessor(conn).getTableName()
            )
        ) //order matter due to foreign key
    }

    override fun testCaseOrder(): TestCaseOrder? {
        return TestCaseOrder.Random
    }

    init {
        "Add new entry" {
            resetPasswordAccessor.addEntry(1) shouldNotBe null
            resetPasswordAccessor.getEntries(1).size shouldBe 1
        }

        "Get entry" {
            val inserted = resetPasswordAccessor.addEntry(1)

            resetPasswordAccessor.getEntry(inserted.userId, inserted.uuid) shouldBe inserted
            resetPasswordAccessor.getEntry(inserted.uuid) shouldBe inserted
            resetPasswordAccessor.getEntries(inserted.userId) shouldBe arrayListOf(inserted)
        }

        "Could not insert two times for same user if first not expired" {
            resetPasswordAccessor.addEntry(1) shouldNotBe null
            assertFailsWith(DbResetPasswordException::class) {
                resetPasswordAccessor.addEntry(1)
            }
            resetPasswordAccessor.getEntries(1).size shouldBe 1
        }

        "Could insert new entry for same user if first expired" {
            insertExpiredEntry()
            resetPasswordAccessor.getEntries(1).size shouldBe 1

            resetPasswordAccessor.addEntry(1) shouldNotBe null
            resetPasswordAccessor.getEntries(1).size shouldBe 1 //old entry has been clean
        }

        "Delete entry" {
            val inserted = resetPasswordAccessor.addEntry(1)
            resetPasswordAccessor.getEntries(1) shouldBe arrayListOf(inserted)

            resetPasswordAccessor.delete(inserted.userId, inserted.uuid)
            resetPasswordAccessor.getEntries(1).size shouldBe 0
        }
    }

    private fun insertExpiredEntry() {
        val expiry = LocalDateTime.now().minusDays(1).toEpochSecond(ZoneOffset.UTC)
        conn.safeExecute(
            ResetPasswordAccessor.INSERT, {
                with(it) {
                    setLong(1, 1L)
                    setString(2, "uuid")
                    setLong(3, expiry)
                    val rowCount = executeUpdate()
                    if (rowCount == 0) throw Exception("executeUpdate return no rowCount")
                }
            },
            ""
        )
    }
}