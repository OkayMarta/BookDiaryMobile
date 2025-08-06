package com.example.bookdiarymobile.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Залежності будуть жити, поки живе додаток
object AppModule {

    @Provides
    @Singleton // Гарантує, що буде створено лише один екземпляр бази даних
    fun provideBookDatabase(@ApplicationContext context: Context): BookDatabase {
        return Room.databaseBuilder(
            context,
            BookDatabase::class.java,
            "books_database"
        )
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .enableMultiInstanceInvalidation()
            .build()
    }

    @Provides
    @Singleton // Один екземпляр DAO для всього додатку
    fun provideBookDao(database: BookDatabase): BookDao {
        return database.bookDao()
    }

    @Provides
    @Singleton // Один екземпляр репозиторію
    fun provideBookRepository(bookDao: BookDao): BookRepository {
        return BookRepository(bookDao)
    }
}