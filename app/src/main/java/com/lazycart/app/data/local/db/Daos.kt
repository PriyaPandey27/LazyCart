package com.lazycart.app.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryItemDao {

    @Query("SELECT * FROM pantry_items WHERE isFinished = 0 ORDER BY name ASC")
    fun observeActivePantryItems(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items ORDER BY name ASC")
    fun observeAllPantryItems(): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    fun observeItemById(id: Long): Flow<PantryItemEntity?>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    suspend fun getItemById(id: Long): PantryItemEntity?

    @Query("SELECT * FROM pantry_items WHERE category = :category AND isFinished = 0 ORDER BY name ASC")
    fun observeByCategory(category: String): Flow<List<PantryItemEntity>>

    @Query("SELECT * FROM pantry_items WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun observeByNameQuery(query: String): Flow<List<PantryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PantryItemEntity): Long

    @Update
    suspend fun updateItem(item: PantryItemEntity)

    @Delete
    suspend fun deleteItem(item: PantryItemEntity)

    @Query("DELETE FROM pantry_items")
    suspend fun deleteAllItems()

    @Query("UPDATE pantry_items SET isFinished = 1 WHERE id = :id")
    suspend fun markAsFinished(id: Long)

    @Query("UPDATE pantry_items SET quantity = :newQuantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, newQuantity: Float)
}

@Dao
interface ConsumptionEventDao {

    @Query("SELECT * FROM consumption_events WHERE pantryItemId = :itemId ORDER BY timestampEpochSecond DESC")
    fun observeEventsForItem(itemId: Long): Flow<List<ConsumptionEventEntity>>

    @Query("""
        SELECT * FROM consumption_events
        WHERE pantryItemId = :itemId
          AND timestampEpochSecond >= :sinceEpochSecond
        ORDER BY timestampEpochSecond DESC
    """)
    suspend fun getEventsForItemSince(itemId: Long, sinceEpochSecond: Long): List<ConsumptionEventEntity>

    @Query("SELECT * FROM consumption_events WHERE pantryItemId = :itemId ORDER BY timestampEpochSecond DESC")
    suspend fun getAllEventsForItem(itemId: Long): List<ConsumptionEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ConsumptionEventEntity): Long

    @Delete
    suspend fun deleteEvent(event: ConsumptionEventEntity)

    @Query("DELETE FROM consumption_events WHERE pantryItemId = :itemId")
    suspend fun deleteEventsForItem(itemId: Long)

    @Query("DELETE FROM consumption_events")
    suspend fun deleteAllEvents()
}

@Dao
interface ShoppingListItemDao {

    @Query("SELECT * FROM shopping_list_items ORDER BY isPurchased ASC, name ASC")
    fun observeAllShoppingItems(): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE isPurchased = 0 ORDER BY name ASC")
    fun observePendingItems(): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE pantryItemId = :pantryItemId LIMIT 1")
    suspend fun getItemByPantryId(pantryItemId: Long): ShoppingListItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingListItemEntity): Long

    @Update
    suspend fun updateItem(item: ShoppingListItemEntity)

    @Delete
    suspend fun deleteItem(item: ShoppingListItemEntity)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE shopping_list_items SET isPurchased = :purchased WHERE id = :id")
    suspend fun setItemPurchased(id: Long, purchased: Boolean)

    @Query("DELETE FROM shopping_list_items WHERE isPurchased = 1")
    suspend fun clearPurchasedItems()

    @Query("DELETE FROM shopping_list_items")
    suspend fun deleteAllItems()
}
