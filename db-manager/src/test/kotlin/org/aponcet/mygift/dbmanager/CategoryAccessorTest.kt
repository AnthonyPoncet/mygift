package org.aponcet.mygift.dbmanager

import io.kotlintest.*
import io.kotlintest.specs.StringSpec
import kotlin.test.assertFailsWith

class CategoryAccessorTest : StringSpec() {
    private val conn = DbConnection("h2", "mem:test")
    private val categoryAccessor = CategoryAccessor(conn)

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
        val joinUserAndCategoryAccessor = JoinUserAndCategoryAccessor(conn)
        usersAccessor.createIfNotExists()
        categoryAccessor.createIfNotExists()
        joinUserAndCategoryAccessor.createIfNotExists()
        conn.safeExecute(
            "DELETE FROM ${JoinUserAndCategoryAccessor(conn).getTableName()}",
            { it.executeUpdate() },
            "Could not clean ${JoinUserAndCategoryAccessor(conn).getTableName()}"
        )
        deleteTable(
            listOf(
                categoryAccessor.getTableName(),
                usersAccessor.getTableName()
            )
        ) //order matter due to foreign key

        usersAccessor.addUser("name1", "pwd".toByteArray(), "azerty".toByteArray(), "", null)
        usersAccessor.addUser("name2", "pwd".toByteArray(), "otherSalt".toByteArray(), "", null)
    }

    override fun afterTest(testCase: TestCase, result: TestResult) {
        val usersAccessor = UsersAccessor(conn)
        conn.safeExecute(
            "DELETE FROM ${JoinUserAndCategoryAccessor(conn).getTableName()}",
            { it.executeUpdate() },
            "Could not clean ${JoinUserAndCategoryAccessor(conn).getTableName()}"
        )
        deleteTable(
            listOf(
                categoryAccessor.getTableName(),
                usersAccessor.getTableName()
            )
        ) //order matter due to foreign key
    }

    override fun testCaseOrder(): TestCaseOrder {
        return TestCaseOrder.Random
    }

    init {
        "Add one category ranked one." {
            categoryAccessor.addCategory("cat", listOf(1))

            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "cat", 1L, setOf()))
            categoryAccessor.getUserCategories(2) shouldBe emptyList()
            categoryAccessor.getFriendCategories(2, 1) shouldBe listOf(DbCategory(1L, "cat", 1L, setOf()))
            categoryAccessor.getFriendCategories(1, 2) shouldBe emptyList()
        }

        "Add one category two users" {
            categoryAccessor.addCategory("cat", listOf(1, 2))
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "cat", 1L, setOf(2)))
            categoryAccessor.getUserCategories(2) shouldBe listOf(DbCategory(1L, "cat", 1L, setOf(1)))
            categoryAccessor.getFriendCategories(3, 1) shouldBe listOf(DbCategory(1L, "cat", 1L, setOf()))
            categoryAccessor.getFriendCategories(3, 2) shouldBe listOf(DbCategory(1L, "cat", 1L, setOf()))
            categoryAccessor.getFriendCategories(2, 1) shouldBe emptyList()
            categoryAccessor.getFriendCategories(1, 2) shouldBe emptyList()
        }

        "Add one category user 1, another user 2, another one, another user 1&2 then another user 2" {
            categoryAccessor.addCategory("one", listOf(1))
            categoryAccessor.addCategory("two", listOf(2))
            categoryAccessor.addCategory("one bis", listOf(1))
            categoryAccessor.addCategory("three", listOf(1, 2))
            categoryAccessor.addCategory("four", listOf(2))

            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "one", 1L, setOf()),
                DbCategory(3L, "one bis", 2L, setOf()),
                DbCategory(4L, "three", 3L, setOf(2))
            )
            categoryAccessor.getUserCategories(2) shouldBe listOf(
                DbCategory(2L, "two", 1L, setOf()),
                DbCategory(4L, "three", 2L, setOf(1)),
                DbCategory(5L, "four", 3L, setOf())
            )

            categoryAccessor.getFriendCategories(1, 2) shouldBe listOf(
                DbCategory(2L, "two", 1L, setOf()),
                DbCategory(5L, "four", 3L, setOf())
            )

            categoryAccessor.rankUpCategory(2L, 4L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "one", 1L, setOf()),
                DbCategory(3L, "one bis", 2L, setOf()),
                DbCategory(4L, "three", 3L, setOf(2))
            )
            categoryAccessor.getUserCategories(2) shouldBe listOf(
                DbCategory(2L, "two", 1L, setOf()),
                DbCategory(5L, "four", 2L, setOf()),
                DbCategory(4L, "three", 3L, setOf(1))
            )
        }

        "Add one category unknown user throw." {
            assertFailsWith(DbException::class) {
                categoryAccessor.addCategory("cat", listOf(3))
            }
        }

        "Add two categories same user. Ranked 1 & 2" {
            categoryAccessor.addCategory("cat1", listOf(1))
            categoryAccessor.addCategory("cat2", listOf(1))

            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "cat1", 1L, setOf()),
                DbCategory(2L, "cat2", 2L, setOf())
            )
            categoryAccessor.getUserCategories(2) shouldBe emptyList()
            categoryAccessor.getFriendCategories(2, 1) shouldBe listOf(
                DbCategory(1L, "cat1", 1L, setOf()),
                DbCategory(2L, "cat2", 2L, setOf())
            )
            categoryAccessor.getFriendCategories(1, 2) shouldBe emptyList()
        }

        "Add two categories same name but different user. Both ranked 1" {
            categoryAccessor.addCategory("cat", listOf(1))
            categoryAccessor.addCategory("cat", listOf(2))

            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "cat", 1L, setOf()))
            categoryAccessor.getUserCategories(2) shouldBe listOf(DbCategory(2L, "cat", 1L, setOf()))
            categoryAccessor.getFriendCategories(2, 1) shouldBe listOf(DbCategory(1L, "cat", 1L, setOf()))
            categoryAccessor.getFriendCategories(1, 2) shouldBe listOf(DbCategory(2L, "cat", 1L, setOf()))
        }

        "Modify a category" {
            categoryAccessor.addCategory("cat", listOf(1))
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "cat", 1L, setOf()))

            categoryAccessor.modifyCategory(1L, "other")
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "other", 1L, setOf()))
        }

        "Remove a category" {
            categoryAccessor.addCategory("cat", listOf(1))
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "cat", 1L, setOf()))

            categoryAccessor.removeCategory(1L)
            categoryAccessor.getUserCategories(1) shouldBe emptyList()
        }

        "Category exists" {
            categoryAccessor.addCategory("cat", listOf(1))

            categoryAccessor.categoryExists(1L) shouldBe true
            categoryAccessor.categoryExists(2L) shouldBe false
        }

        "Category belong to users" {
            categoryAccessor.addCategory("cat", listOf(1))
            categoryAccessor.addCategory("cat2", listOf(1))

            categoryAccessor.categoryBelongToUser(1L, 1L) shouldBe true
            categoryAccessor.categoryBelongToUser(1L, 2L) shouldBe true
            categoryAccessor.categoryBelongToUser(1L, 3L) shouldBe false
            categoryAccessor.categoryBelongToUser(2L, 1L) shouldBe false
        }

        "Rank down category" {
            categoryAccessor.addCategory("cat1", listOf(1))
            categoryAccessor.addCategory("cat2", listOf(1))
            categoryAccessor.addCategory("cat3", listOf(1))

            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "cat1", 1L, setOf()),
                DbCategory(2L, "cat2", 2L, setOf()),
                DbCategory(3L, "cat3", 3L, setOf())
            )

            categoryAccessor.rankDownCategory(1L, 2L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(2L, "cat2", 1L, setOf()),
                DbCategory(1L, "cat1", 2L, setOf()),
                DbCategory(3L, "cat3", 3L, setOf())
            )

            categoryAccessor.rankDownCategory(1L, 3L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(2L, "cat2", 1L, setOf()),
                DbCategory(3L, "cat3", 2L, setOf()),
                DbCategory(1L, "cat1", 3L, setOf())
            )

            categoryAccessor.rankDownCategory(1L, 3L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(3L, "cat3", 1L, setOf()),
                DbCategory(2L, "cat2", 2L, setOf()),
                DbCategory(1L, "cat1", 3L, setOf())
            )

            assertFailsWith(Exception::class) {
                categoryAccessor.rankDownCategory(1L, 3L)
            }
        }

        "Rank up category" {
            categoryAccessor.addCategory("cat1", listOf(1))
            categoryAccessor.addCategory("cat2", listOf(1))
            categoryAccessor.addCategory("cat3", listOf(1))

            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "cat1", 1L, setOf()),
                DbCategory(2L, "cat2", 2L, setOf()),
                DbCategory(3L, "cat3", 3L, setOf())
            )

            categoryAccessor.rankUpCategory(1L, 2L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "cat1", 1L, setOf()),
                DbCategory(3L, "cat3", 2L, setOf()),
                DbCategory(2L, "cat2", 3L, setOf())
            )

            categoryAccessor.rankUpCategory(1L, 1L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(3L, "cat3", 1L, setOf()),
                DbCategory(1L, "cat1", 2L, setOf()),
                DbCategory(2L, "cat2", 3L, setOf())
            )

            categoryAccessor.rankUpCategory(1L, 1L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(3L, "cat3", 1L, setOf()),
                DbCategory(2L, "cat2", 2L, setOf()),
                DbCategory(1L, "cat1", 3L, setOf())
            )

            assertFailsWith(Exception::class) {
                categoryAccessor.rankUpCategory(1L, 1L)
            }
        }
    }
}

