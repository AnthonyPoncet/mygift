package org.aponcet.mygift.dbmanager

import io.kotlintest.Description
import io.kotlintest.TestCaseOrder
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlin.test.assertFailsWith

class GiftAccessorTest : StringSpec() {
    private val conn = DbConnection("h2", "mem:test")
    private val giftAccessor = GiftAccessor(conn)

    override fun isInstancePerTest(): Boolean {
        return true
    }

    private fun deleteTable(tables: List<String>) {
        for (table in tables) {
            conn.safeExecute("DELETE FROM $table", { it.executeUpdate() }, "Could not clean $table")
            conn.safeExecute("ALTER TABLE $table ALTER COLUMN id RESTART WITH 1", { it.executeUpdate() }, "Could not reset sequence id $table"
            )
        }
    }

    override fun beforeTest(description: Description) {
        val usersAccessor = UsersAccessor(conn)
        usersAccessor.createIfNotExists()
        val categoryAccessor = CategoryAccessor(conn)
        categoryAccessor.createIfNotExists()
        giftAccessor.createIfNotExists()
        deleteTable(listOf(giftAccessor.getTableName(), categoryAccessor.getTableName(), usersAccessor.getTableName())) //order matter due to foreign key

        usersAccessor.addUser("name1", "pwd", "")
        usersAccessor.addUser("name2", "pwd", "")
        categoryAccessor.addCategory(1L, NewCategory("Default"))
        categoryAccessor.addCategory(1L, NewCategory("cat1"))
        categoryAccessor.addCategory(2L, NewCategory("Default"))
        categoryAccessor.addCategory(2L, NewCategory("cat2"))
    }

    override fun testCaseOrder(): TestCaseOrder? {
        return TestCaseOrder.Random
    }

    init {
        "Add one gift with name only ranked one." {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)

            val expected = DbGift(1L, 1L,
                "g1", null, null, null, 1L, null, false, 1L)
            giftAccessor.getGift(1L) shouldBe expected
            giftAccessor.getUserGifts(1L) shouldBe listOf(expected)
            giftAccessor.getUserGifts(2L) shouldBe emptyList()
            giftAccessor.getFriendGifts(1L) shouldBe listOf(expected)
            giftAccessor.getFriendGifts(2L) shouldBe emptyList()
        }

        "Add one gift with all fields ranked one." {
            giftAccessor.addGift(2L,
                NewGift("g1", "desc", "1€", "Here", 3L, "nice_pic.jpg"),
                false)

            val expected = DbGift(1L, 2L,
                "g1", "desc", "1€", "Here", 3L, "nice_pic.jpg", false, 1L)
            giftAccessor.getGift(1L) shouldBe expected
            giftAccessor.getUserGifts(1L) shouldBe emptyList()
            giftAccessor.getUserGifts(2L) shouldBe listOf(expected)
            giftAccessor.getFriendGifts(1L) shouldBe emptyList()
            giftAccessor.getFriendGifts(2L) shouldBe listOf(expected)
        }

        "Add one gift unknown user throw." {
            assertFailsWith(DbException::class) {
                giftAccessor.addGift(3L, NewGift(name = "g1", categoryId = 1L), false)
            }
        }

        "Add one gift unknown category throw." {
            assertFailsWith(DbException::class) {
                giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 6L), false)
            }
        }

        //TODO: not working today
        "!Add one gift category does not belong to user throw." {
            assertFailsWith(DbException::class) {
                giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 3L), false)
            }
        }
    }
}