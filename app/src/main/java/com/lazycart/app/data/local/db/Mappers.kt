package com.lazycart.app.data.local.db

import com.lazycart.app.domain.model.ConsumptionEvent
import com.lazycart.app.domain.model.ItemCategory
import com.lazycart.app.domain.model.ItemUnit
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.model.ShoppingListItem
import java.time.Instant
import java.time.LocalDate

// ── PantryItem mappers ─────────────────────────────────────────────────────

fun PantryItemEntity.toDomain(): PantryItem = PantryItem(
    id = id,
    name = name,
    category = ItemCategory.valueOf(category),
    quantity = quantity,
    unit = ItemUnit.valueOf(unit),
    purchaseDate = LocalDate.ofEpochDay(purchaseDateEpochDay),
    expiryDate = expiryDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    isFinished = isFinished,
    priceHint = priceHint
)

fun PantryItem.toEntity(): PantryItemEntity = PantryItemEntity(
    id = id,
    name = name,
    category = category.name,
    quantity = quantity,
    unit = unit.name,
    purchaseDateEpochDay = purchaseDate.toEpochDay(),
    expiryDateEpochDay = expiryDate?.toEpochDay(),
    isFinished = isFinished,
    priceHint = priceHint
)

// ── ConsumptionEvent mappers ────────────────────────────────────────────────

fun ConsumptionEventEntity.toDomain(): ConsumptionEvent = ConsumptionEvent(
    id = id,
    pantryItemId = pantryItemId,
    quantityConsumed = quantityConsumed,
    unit = ItemUnit.valueOf(unit),
    timestamp = Instant.ofEpochSecond(timestampEpochSecond),
    note = note
)

fun ConsumptionEvent.toEntity(): ConsumptionEventEntity = ConsumptionEventEntity(
    id = id,
    pantryItemId = pantryItemId,
    quantityConsumed = quantityConsumed,
    unit = unit.name,
    timestampEpochSecond = timestamp.epochSecond,
    note = note
)

// ── ShoppingListItem mappers ────────────────────────────────────────────────

fun ShoppingListItemEntity.toDomain(): ShoppingListItem = ShoppingListItem(
    id = id,
    pantryItemId = pantryItemId,
    name = name,
    quantity = quantity,
    unit = ItemUnit.valueOf(unit),
    isPurchased = isPurchased,
    priceHint = priceHint,
    category = ItemCategory.valueOf(category)
)

fun ShoppingListItem.toEntity(): ShoppingListItemEntity = ShoppingListItemEntity(
    id = id,
    pantryItemId = pantryItemId,
    name = name,
    quantity = quantity,
    unit = unit.name,
    isPurchased = isPurchased,
    priceHint = priceHint,
    category = category.name
)
