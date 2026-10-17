package com.lazycart.app.data.repository

import com.lazycart.app.data.local.db.ShoppingListItemDao
import com.lazycart.app.data.local.db.toDomain
import com.lazycart.app.data.local.db.toEntity
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.model.ShoppingListItem
import com.lazycart.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListRepositoryImpl @Inject constructor(
    private val shoppingListItemDao: ShoppingListItemDao
) : ShoppingListRepository {

    override fun observeAllShoppingItems(): Flow<List<ShoppingListItem>> =
        shoppingListItemDao.observeAllShoppingItems().map { list -> list.map { it.toDomain() } }

    override fun observePendingItems(): Flow<List<ShoppingListItem>> =
        shoppingListItemDao.observePendingItems().map { list -> list.map { it.toDomain() } }

    override suspend fun addItem(item: ShoppingListItem): Long =
        shoppingListItemDao.insertItem(item.toEntity())

    override suspend fun updateItem(item: ShoppingListItem) =
        shoppingListItemDao.updateItem(item.toEntity())

    override suspend fun removeItem(id: Long) =
        shoppingListItemDao.deleteById(id)

    override suspend fun setItemPurchased(id: Long, purchased: Boolean) =
        shoppingListItemDao.setItemPurchased(id, purchased)

    override suspend fun clearPurchasedItems() =
        shoppingListItemDao.clearPurchasedItems()

    override suspend fun deleteAllItems() =
        shoppingListItemDao.deleteAllItems()

    /**
     * Adds a pantry item to the shopping list only if it isn't already present.
     * Returns the new item's ID if inserted, null if it already existed.
     */
    override suspend fun addFromPantryItemIfNotPresent(pantryItem: PantryItem): Long? {
        val existing = shoppingListItemDao.getItemByPantryId(pantryItem.id)
        if (existing != null) return null

        val shoppingItem = ShoppingListItem(
            pantryItemId = pantryItem.id,
            name = pantryItem.name,
            quantity = 1f,          // Default quantity — user can edit
            unit = pantryItem.unit,
            priceHint = pantryItem.priceHint,
            category = pantryItem.category
        )
        return shoppingListItemDao.insertItem(shoppingItem.toEntity())
    }
}
