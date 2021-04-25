package org.aponcet.mygift.dbmanager

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.sql.SQLException
import kotlin.test.assertFailsWith

class DbConnectionTest : StringSpec() {
    private val conn = DbConnection("h2", "mem:test")

    override fun closeResources() {
        conn.close()
    }

    init {
        conn.execute("CREATE TABLE testTable (id NUMBER)")

        "valid SQL return list" {
            conn.safeExecute("SELECT * FROM testTable", {
                with(it) {
                    return@with listOf(1, 2)
                }
            }, "error").shouldBe(listOf(1, 2))
        }

        "invalid SQL: unknown table throw exception" {
            assertFailsWith(DbException::class) {
                conn.safeExecute("SELECT * FROM notATable", {
                    with(it) {
                        return@with
                    }
                }, "error")
            }
        }

        "invalid SQL: throw during execution throw exception" {
            val conn = DbConnection("h2", "mem:test")
            assertFailsWith(DbException::class) {
                conn.safeExecute("SELECT * FROM testTable", {
                    throw SQLException()
                }, "error")
            }
        }
    }
}
