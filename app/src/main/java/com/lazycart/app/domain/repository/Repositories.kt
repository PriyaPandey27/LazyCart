package com.lazycart.app.domain.repository

import com.lazycart.app.domain.model.ConsumptionEvent
import com.lazycart.app.domain.model.ItemCategory
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

interface PantryRepository {
    fun observeActivePantryItems(): Flow<List<PantryItem>>
    fun observeAllPantryItems(): Flow<List<PantryItem>>
    fun observeItemById(id: Long): Flow<PantryItem?>
    fun observeByCategory(category: ItemCategory): Flow<List<PantryItem>>
    fun observeByNameQuery(query: String): Flow<List<PantryItem>>
    suspend fun getItemById(id: Long): PantryItem?
    suspend fun addItem(item: PantryItem): Long
    suspend fun updateItem(item: PantryItem)
    suspend fun deleteItem(item: PantryItem)
    suspend fun markAsFinished(id: Long)
    suspend fun updateQuantity(id: Long, newQuantity: Float)
    suspend fun deleteAllItems()
}

interface ConsumptionRepository {
    fun observeEventsForItem(itemId: Long): Flow<List<ConsumptionEvent>>
    suspend fun getEventsForItemSince(itemId: Long, sinceEpochSecond: Long): List<ConsumptionEvent>
    suspend fun getAllEventsForItem(itemId: Long): List<ConsumptionEvent>
    suspend fun logConsumption(event: ConsumptionEvent)
    suspend fun deleteEvent(event: ConsumptionEvent)
    suspend fun deleteAllEvents()
}

interface ShoppingListRepository {
    fun observeAllShoppingItems(): Flow<List<ShoppingListItem>>
    fun observePendingItems(): Flow<List<ShoppingListItem>>
    suspend fun addItem(item: ShoppingListItem): Long
    suspend fun updateItem(item: ShoppingListItem)
    suspend fun removeItem(id: Long)
    suspend fun setItemPurchased(id: Long, purchased: Boolean)
    suspend fun clearPurchasedItems()
    suspend fun deleteAllItems()
    suspend fun addFromPantryItemIfNotPresent(pantryItem: PantryItem): Long?
}
