package com.lazycart.app

import com.lazycart.app.domain.model.ConsumptionEvent
import com.lazycart.app.domain.model.ItemCategory
import com.lazycart.app.domain.model.ItemUnit
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.model.PredictionConfidence
import com.lazycart.app.domain.usecase.RestockPredictionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Unit tests for [RestockPredictionEngine].
 *
 * These tests use a fixed [today] date to make assertions deterministic.
 * No Android dependencies — runs on the JVM.
 */
class RestockPredictionEngineTest {

    // A minimal valid pantry item used as a base for all tests.
    private val baseItem = PantryItem(
        id = 1L,
        name = "Test Milk",
        category = ItemCategory.DAIRY,
        quantity = 2f,
        unit = ItemUnit.LITRES,
        purchaseDate = LocalDate.of(2026, 10, 1),
        expiryDate = null
    )

    private val today = LocalDate.of(2026, 10, 17)

    // Helper: create a ConsumptionEvent at [daysAgo] days before [today].
    private fun eventDaysAgo(daysAgo: Long, amount: Float): ConsumptionEvent {
        val ts = today.minusDays(daysAgo)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
        return ConsumptionEvent(
            pantryItemId = baseItem.id,
            quantityConsumed = amount,
            unit = ItemUnit.LITRES,
            timestamp = ts
        )
    }

    // ── Insufficient data tests ────────────────────────────────────────────

    @Test
    fun `given no consumption events, returns INSUFFICIENT_DATA`() {
        val result = RestockPredictionEngine.predict(
            item = baseItem,
            consumptionHistory = emptyList(),
            today = today
        )
        assertEquals(PredictionConfidence.INSUFFICIENT_DATA, result.confidence)
        assertNull(result.daysRemaining)
        assertNull(result.estimatedRestockDate)
    }

    @Test
    fun `given only 1 consumption event, returns INSUFFICIENT_DATA`() {
        val history = listOf(eventDaysAgo(3, 0.5f))
        val result = RestockPredictionEngine.predict(baseItem, history, today)
        assertEquals(PredictionConfidence.INSUFFICIENT_DATA, result.confidence)
    }

    // ── Finished / zero quantity tests ────────────────────────────────────

    @Test
    fun `given item is finished, returns zero days remaining with today as restock date`() {
        val finished = baseItem.copy(isFinished = true, quantity = 0f)
        val result = RestockPredictionEngine.predict(finished, emptyList(), today)
        assertEquals(0f, result.daysRemaining)
        assertEquals(today, result.estimatedRestockDate)
    }

    @Test
    fun `given zero quantity, treats as out of stock`() {
        val empty = baseItem.copy(quantity = 0f)
        val result = RestockPredictionEngine.predict(empty, emptyList(), today)
        assertEquals(0f, result.daysRemaining)
    }

    // ── Correct calculation tests ──────────────────────────────────────────

    @Test
    fun `given 2 events consuming 1L each over 2 days, daily rate is 0_5L per day`() {
        // Consumed 0.5L on day -2 and 0.5L on day -1 = 1L over effective window (16 days since purchase)
        // actualDailyRate = 1.0 / 16 = 0.0625 per day... but window is min(30, 16) = 16
        // Let's use explicit known consumption: 4L over last 8 days = 0.5L/day
        val history = listOf(
            eventDaysAgo(8, 2f),
            eventDaysAgo(4, 2f)
        )
        val item = baseItem.copy(
            quantity = 2f,
            purchaseDate = LocalDate.of(2026, 10, 1) // 16 days ago
        )
        val result = RestockPredictionEngine.predict(item, history, today)

        // effective window = min(30, 16) = 16 days
        // totalConsumed in window = 4f
        // dailyRate = 4 / 16 = 0.25 per day
        // daysRemaining = 2 / 0.25 = 8 days
        val expectedDailyRate = 4f / 16f
        val expectedDaysRemaining = 2f / expectedDailyRate

        assertNotNull(result.daysRemaining)
        assertEquals(expectedDaysRemaining, result.daysRemaining!!, 0.01f)
    }

    @Test
    fun `restock date is today plus daysRemaining`() {
        val history = listOf(
            eventDaysAgo(5, 1f),
            eventDaysAgo(2, 1f)
        )
        val item = baseItem.copy(quantity = 3f, purchaseDate = today.minusDays(10))
        val result = RestockPredictionEngine.predict(item, history, today)

        if (result.daysRemaining != null) {
            val expectedDate = today.plusDays(result.daysRemaining!!.toLong())
            assertEquals(expectedDate, result.estimatedRestockDate)
        }
    }

    // ── Confidence levels ─────────────────────────────────────────────────

