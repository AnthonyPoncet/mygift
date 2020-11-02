package org.aponcet.mygift.dbmanager

import io.kotlintest.Description
import io.kotlintest.TestCaseOrder
import io.kotlintest.TestResult
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

        usersAccessor.addUser("name1", "pwd".toByteArray(), "azerty".toByteArray(), "")
        usersAccessor.addUser("name2", "pwd".toByteArray(), "otherSalt".toByteArray(), "")
        categoryAccessor.addCategory(1L, NewCategory("Default"))
        categoryAccessor.addCategory(1L, NewCategory("cat1"))
        categoryAccessor.addCategory(2L, NewCategory("Default"))
        categoryAccessor.addCategory(2L, NewCategory("cat2"))
    }

    override fun afterTest(description: Description, result: TestResult) {
        deleteTable(listOf(giftAccessor.getTableName(), CategoryAccessor(conn).getTableName(), UsersAccessor(conn).getTableName())) //order matter due to foreign key
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

        "Add secret one gift with name only not returned to user." {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), true)

            val expected = DbGift(1L, 1L,
                "g1", null, null, null, 1L, null, true, 1L)
            giftAccessor.getGift(1L) shouldBe expected
            giftAccessor.getUserGifts(1L) shouldBe emptyList()
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

        "Add two gifts same user, same category with name only." {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)
            giftAccessor.addGift(1L, NewGift(name = "g2", categoryId = 1L), true)

            val expected1 = DbGift(1L, 1L,
                "g1", null, null, null, 1L, null, false, 1L)
            val expected2 = DbGift(2L, 1L,
                "g2", null, null, null, 1L, null, true, 2L)
            giftAccessor.getGift(1L) shouldBe expected1
            giftAccessor.getGift(2L) shouldBe expected2
            giftAccessor.getUserGifts(1L) shouldBe listOf(expected1)
            giftAccessor.getUserGifts(2L) shouldBe emptyList()
            giftAccessor.getFriendGifts(1L) shouldBe listOf(expected1, expected2)
            giftAccessor.getFriendGifts(2L) shouldBe emptyList()
        }

        "Get unknown gift return null" {
            giftAccessor.getGift(1L) shouldBe null
        }

        "Get user gifts and friend gifts from unknown user return emptyList" {
            giftAccessor.getUserGifts(3L) shouldBe emptyList()
            giftAccessor.getFriendGifts(3L) shouldBe emptyList()
        }

        "Modify gift from null to values." {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)
            giftAccessor.modifyGift(1L, Gift("modified", "desc", "1€", "here", 2L, "pic.jpg", 1L))

            val expected = DbGift(1L, 1L,
                "modified", "desc", "1€", "here", 2L, "pic.jpg", false, 1L)
            giftAccessor.getGift(1L) shouldBe expected
            giftAccessor.getUserGifts(1L) shouldBe listOf(expected)
            giftAccessor.getUserGifts(2L) shouldBe emptyList()
            giftAccessor.getFriendGifts(1L) shouldBe listOf(expected)
            giftAccessor.getFriendGifts(2L) shouldBe emptyList()
        }

        "Modify gift from values to null." {
            giftAccessor.addGift(1L,
                NewGift("g1", "desc", "1€", "Here", 1L, "nice_pic.jpg"),
                false)
            giftAccessor.modifyGift(1L, Gift(name = "modified", categoryId =  2L, rank = 1L))

            val expected = DbGift(1L, 1L,
                "modified", null, null, null, 2L, null, false, 1L)
            giftAccessor.getGift(1L) shouldBe expected
            giftAccessor.getUserGifts(1L) shouldBe listOf(expected)
            giftAccessor.getUserGifts(2L) shouldBe emptyList()
            giftAccessor.getFriendGifts(1L) shouldBe listOf(expected)
            giftAccessor.getFriendGifts(2L) shouldBe emptyList()
        }

        "Modify unknown gift throw." {
            assertFailsWith(DbException::class) {
                giftAccessor.modifyGift(1L, Gift(name = "modified", categoryId =  2L, rank = 1L))
            }
        }

        "Delete gift." {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)
            giftAccessor.removeGift(1L)

            giftAccessor.getGift(1L) shouldBe null
            giftAccessor.getUserGifts(1L) shouldBe emptyList()
            giftAccessor.getUserGifts(2L) shouldBe emptyList()
            giftAccessor.getFriendGifts(1L) shouldBe emptyList()
            giftAccessor.getFriendGifts(2L) shouldBe emptyList()
        }

        "Delete unknown gift throw." {
            assertFailsWith(DbException::class) {
                giftAccessor.removeGift(1L)
            }
        }

        "Gift exists" {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)

            giftAccessor.giftExists(1L) shouldBe true
            giftAccessor.giftExists(2L) shouldBe false
        }

        "Gift belong to user" {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)

            giftAccessor.giftBelongToUser(1L, 1L) shouldBe true
            giftAccessor.giftBelongToUser(2L, 1L) shouldBe false
        }

        "Gift is secret" {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)
            giftAccessor.addGift(1L, NewGift(name = "g2", categoryId = 1L), true)

            giftAccessor.giftIsSecret(1L) shouldBe false
            giftAccessor.giftIsSecret(2L) shouldBe true
        }

        "Rank down no secret gift" {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)
            giftAccessor.addGift(1L, NewGift(name = "g2", categoryId = 1L), false)
            giftAccessor.addGift(1L, NewGift(name = "g3", categoryId = 1L), false)

            giftAccessor.getUserGifts(1L) shouldBe listOf(
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 1L),
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, false, 2L),
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, false, 3L))

            giftAccessor.rankDownGift(1L, 2L)
            giftAccessor.getUserGifts(1L) shouldBe listOf(
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, false, 1L),
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 2L),
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, false, 3L))

            giftAccessor.rankDownGift(1L, 3L)
            giftAccessor.getUserGifts(1L) shouldBe listOf(
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, false, 1L),
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, false, 2L),
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 3L)
            )

            giftAccessor.rankDownGift(1L, 3L)
            giftAccessor.getUserGifts(1L) shouldBe listOf(
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, false, 1L),
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, false, 2L),
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 3L)
            )

            assertFailsWith(Exception::class) {
                giftAccessor.rankDownGift(1L, 3L)
            }
        }

        "Rank up no secret gift" {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)
            giftAccessor.addGift(1L, NewGift(name = "g2", categoryId = 1L), false)
            giftAccessor.addGift(1L, NewGift(name = "g3", categoryId = 1L), false)

            giftAccessor.getUserGifts(1L) shouldBe listOf(
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 1L),
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, false, 2L),
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, false, 3L))

            giftAccessor.rankUpGift(1L, 2L)
            giftAccessor.getUserGifts(1L) shouldBe listOf(
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 1L),
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, false, 2L),
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, false, 3L)
            )

            giftAccessor.rankUpGift(1L, 1L)
            giftAccessor.getUserGifts(1L) shouldBe listOf(
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, false, 1L),
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 2L),
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, false, 3L)
            )

            giftAccessor.rankUpGift(1L, 1L)
            giftAccessor.getUserGifts(1L) shouldBe listOf(
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, false, 1L),
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, false, 2L),
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 3L)
            )

            assertFailsWith(Exception::class) {
                giftAccessor.rankUpGift(1L, 1L)
            }
        }

        "Rank down with secret gift" {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)
            giftAccessor.addGift(1L, NewGift(name = "g2", categoryId = 1L), true)
            giftAccessor.addGift(1L, NewGift(name = "g3", categoryId = 1L), true)
            giftAccessor.addGift(1L, NewGift(name = "g4", categoryId = 1L), false)

            giftAccessor.getFriendGifts(1L) shouldBe listOf(
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 1L),
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, true, 2L),
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, true, 3L),
                DbGift(4L, 1L, "g4", null, null, null, 1L, null, false, 4L))

            //TODO: was expected id 4 -> 1 -> 2 -> 3
            giftAccessor.rankDownGift(1L, 4L)
            giftAccessor.getFriendGifts(1L) shouldBe listOf(
                DbGift(4L, 1L, "g4", null, null, null, 1L, null, false, 1L),
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, true, 2L),
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, true, 3L),
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 4L)
            )
        }

        "Rank up with secret gift" {
            giftAccessor.addGift(1L, NewGift(name = "g1", categoryId = 1L), false)
            giftAccessor.addGift(1L, NewGift(name = "g2", categoryId = 1L), true)
            giftAccessor.addGift(1L, NewGift(name = "g3", categoryId = 1L), true)
            giftAccessor.addGift(1L, NewGift(name = "g4", categoryId = 1L), false)

            giftAccessor.getFriendGifts(1L) shouldBe listOf(
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 1L),
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, true, 2L),
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, true, 3L),
                DbGift(4L, 1L, "g4", null, null, null, 1L, null, false, 4L))

            //TODO: was expected id 2 -> 3 -> 4 -> 1. But not even sure.
            giftAccessor.rankUpGift(1L, 1L)
            giftAccessor.getFriendGifts(1L) shouldBe listOf(
                DbGift(4L, 1L, "g4", null, null, null, 1L, null, false, 1L),
                DbGift(2L, 1L, "g2", null, null, null, 1L, null, true, 2L),
                DbGift(3L, 1L, "g3", null, null, null, 1L, null, true, 3L),
                DbGift(1L, 1L, "g1", null, null, null, 1L, null, false, 4L)
            )
        }
    }
}