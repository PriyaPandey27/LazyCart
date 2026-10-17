package com.lazycart.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database — single source of truth for all persistent app data.
 *
 * Version history:
 *   1 — initial schema (pantry_items, consumption_events, shopping_list_items)
 *
 * Use destructive migration during development. Before shipping, write proper migrations.
 */
@Database(
    entities = [
        PantryItemEntity::class,
        ConsumptionEventEntity::class,
        ShoppingListItemEntity::class
    ],
    version = 1,
    exportSchema = false // Set to true and provide a schema directory for production
)
abstract class LazyCartDatabase : RoomDatabase() {
    abstract fun pantryItemDao(): PantryItemDao
    abstract fun consumptionEventDao(): ConsumptionEventDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao
}
