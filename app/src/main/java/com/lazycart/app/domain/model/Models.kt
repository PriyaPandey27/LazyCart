package com.lazycart.app.domain.model

import java.time.LocalDate

/**
 * Category enum for pantry items.
 * Keeping this as a sealed set makes filtering and icon-mapping type-safe.
 */
enum class ItemCategory(val displayName: String) {
    DAIRY("Dairy"),
    GRAINS("Grains & Cereals"),
    PRODUCE("Fruits & Vegetables"),
    PROTEIN("Proteins"),
    BEVERAGES("Beverages"),
    CONDIMENTS("Oils & Condiments"),
    SNACKS("Snacks"),
    HOUSEHOLD("Household"),
    OTHER("Other")
}

/**
 * Unit of measurement for quantities.
 */
enum class ItemUnit(val displayName: String, val abbreviation: String) {
    LITRES("Litres", "L"),
    MILLILITRES("Millilitres", "mL"),
    KILOGRAMS("Kilograms", "kg"),
    GRAMS("Grams", "g"),
    PIECES("Pieces", "pcs"),
    DOZENS("Dozens", "doz"),
    PACKETS("Packets", "pkts"),
    BOTTLES("Bottles", "btls"),
    CANS("Cans", "cans"),
    BAGS("Bags", "bags")
}

/**
 * Confidence level for a restock prediction.
 * Honest communication about how much data backs the estimate.
 */
enum class PredictionConfidence {
    /** Not enough consumption events to produce a meaningful estimate. */
    INSUFFICIENT_DATA,
    /** 2–3 consumption events — estimate exists but could be off. */
    LOW,
    /** 4–6 events — reasonably reliable for planning purposes. */
    MEDIUM,
    /** 7+ events — most reliable estimate within the rolling window. */
    HIGH
}

/**
 * The result of the restock prediction for a single pantry item.
 *
 * @param daysRemaining Estimated days until the item runs out. Null when confidence is INSUFFICIENT_DATA.
 * @param estimatedRestockDate Calculated date to buy more. Null when we can't predict.
 * @param averageDailyConsumption The computed daily rate (same unit as quantity). Null if not calculated.
 * @param confidence How much we trust this prediction.
 * @param explanation Plain-language sentence shown to the user below the prediction.
 * @param observationWindowDays How many days of history were considered.
 */
data class RestockPrediction(
    val daysRemaining: Float?,
    val estimatedRestockDate: LocalDate?,
    val averageDailyConsumption: Float?,
    val confidence: PredictionConfidence,
    val explanation: String,
    val observationWindowDays: Int
)

/**
 * Domain model for a pantry item.
 *
 * This is the model the UI and ViewModels work with.
 * Room entities are mapped to/from this via mapper functions in the repository layer.
 */
data class PantryItem(
    val id: Long = 0L,
    val name: String,
    val category: ItemCategory,
    val quantity: Float,
    val unit: ItemUnit,
    val purchaseDate: LocalDate,
    val expiryDate: LocalDate?,
    val isFinished: Boolean = false,
    /** Optional price hint for shopping list budget estimation. 0 means unknown. */
    val priceHint: Float = 0f,
    /** Populated by the prediction engine — not stored in Room directly. */
    val prediction: RestockPrediction? = null
) {
    /** True if the item has an expiry date within the next [withinDays] days. */
    fun isExpiringSoon(withinDays: Int = 3): Boolean {
        val expiry = expiryDate ?: return false
        val today = LocalDate.now()
        return !expiry.isBefore(today) && !expiry.isAfter(today.plusDays(withinDays.toLong()))
    }

    /** True if the item is already past its expiry date. */
    fun isExpired(): Boolean = expiryDate?.isBefore(LocalDate.now()) == true

    /** True if the prediction suggests restocking within [withinDays] days. */
    fun isRestockingSoon(withinDays: Int = 3): Boolean {
        val days = prediction?.daysRemaining ?: return false
        return days in 0f..withinDays.toFloat()
    }
}

/**
 * Domain model for a single consumption event.
 */
data class ConsumptionEvent(
    val id: Long = 0L,
    val pantryItemId: Long,
    val quantityConsumed: Float,
    val unit: ItemUnit,
    val timestamp: java.time.Instant = java.time.Instant.now(),
    val note: String = ""
)

/**
 * Domain model for a shopping list item.
 * Can be linked to a pantry item (via [pantryItemId]) or manually added (null).
 */
data class ShoppingListItem(
    val id: Long = 0L,
    val pantryItemId: Long?,
    val name: String,
    val quantity: Float,
    val unit: ItemUnit,
    val isPurchased: Boolean = false,
    /** Price per unit the user has set, or the pantry item's price hint. 0 = unknown. */
    val priceHint: Float = 0f,
    val category: ItemCategory = ItemCategory.OTHER
) {
    val estimatedCost: Float get() = if (priceHint > 0f) priceHint * quantity else 0f
}
