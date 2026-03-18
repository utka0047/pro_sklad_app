package com.tornus.pro_sklad.di

import android.content.Context
import androidx.room.Room
import com.tornus.pro_sklad.data.local.AppDatabase
import com.tornus.pro_sklad.data.local.dao.MovementDao
import com.tornus.pro_sklad.data.local.dao.ProductDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "sklad.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideMovementDao(db: AppDatabase): MovementDao = db.movementDao()
}
