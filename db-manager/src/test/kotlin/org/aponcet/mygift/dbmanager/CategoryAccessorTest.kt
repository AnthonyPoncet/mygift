package org.aponcet.mygift.dbmanager

import io.kotlintest.Description
import io.kotlintest.TestCaseOrder
import io.kotlintest.TestResult
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlin.test.assertFailsWith

class CategoryAccessorTest : StringSpec(){
    private val conn = DbConnection("h2", "mem:test")
    private val categoryAccessor = CategoryAccessor(conn)

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
        val usersAccessor = UsersAccessor(conn)
        usersAccessor.createIfNotExists()
        categoryAccessor.createIfNotExists()
        deleteTable(listOf(categoryAccessor.getTableName(), usersAccessor.getTableName())) //order matter due to foreign key

        usersAccessor.addUser("name1", "pwd".toByteArray(), "azerty".toByteArray(), "")
        usersAccessor.addUser("name2", "pwd".toByteArray(), "otherSalt".toByteArray(), "")
    }

    override fun afterTest(description: Description, result: TestResult) {
        val usersAccessor = UsersAccessor(conn)
        deleteTable(listOf(categoryAccessor.getTableName(), usersAccessor.getTableName())) //order matter due to foreign key
    }

    override fun testCaseOrder(): TestCaseOrder? {
        return TestCaseOrder.Random
    }

    init {
        "Add one category ranked one." {
            categoryAccessor.addCategory(1, NewCategory("cat"))

            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, 1L, "cat", 1L))
            categoryAccessor.getUserCategories(2) shouldBe emptyList()
            categoryAccessor.getFriendCategories(1) shouldBe listOf(DbCategory(1L, 1L, "cat", 1L))
            categoryAccessor.getFriendCategories(2) shouldBe emptyList()
        }

        "Add one category unknown user throw." {
            assertFailsWith(DbException::class) {
                categoryAccessor.addCategory(3, NewCategory("cat"))
            }
        }

        "Add two categories same user. Ranked 1 & 2" {
            categoryAccessor.addCategory(1, NewCategory("cat1"))
            categoryAccessor.addCategory(1, NewCategory("cat2"))

            categoryAccessor.getUserCategories(1) shouldBe
                    listOf(DbCategory(1L, 1L, "cat1", 1L),
                        DbCategory(2L, 1L, "cat2", 2L))
            categoryAccessor.getUserCategories(2) shouldBe emptyList()
            categoryAccessor.getFriendCategories(1) shouldBe
                    listOf(DbCategory(1L, 1L, "cat1", 1L),
                        DbCategory(2L, 1L, "cat2", 2L))
            categoryAccessor.getFriendCategories(2) shouldBe emptyList()
        }

        //TODO: should throw ?
        "Add two categories same name same user. No throw" {
            categoryAccessor.addCategory(1, NewCategory("cat"))
            categoryAccessor.addCategory(1, NewCategory("cat"))

            categoryAccessor.getUserCategories(1) shouldBe
                    listOf(DbCategory(1L, 1L, "cat", 1L),
                        DbCategory(2L, 1L, "cat", 2L))
            categoryAccessor.getUserCategories(2) shouldBe emptyList()
            categoryAccessor.getFriendCategories(1) shouldBe
                    listOf(DbCategory(1L, 1L, "cat", 1L),
                        DbCategory(2L, 1L, "cat", 2L))
            categoryAccessor.getFriendCategories(2) shouldBe emptyList()
        }

        "Add two categories same name but different user. Both ranked 1" {
            categoryAccessor.addCategory(1, NewCategory("cat"))
            categoryAccessor.addCategory(2, NewCategory("cat"))

            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, 1L, "cat", 1L))
            categoryAccessor.getUserCategories(2) shouldBe listOf(DbCategory(2L, 2L, "cat", 1L))
            categoryAccessor.getFriendCategories(1) shouldBe listOf(DbCategory(1L, 1L, "cat", 1L))
            categoryAccessor.getFriendCategories(2) shouldBe listOf(DbCategory(2L, 2L, "cat", 1L))
        }

        "Modify a category" {
            categoryAccessor.addCategory(1, NewCategory("cat"))
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, 1L, "cat", 1L))

            categoryAccessor.modifyCategory(1L, Category("other", 2L))
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, 1L, "other", 2L))
        }

        "Remove a category" {
            categoryAccessor.addCategory(1, NewCategory("cat"))
            categoryAccessor.getUserCategories(1) shouldBe listOf(DbCategory(1L, 1L, "cat", 1L))

            categoryAccessor.removeCategory(1L)
            categoryAccessor.getUserCategories(1) shouldBe emptyList()
        }

        "Category exists" {
            categoryAccessor.addCategory(1, NewCategory("cat"))

            categoryAccessor.categoryExists(1L) shouldBe true
            categoryAccessor.categoryExists(2L) shouldBe false
        }

        "Category belong to users" {
            categoryAccessor.addCategory(1, NewCategory("cat"))
            categoryAccessor.addCategory(1, NewCategory("cat2"))

            categoryAccessor.categoryBelongToUser(1L, 1L) shouldBe true
            categoryAccessor.categoryBelongToUser(1L, 2L) shouldBe true
            categoryAccessor.categoryBelongToUser(1L, 3L) shouldBe false
            categoryAccessor.categoryBelongToUser(2L, 1L) shouldBe false
        }

        "Rank down category" {
            categoryAccessor.addCategory(1, NewCategory("cat1"))
            categoryAccessor.addCategory(1, NewCategory("cat2"))
            categoryAccessor.addCategory(1, NewCategory("cat3"))

            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, 1L, "cat1", 1L),
                DbCategory(2L, 1L, "cat2", 2L),
                DbCategory(3L, 1L, "cat3", 3L))

            categoryAccessor.rankDownCategory(1L, 2L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(2L, 1L, "cat2", 1L),
                DbCategory(1L, 1L, "cat1", 2L),
                DbCategory(3L, 1L, "cat3", 3L))

            categoryAccessor.rankDownCategory(1L, 3L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(2L, 1L, "cat2", 1L),
                DbCategory(3L, 1L, "cat3", 2L),
                DbCategory(1L, 1L, "cat1", 3L))

            categoryAccessor.rankDownCategory(1L, 3L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(3L, 1L, "cat3", 1L),
                DbCategory(2L, 1L, "cat2", 2L),
                DbCategory(1L, 1L, "cat1", 3L))

            assertFailsWith(Exception::class) {
                categoryAccessor.rankDownCategory(1L, 3L)
            }
        }

        "Rank up category" {
            categoryAccessor.addCategory(1, NewCategory("cat1"))
            categoryAccessor.addCategory(1, NewCategory("cat2"))
            categoryAccessor.addCategory(1, NewCategory("cat3"))

            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, 1L, "cat1", 1L),
                DbCategory(2L, 1L, "cat2", 2L),
                DbCategory(3L, 1L, "cat3", 3L))

            categoryAccessor.rankUpCategory(1L, 2L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(1L, 1L, "cat1", 1L),
                DbCategory(3L, 1L, "cat3", 2L),
                DbCategory(2L, 1L, "cat2", 3L))

            categoryAccessor.rankUpCategory(1L, 1L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(3L, 1L, "cat3", 1L),
                DbCategory(1L, 1L, "cat1", 2L),
                DbCategory(2L, 1L, "cat2", 3L))

            categoryAccessor.rankUpCategory(1L, 1L)
            categoryAccessor.getUserCategories(1) shouldBe listOf(
                DbCategory(3L, 1L, "cat3", 1L),
                DbCategory(2L, 1L, "cat2", 2L),
                DbCategory(1L, 1L, "cat1", 3L))

            assertFailsWith(Exception::class) {
                categoryAccessor.rankUpCategory(1L, 1L)
            }
        }
    }
}