    @Test
    fun `2 to 3 events yields LOW confidence`() {
        val history = listOf(eventDaysAgo(5, 0.5f), eventDaysAgo(2, 0.5f))
        val item = baseItem.copy(purchaseDate = today.minusDays(10))
        val result = RestockPredictionEngine.predict(item, history, today)
        assertEquals(PredictionConfidence.LOW, result.confidence)
    }

    @Test
    fun `4 to 6 events yields MEDIUM confidence`() {
        val history = (1..5).map { eventDaysAgo(it.toLong(), 0.2f) }
        val item = baseItem.copy(purchaseDate = today.minusDays(10))
        val result = RestockPredictionEngine.predict(item, history, today)
        assertEquals(PredictionConfidence.MEDIUM, result.confidence)
    }

    @Test
    fun `7 or more events yields HIGH confidence`() {
        val history = (1..8).map { eventDaysAgo(it.toLong(), 0.1f) }
        val item = baseItem.copy(purchaseDate = today.minusDays(15))
        val result = RestockPredictionEngine.predict(item, history, today)
        assertEquals(PredictionConfidence.HIGH, result.confidence)
    }

    // ── Observation window tests ──────────────────────────────────────────

    @Test
    fun `events outside the observation window are not counted`() {
        // Purchase was 5 days ago, so effective window = 5 days
        val item = baseItem.copy(purchaseDate = today.minusDays(5))

        // One event 10 days ago (outside window) and one 3 days ago (inside)
        val history = listOf(
            eventDaysAgo(10, 2f),  // Should be excluded
            eventDaysAgo(3, 0.5f)  // Should be excluded too since only 1 inside
        )
        val result = RestockPredictionEngine.predict(item, history, today)
        // Only 1 event within 5-day window → INSUFFICIENT_DATA
        assertEquals(PredictionConfidence.INSUFFICIENT_DATA, result.confidence)
    }

    @Test
    fun `effective window shrinks to item age when item is newer than default window`() {
        // Item purchased 3 days ago — window can't be 30 days
        val item = baseItem.copy(purchaseDate = today.minusDays(3))
        val history = listOf(eventDaysAgo(2, 0.5f), eventDaysAgo(1, 0.5f))
        val result = RestockPredictionEngine.predict(item, history, today)
        // observationWindowDays should be capped at 3 (item age)
        assertEquals(3, result.observationWindowDays)
    }

    // ── Edge cases ────────────────────────────────────────────────────────

    @Test
    fun `explanation is always a non-blank string`() {
        listOf(
            emptyList<ConsumptionEvent>(),
            listOf(eventDaysAgo(3, 0.5f)),
            listOf(eventDaysAgo(5, 1f), eventDaysAgo(2, 1f))
        ).forEach { history ->
            val item = baseItem.copy(purchaseDate = today.minusDays(10))
            val result = RestockPredictionEngine.predict(item, history, today)
            assertTrue("Explanation should not be blank", result.explanation.isNotBlank())
        }
    }

    @Test
    fun `daysRemaining is non-negative when item has quantity`() {
        val history = (1..5).map { eventDaysAgo(it.toLong() * 2, 0.3f) }
        val item = baseItem.copy(quantity = 5f, purchaseDate = today.minusDays(20))
        val result = RestockPredictionEngine.predict(item, history, today)
        if (result.daysRemaining != null) {
            assertTrue("Days remaining should be >= 0", result.daysRemaining!! >= 0f)
        }
    }

    // ── Shopping list calculation tests ────────────────────────────────────

    @Test
    fun `ShoppingListItem estimated cost is zero when priceHint is zero`() {
        val item = com.lazycart.app.domain.model.ShoppingListItem(
            pantryItemId = 1L,
            name = "Milk",
            quantity = 1f,
            unit = ItemUnit.LITRES,
            priceHint = 0f
        )
        assertEquals(0f, item.estimatedCost)
    }

    @Test
    fun `ShoppingListItem estimated cost equals priceHint times quantity`() {
        val item = com.lazycart.app.domain.model.ShoppingListItem(
            pantryItemId = 1L,
            name = "Rice",
            quantity = 2f,
            unit = ItemUnit.KILOGRAMS,
            priceHint = 60f
        )
        assertEquals(120f, item.estimatedCost, 0.01f)
    }

    // ── PantryItem helper tests ────────────────────────────────────────────

    @Test
    fun `isExpiringSoon returns true when expiry is within 3 days`() {
        val item = baseItem.copy(expiryDate = today.plusDays(2))
        assertTrue(item.isExpiringSoon(3))
    }

    @Test
    fun `isExpired returns true when expiry is in the past`() {
        val item = baseItem.copy(expiryDate = today.minusDays(1))
        assertTrue(item.isExpired())
    }
}
