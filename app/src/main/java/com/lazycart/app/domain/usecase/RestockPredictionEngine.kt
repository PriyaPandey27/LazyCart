package com.lazycart.app.domain.usecase

import com.lazycart.app.domain.model.ConsumptionEvent
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.model.PredictionConfidence
import com.lazycart.app.domain.model.RestockPrediction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Pure prediction engine — no Android dependencies, fully unit-testable.
 *
 * Algorithm:
 *   1. Collect consumption events within a rolling observation window.
 *   2. Sum total quantity consumed in that window.
 *   3. Compute averageDailyRate = totalConsumed / windowDays.
 *   4. estimatedDaysRemaining = currentQuantity / averageDailyRate.
 *   5. estimatedRestockDate = today + estimatedDaysRemaining.
 *
 * The observation window defaults to 30 days. If the item was purchased more recently
 * than 30 days ago, the window shrinks to the item's age so we don't divide by zero.
 *
 * Confidence is based on the number of distinct consumption events:
 *   INSUFFICIENT_DATA  → < 2 events
 *   LOW                → 2–3 events
 *   MEDIUM             → 4–6 events
 *   HIGH               → 7+ events
 */
object RestockPredictionEngine {

    const val DEFAULT_OBSERVATION_WINDOW_DAYS = 30

    /**
     * Build a [RestockPrediction] for [item] given its [consumptionHistory].
     *
     * @param item The pantry item to predict for.
     * @param consumptionHistory All recorded consumption events for this item.
     * @param today Injected so tests can use a fixed date without mocking the system clock.
     * @param observationWindowDays How many days back to look at consumption events.
     */
    fun predict(
        item: PantryItem,
        consumptionHistory: List<ConsumptionEvent>,
        today: LocalDate = LocalDate.now(),
        observationWindowDays: Int = DEFAULT_OBSERVATION_WINDOW_DAYS
    ): RestockPrediction {

        // Edge case: item is already finished or at zero quantity.
        if (item.isFinished || item.quantity <= 0f) {
            return RestockPrediction(
                daysRemaining = 0f,
                estimatedRestockDate = today,
                averageDailyConsumption = null,
                confidence = PredictionConfidence.INSUFFICIENT_DATA,
                explanation = "This item is out of stock.",
                observationWindowDays = observationWindowDays
            )
        }

        // Determine the actual observation window (capped at item age).
        val itemAgeDays = max(
            1L,
            ChronoUnit.DAYS.between(item.purchaseDate, today)
        )
        val effectiveWindowDays = minOf(observationWindowDays.toLong(), itemAgeDays).toInt()

        // Filter events to those within the observation window.
        val windowStart: Instant = today
            .minusDays(effectiveWindowDays.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val windowEvents = consumptionHistory.filter { it.timestamp >= windowStart }

        val eventCount = windowEvents.size
        val confidence = when {
            eventCount < 2 -> PredictionConfidence.INSUFFICIENT_DATA
            eventCount < 4 -> PredictionConfidence.LOW
            eventCount < 7 -> PredictionConfidence.MEDIUM
            else -> PredictionConfidence.HIGH
        }

        // With insufficient data, return an honest "we don't know yet" result.
        if (confidence == PredictionConfidence.INSUFFICIENT_DATA) {
            return RestockPrediction(
                daysRemaining = null,
                estimatedRestockDate = null,
                averageDailyConsumption = null,
                confidence = PredictionConfidence.INSUFFICIENT_DATA,
                explanation = if (eventCount == 0)
                    "Log your first consumption to start seeing predictions."
                else
                    "Log at least 2 consumption events to get a reliable estimate.",
                observationWindowDays = effectiveWindowDays
            )
        }

        // Sum all consumption in the window.
        val totalConsumed = windowEvents.sumOf { it.quantityConsumed.toDouble() }.toFloat()

        // Guard against consuming nothing (item in pantry but never touched).
        if (totalConsumed <= 0f) {
            return RestockPrediction(
                daysRemaining = null,
                estimatedRestockDate = null,
                averageDailyConsumption = 0f,
                confidence = PredictionConfidence.INSUFFICIENT_DATA,
                explanation = "No measurable consumption recorded yet.",
                observationWindowDays = effectiveWindowDays
            )
        }

        val averageDailyRate = totalConsumed / effectiveWindowDays.toFloat()

        // Guard against division by an effectively zero rate.
        if (averageDailyRate < 0.0001f) {
            return RestockPrediction(
                daysRemaining = null,
                estimatedRestockDate = null,
                averageDailyConsumption = averageDailyRate,
                confidence = PredictionConfidence.INSUFFICIENT_DATA,
                explanation = "Your usage rate is too low to estimate a restock date reliably.",
                observationWindowDays = effectiveWindowDays
            )
        }

        val daysRemaining = item.quantity / averageDailyRate
        val restockDate = today.plusDays(daysRemaining.toLong())

        val confidenceLabel = when (confidence) {
            PredictionConfidence.LOW -> "rough"
            PredictionConfidence.MEDIUM -> "reasonable"
            PredictionConfidence.HIGH -> "reliable"
            else -> ""
        }

        val explanation = buildString {
            append("Based on your last $effectiveWindowDays day")
            if (effectiveWindowDays != 1) append("s")
            append(" of usage (")
            append(eventCount)
            append(" log")
            if (eventCount != 1) append("s")
            append("), this is a $confidenceLabel estimate. ")
            append("Actual usage may vary.")
        }

        return RestockPrediction(
            daysRemaining = daysRemaining,
            estimatedRestockDate = restockDate,
            averageDailyConsumption = averageDailyRate,
            confidence = confidence,
            explanation = explanation,
            observationWindowDays = effectiveWindowDays
        )
    }
}
