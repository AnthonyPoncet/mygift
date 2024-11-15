package org.aponcet.mygift.dbmanager

import io.kotlintest.*
import io.kotlintest.specs.StringSpec
import kotlin.test.assertFailsWith

class ToDeleteGiftsAccessorTest : StringSpec() {
    private val conn = DbConnection("h2", "mem:test")
    private val toDeleteGiftsAccessor = ToDeleteGiftsAccessor(conn)

    override fun isolationMode() = IsolationMode.InstancePerTest

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

    override fun beforeTest(testCase: TestCase) {
        val usersAccessor = UsersAccessor(conn)
        usersAccessor.createIfNotExists()
        toDeleteGiftsAccessor.createIfNotExists()
        conn.safeExecute(
            "DELETE FROM ${toDeleteGiftsAccessor.getTableName()}",
            { it.executeUpdate() },
            "Could not clean ${toDeleteGiftsAccessor.getTableName()}"
        )
        deleteTable(listOf(usersAccessor.getTableName())) //order matter due to foreign key

        usersAccessor.addUser("name1", "pwd".toByteArray(), "azerty".toByteArray(), "", null)
        usersAccessor.addUser("name2", "pwd".toByteArray(), "otherSalt".toByteArray(), "", null)
    }

    override fun afterTest(testCase: TestCase, result: TestResult) {
        conn.safeExecute(
            "DELETE FROM ${toDeleteGiftsAccessor.getTableName()}",
            { it.executeUpdate() },
            "Could not clean ${toDeleteGiftsAccessor.getTableName()}"
        )
        deleteTable(listOf(UsersAccessor(conn).getTableName())) //order matter due to foreign key
    }

    override fun testCaseOrder(): TestCaseOrder {
        return TestCaseOrder.Random
    }

    init {
        "Add one gift." {
            toDeleteGiftsAccessor.add(
                DbGift(1L, "g1", null, null, null, 1L, null, false, heart = false, rank = 1L),
                1L,
                Status.RECEIVED,
                DbFriendActionOnGift(1L, 1L, 2L)
            )

            toDeleteGiftsAccessor.getDeletedGiftsWhereUserHasActionOn(2L) shouldBe listOf(
                DbToDeleteGifts(
                    1L,
                    1L,
                    "g1",
                    null,
                    null,
                    null,
                    null,
                    Status.RECEIVED,
                    2L
                )
            )
        }

        "Delete one gift" {
            toDeleteGiftsAccessor.add(
                DbGift(1L, "g1", null, null, null, 1L, null, false, heart = false, rank = 1L),
                1L,
                Status.RECEIVED,
                DbFriendActionOnGift(1L, 1L, 2L)
            )

            toDeleteGiftsAccessor.deleteDeletedGift(1L, 2L)
            toDeleteGiftsAccessor.getDeletedGiftsWhereUserHasActionOn(2L) shouldBe emptyList()
        }

        "Delete one gift wrong friend id" {
            toDeleteGiftsAccessor.add(
                DbGift(1L, "g1", null, null, null, 1L, null, false, heart = false, rank = 1L),
                1L,
                Status.RECEIVED,
                DbFriendActionOnGift(1L, 1L, 2L)
            )

            assertFailsWith(DbException::class) {
                toDeleteGiftsAccessor.deleteDeletedGift(1L, 1L)
            }
        }

        "Delete one gift wrong gift id" {
            toDeleteGiftsAccessor.add(
                DbGift(1L, "g1", null, null, null, 1L, null, false, heart = false, rank = 1L),
                1L,
                Status.RECEIVED,
                DbFriendActionOnGift(1L, 1L, 2L)
            )

            assertFailsWith(DbException::class) {
                toDeleteGiftsAccessor.deleteDeletedGift(5L, 1L)
            }
        }
    }
}