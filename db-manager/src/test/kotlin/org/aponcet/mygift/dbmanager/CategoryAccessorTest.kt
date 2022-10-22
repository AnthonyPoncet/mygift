package org.aponcet.mygift.dbmanager

import io.kotlintest.Description
import io.kotlintest.TestCaseOrder
import io.kotlintest.TestResult
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlin.test.assertFailsWith

class CategoryAccessorTest : StringSpec() {
    private val conn = DbConnection("h2", "mem:test")
    private val categoryAccessor = CategoryAccessor(conn)

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

        usersAccessor.addUser("name1", "pwd".toByteArray(), "azerty".toByteArray(), "")
        usersAccessor.addUser("name2", "pwd".toByteArray(), "otherSalt".toByteArray(), "")
    }

    override fun afterTest(description: Description, result: TestResult) {
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
            categoryAccessor.addCategory(NewCategory("cat"), listOf(1))

            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "cat", 1L))
            categoryAccessor.getUserCategories(2) shouldBe emptyList()
            categoryAccessor.getFriendCategories(2, 1) shouldBe listOf(DbCategory(1L, "cat", 1L))
            categoryAccessor.getFriendCategories(1, 2) shouldBe emptyList()
        }

        "Add one category two users" {
            categoryAccessor.addCategory(NewCategory("cat"), listOf(1, 2))
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "cat", 1L))
            categoryAccessor.getUserCategories(2) shouldBe listOf(DbCategory(1L, "cat", 1L))
            categoryAccessor.getFriendCategories(3, 1) shouldBe listOf(DbCategory(1L, "cat", 1L))
            categoryAccessor.getFriendCategories(3, 2) shouldBe listOf(DbCategory(1L, "cat", 1L))
            categoryAccessor.getFriendCategories(2, 1) shouldBe emptyList()
            categoryAccessor.getFriendCategories(1, 2) shouldBe emptyList()
        }

        "Add one category user 1, another user 2, another one, another user 1&2 then another user 2" {
            categoryAccessor.addCategory(NewCategory("one"), listOf(1))
            categoryAccessor.addCategory(NewCategory("two"), listOf(2))
            categoryAccessor.addCategory(NewCategory("one bis"), listOf(1))
            categoryAccessor.addCategory(NewCategory("three"), listOf(1, 2))
            categoryAccessor.addCategory(NewCategory("four"), listOf(2))

            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "one", 1L),
                DbCategory(3L, "one bis", 2L),
                DbCategory(4L, "three", 3L)
            )
            categoryAccessor.getUserCategories(2) shouldBe listOf(
                DbCategory(2L, "two", 1L),
                DbCategory(4L, "three", 2L),
                DbCategory(5L, "four", 3L)
            )

            categoryAccessor.getFriendCategories(1, 2) shouldBe listOf(
                DbCategory(2L, "two", 1L),
                DbCategory(5L, "four", 3L)
            )

            categoryAccessor.rankUpCategory(2L, 4L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "one", 1L),
                DbCategory(3L, "one bis", 2L),
                DbCategory(4L, "three", 3L)
            )
            categoryAccessor.getUserCategories(2) shouldBe listOf(
                DbCategory(2L, "two", 1L),
                DbCategory(5L, "four", 2L),
                DbCategory(4L, "three", 3L)
            )

        }

        "Add one category unknown user throw." {
            assertFailsWith(DbException::class) {
                categoryAccessor.addCategory(NewCategory("cat"), listOf(3))
            }
        }

        "Add two categories same user. Ranked 1 & 2" {
            categoryAccessor.addCategory(NewCategory("cat1"), listOf(1))
            categoryAccessor.addCategory(NewCategory("cat2"), listOf(1))

            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "cat1", 1L),
                DbCategory(2L, "cat2", 2L)
            )
            categoryAccessor.getUserCategories(2) shouldBe emptyList()
            categoryAccessor.getFriendCategories(2, 1) shouldBe listOf(
                DbCategory(1L, "cat1", 1L),
                DbCategory(2L, "cat2", 2L)
            )
            categoryAccessor.getFriendCategories(1, 2) shouldBe emptyList()
        }

        "Add two categories same name but different user. Both ranked 1" {
            categoryAccessor.addCategory(NewCategory("cat"), listOf(1))
            categoryAccessor.addCategory(NewCategory("cat"), listOf(2))

            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "cat", 1L))
            categoryAccessor.getUserCategories(2) shouldBe listOf(DbCategory(2L, "cat", 1L))
            categoryAccessor.getFriendCategories(2, 1) shouldBe listOf(DbCategory(1L, "cat", 1L))
            categoryAccessor.getFriendCategories(1, 2) shouldBe listOf(DbCategory(2L, "cat", 1L))
        }

        "Modify a category" {
            categoryAccessor.addCategory(NewCategory("cat"), listOf(1))
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "cat", 1L))

            categoryAccessor.modifyCategory(1L, 1L, Category("other", 2L))
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "other", 2L))
        }

        "Remove a category" {
            categoryAccessor.addCategory(NewCategory("cat"), listOf(1))
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, "cat", 1L))

            categoryAccessor.removeCategory(1L)
            categoryAccessor.getUserCategories(1) shouldBe emptyList()
        }

        "Category exists" {
            categoryAccessor.addCategory(NewCategory("cat"), listOf(1))

            categoryAccessor.categoryExists(1L) shouldBe true
            categoryAccessor.categoryExists(2L) shouldBe false
        }

        "Category belong to users" {
            categoryAccessor.addCategory(NewCategory("cat"), listOf(1))
            categoryAccessor.addCategory(NewCategory("cat2"), listOf(1))

            categoryAccessor.categoryBelongToUser(1L, 1L) shouldBe true
            categoryAccessor.categoryBelongToUser(1L, 2L) shouldBe true
            categoryAccessor.categoryBelongToUser(1L, 3L) shouldBe false
            categoryAccessor.categoryBelongToUser(2L, 1L) shouldBe false
        }

        "Rank down category" {
            categoryAccessor.addCategory(NewCategory("cat1"), listOf(1))
            categoryAccessor.addCategory(NewCategory("cat2"), listOf(1))
            categoryAccessor.addCategory(NewCategory("cat3"), listOf(1))

            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "cat1", 1L),
                DbCategory(2L, "cat2", 2L),
                DbCategory(3L, "cat3", 3L)
            )

            categoryAccessor.rankDownCategory(1L, 2L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(2L, "cat2", 1L),
                DbCategory(1L, "cat1", 2L),
                DbCategory(3L, "cat3", 3L)
            )

            categoryAccessor.rankDownCategory(1L, 3L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(2L, "cat2", 1L),
                DbCategory(3L, "cat3", 2L),
                DbCategory(1L, "cat1", 3L)
            )

            categoryAccessor.rankDownCategory(1L, 3L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(3L, "cat3", 1L),
                DbCategory(2L, "cat2", 2L),
                DbCategory(1L, "cat1", 3L)
            )

            assertFailsWith(Exception::class) {
                categoryAccessor.rankDownCategory(1L, 3L)
            }
        }

        "Rank up category" {
            categoryAccessor.addCategory(NewCategory("cat1"), listOf(1))
            categoryAccessor.addCategory(NewCategory("cat2"), listOf(1))
            categoryAccessor.addCategory(NewCategory("cat3"), listOf(1))

            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "cat1", 1L),
                DbCategory(2L, "cat2", 2L),
                DbCategory(3L, "cat3", 3L)
            )

            categoryAccessor.rankUpCategory(1L, 2L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, "cat1", 1L),
                DbCategory(3L, "cat3", 2L),
                DbCategory(2L, "cat2", 3L)
            )

            categoryAccessor.rankUpCategory(1L, 1L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(3L, "cat3", 1L),
                DbCategory(1L, "cat1", 2L),
                DbCategory(2L, "cat2", 3L)
            )

            categoryAccessor.rankUpCategory(1L, 1L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(3L, "cat3", 1L),
                DbCategory(2L, "cat2", 2L),
                DbCategory(1L, "cat1", 3L)
            )

            assertFailsWith(Exception::class) {
                categoryAccessor.rankUpCategory(1L, 1L)
            }
        }
    }
}

