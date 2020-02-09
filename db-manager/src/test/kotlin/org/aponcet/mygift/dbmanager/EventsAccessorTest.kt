package org.aponcet.mygift.dbmanager

import io.kotlintest.Description
import io.kotlintest.TestCaseOrder
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.time.LocalDate
import kotlin.test.assertFailsWith

class EventsAccessorTest: StringSpec() {
    private val conn = DbConnection("h2", "mem:test")
    private val eventsAccessor = EventsAccessor(conn)

    override fun isInstancePerTest(): Boolean {
        return true
    }

    private fun deleteTable(tables: List<String>) {
        for (table in tables) {
            conn.safeExecute("DELETE FROM $table", { it.executeUpdate() }, "Could not clean $table")
            conn.safeExecute("ALTER TABLE $table ALTER COLUMN id RESTART WITH 1", { it.executeUpdate() },"Could not reset sequence id $table"
            )
        }
    }

    override fun beforeTest(description: Description) {
        eventsAccessor.createIfNotExists()
        deleteTable(listOf(eventsAccessor.getTableName()))
    }

    override fun testCaseOrder(): TestCaseOrder? {
        return TestCaseOrder.Random
    }

    init {
        "Insert event" {
            eventsAccessor.insertEvent("name", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)

            val expected = DbEvent(1L, "name", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)

            eventsAccessor.getEventsById(1L) shouldBe expected
            eventsAccessor.getEventsCreateBy(1L) shouldBe listOf(expected)
            eventsAccessor.getEventsNamed("name") shouldBe listOf(expected)
        }

        "Two events same creator" {
            eventsAccessor.insertEvent("name", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)
            eventsAccessor.insertEvent("other", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)

            val expected1 = DbEvent(1L, "name", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)
            val expected2 = DbEvent(2L, "other", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)

            eventsAccessor.getEventsCreateBy(1L) shouldBe listOf(expected1, expected2)
            eventsAccessor.getEventsNamed("name") shouldBe listOf(expected1)
            eventsAccessor.getEventsNamed("other") shouldBe listOf(expected2)
        }

        "Two events same name" {
            eventsAccessor.insertEvent("name", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)
            eventsAccessor.insertEvent("name", 2L, "desc", LocalDate.of(2020, 12, 26), 3L)

            val expected1 = DbEvent(1L, "name", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)
            val expected2 = DbEvent(2L, "name", 2L, "desc", LocalDate.of(2020, 12, 26), 3L)

            eventsAccessor.getEventsCreateBy(1L) shouldBe listOf(expected1)
            eventsAccessor.getEventsCreateBy(2L) shouldBe listOf(expected2)
            eventsAccessor.getEventsNamed("name") shouldBe listOf(expected1, expected2)
        }

        "Unknown id return null" {
            eventsAccessor.getEventsById(1L) shouldBe null
        }

        "Unknown creator id return empty list" {
            eventsAccessor.getEventsCreateBy(1L) shouldBe emptyList()
        }

        "Unknown event name return empty list" {
            eventsAccessor.getEventsNamed("name") shouldBe emptyList()
        }

        "Delete event" {
            eventsAccessor.insertEvent("name", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)

            val expected = DbEvent(1L, "name", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)
            eventsAccessor.getEventsById(1L) shouldBe expected
            eventsAccessor.getEventsCreateBy(1L) shouldBe listOf(expected)
            eventsAccessor.getEventsNamed("name") shouldBe listOf(expected)

            eventsAccessor.deleteEvent(1L)
            eventsAccessor.getEventsById(1L) shouldBe null
            eventsAccessor.getEventsCreateBy(1L) shouldBe emptyList()
            eventsAccessor.getEventsNamed("name") shouldBe emptyList()
        }

        "Delete unknown event throw" {
            assertFailsWith(DbException::class) {
                eventsAccessor.deleteEvent(1L)
            }
        }

        "Event exists" {
            eventsAccessor.insertEvent("name", 1L, "desc", LocalDate.of(2020, 12, 25), 2L)

            eventsAccessor.eventExists(1L) shouldBe true
            eventsAccessor.eventExists(2L) shouldBe false
        }
    }
}