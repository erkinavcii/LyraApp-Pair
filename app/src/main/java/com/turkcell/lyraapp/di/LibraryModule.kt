package com.turkcell.lyraapp.di

import com.turkcell.lyraapp.data.library.InMemoryLibraryRepository
import com.turkcell.lyraapp.data.library.LibraryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(
        impl: InMemoryLibraryRepository
    ): LibraryRepository
}
