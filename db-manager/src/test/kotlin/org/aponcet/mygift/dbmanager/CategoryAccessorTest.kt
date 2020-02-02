package org.aponcet.mygift.dbmanager

import io.kotlintest.Description
import io.kotlintest.TestCaseOrder
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

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

        usersAccessor.addUser("name1", "pwd", "")
        usersAccessor.addUser("name2", "pwd", "")
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
    }
}

