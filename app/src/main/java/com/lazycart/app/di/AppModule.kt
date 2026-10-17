package com.lazycart.app.di

import android.content.Context
import androidx.room.Room
import com.lazycart.app.data.local.db.ConsumptionEventDao
import com.lazycart.app.data.local.db.PantryItemDao
import com.lazycart.app.data.local.db.LazyCartDatabase
import com.lazycart.app.data.local.db.ShoppingListItemDao
import com.lazycart.app.data.repository.ConsumptionRepositoryImpl
import com.lazycart.app.data.repository.PantryRepositoryImpl
import com.lazycart.app.data.repository.ShoppingListRepositoryImpl
import com.lazycart.app.domain.repository.ConsumptionRepository
import com.lazycart.app.domain.repository.PantryRepository
import com.lazycart.app.domain.repository.ShoppingListRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LazyCartDatabase =
        Room.databaseBuilder(
            context,
            LazyCartDatabase::class.java,
            "LazyCart.db"
        )
        .fallbackToDestructiveMigration() // Acceptable during development; add migrations before release
        .build()

    @Provides
    fun providePantryItemDao(db: LazyCartDatabase): PantryItemDao = db.pantryItemDao()

    @Provides
    fun provideConsumptionEventDao(db: LazyCartDatabase): ConsumptionEventDao = db.consumptionEventDao()

    @Provides
    fun provideShoppingListItemDao(db: LazyCartDatabase): ShoppingListItemDao = db.shoppingListItemDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPantryRepository(impl: PantryRepositoryImpl): PantryRepository

    @Binds
    @Singleton
    abstract fun bindConsumptionRepository(impl: ConsumptionRepositoryImpl): ConsumptionRepository

    @Binds
    @Singleton
    abstract fun bindShoppingListRepository(impl: ShoppingListRepositoryImpl): ShoppingListRepository
}
