package com.lazycart.app.data.repository

import com.lazycart.app.data.local.db.ConsumptionEventDao
import com.lazycart.app.data.local.db.PantryItemDao
import com.lazycart.app.data.local.db.toDomain
import com.lazycart.app.data.local.db.toEntity
import com.lazycart.app.domain.model.ConsumptionEvent
import com.lazycart.app.domain.model.ItemCategory
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.repository.ConsumptionRepository
import com.lazycart.app.domain.repository.PantryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PantryRepositoryImpl @Inject constructor(
    private val pantryItemDao: PantryItemDao
) : PantryRepository {

    override fun observeActivePantryItems(): Flow<List<PantryItem>> =
        pantryItemDao.observeActivePantryItems().map { list -> list.map { it.toDomain() } }

    override fun observeAllPantryItems(): Flow<List<PantryItem>> =
        pantryItemDao.observeAllPantryItems().map { list -> list.map { it.toDomain() } }

    override fun observeItemById(id: Long): Flow<PantryItem?> =
        pantryItemDao.observeItemById(id).map { it?.toDomain() }

    override fun observeByCategory(category: ItemCategory): Flow<List<PantryItem>> =
        pantryItemDao.observeByCategory(category.name).map { list -> list.map { it.toDomain() } }

    override fun observeByNameQuery(query: String): Flow<List<PantryItem>> =
        pantryItemDao.observeByNameQuery(query).map { list -> list.map { it.toDomain() } }

    override suspend fun getItemById(id: Long): PantryItem? =
        pantryItemDao.getItemById(id)?.toDomain()

    override suspend fun addItem(item: PantryItem): Long =
        pantryItemDao.insertItem(item.toEntity())

    override suspend fun updateItem(item: PantryItem) =
        pantryItemDao.updateItem(item.toEntity())

    override suspend fun deleteItem(item: PantryItem) =
        pantryItemDao.deleteItem(item.toEntity())

    override suspend fun markAsFinished(id: Long) =
        pantryItemDao.markAsFinished(id)

    override suspend fun updateQuantity(id: Long, newQuantity: Float) =
        pantryItemDao.updateQuantity(id, newQuantity)

    override suspend fun deleteAllItems() =
        pantryItemDao.deleteAllItems()
}

@Singleton
class ConsumptionRepositoryImpl @Inject constructor(
    private val consumptionEventDao: ConsumptionEventDao
) : ConsumptionRepository {

    override fun observeEventsForItem(itemId: Long): Flow<List<ConsumptionEvent>> =
        consumptionEventDao.observeEventsForItem(itemId).map { list -> list.map { it.toDomain() } }

    override suspend fun getEventsForItemSince(itemId: Long, sinceEpochSecond: Long): List<ConsumptionEvent> =
        consumptionEventDao.getEventsForItemSince(itemId, sinceEpochSecond).map { it.toDomain() }

    override suspend fun getAllEventsForItem(itemId: Long): List<ConsumptionEvent> =
        consumptionEventDao.getAllEventsForItem(itemId).map { it.toDomain() }

    override suspend fun logConsumption(event: ConsumptionEvent) {
        consumptionEventDao.insertEvent(event.toEntity())
    }

    override suspend fun deleteEvent(event: ConsumptionEvent) =
        consumptionEventDao.deleteEvent(event.toEntity())

    override suspend fun deleteAllEvents() =
        consumptionEventDao.deleteAllEvents()
}
