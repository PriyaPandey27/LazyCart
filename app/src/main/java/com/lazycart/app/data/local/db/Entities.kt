package com.lazycart.app.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a pantry item.
 * Dates stored as epoch days (Long) for simplicity — no serialization complexity.
 */
@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: String,           // Stored as ItemCategory.name
    val quantity: Float,
    val unit: String,               // Stored as ItemUnit.name
    val purchaseDateEpochDay: Long,
    val expiryDateEpochDay: Long?,  // Null if no expiry
    val isFinished: Boolean = false,
    val priceHint: Float = 0f
)

/**
 * Room entity for a consumption event.
 * Linked to a pantry item via foreign key; cascades delete so orphan events are cleaned up.
 */
@Entity(
    tableName = "consumption_events",
    foreignKeys = [ForeignKey(
        entity = PantryItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["pantryItemId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("pantryItemId")]
)
data class ConsumptionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val pantryItemId: Long,
    val quantityConsumed: Float,
    val unit: String,               // Stored as ItemUnit.name
    val timestampEpochSecond: Long, // java.time.Instant.epochSecond
    val note: String = ""
)

/**
 * Room entity for a shopping list item.
 * [pantryItemId] is nullable — manually added items have no linked pantry entry.
 * No foreign key constraint so shopping list survives pantry item deletion.
 */
@Entity(tableName = "shopping_list_items")
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val pantryItemId: Long?,        // Nullable: manually added items have no linked pantry item
    val name: String,
    val quantity: Float,
    val unit: String,               // Stored as ItemUnit.name
    val isPurchased: Boolean = false,
    val priceHint: Float = 0f,
    val category: String = "OTHER"  // Stored as ItemCategory.name
)
