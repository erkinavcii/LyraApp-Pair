package com.turkcell.lyraapp.di

import android.content.Context
import androidx.room.Room
import com.turkcell.lyraapp.data.local.LyraDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): LyraDatabase =
        Room.databaseBuilder(context, LyraDatabase::class.java, "lyra_db").build()

    @Provides
    fun provideDownloadedSongDao(db: LyraDatabase) = db.downloadedSongDao()

    @Provides
    fun provideRecentlyPlayedDao(db: LyraDatabase) = db.recentlyPlayedDao()
}
