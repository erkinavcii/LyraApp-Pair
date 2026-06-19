package com.turkcell.lyraapp.di

import com.turkcell.lyraapp.data.home.HomeRepository
import com.turkcell.lyraapp.data.home.NetworkHomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: NetworkHomeRepository): HomeRepository
}