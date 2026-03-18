package com.tornus.pro_sklad.di

import com.tornus.pro_sklad.data.repository.MovementRepositoryImpl
import com.tornus.pro_sklad.data.repository.ProductRepositoryImpl
import com.tornus.pro_sklad.domain.repository.MovementRepository
import com.tornus.pro_sklad.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindMovementRepository(impl: MovementRepositoryImpl): MovementRepository
}
