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

/**
 * Hilt-модуль, що відповідає за надання залежностей на рівні додатку.
 *
 * Використовує `@InstallIn(SingletonComponent::class)`, що означає, що всі надані
 * залежності будуть синглтонами і їх життєвий цикл буде відповідати життєвому
 * циклу додатку.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Надає синглтон-екземпляр [BookDatabase].
     *
     * Створює базу даних Room з назвою "books_database".
     * - `setJournalMode(TRUNCATE)`: Встановлює режим журналу для кращої сумісності
     *   з процесом резервного копіювання.
     * - `enableMultiInstanceInvalidation()`: Дозволяє інвалідацію даних, якщо
     *   кілька екземплярів бази даних працюють з одним файлом, що корисно
     *   після відновлення з резервної копії.
     *
     * @param context Контекст додатку, що надається Hilt.
     * @return Синглтон-екземпляр [BookDatabase].
     */
    @Provides
    @Singleton
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

    /**
     * Надає синглтон-екземпляр [BookDao].
     *
     * DAO отримується з екземпляра [BookDatabase].
     *
     * @param database Екземпляр бази даних додатку.
     * @return Синглтон-екземпляр [BookDao].
     */
    @Provides
    @Singleton
    fun provideBookDao(database: BookDatabase): BookDao {
        return database.bookDao()
    }

    /**
     * Надає синглтон-екземпляр [BookRepository].
     *
     * Репозиторій є посередником між шаром даних (DAO) та ViewModel.
     *
     * @param bookDao Екземпляр Data Access Object для роботи з книгами.
     * @return Синглтон-екземпляр [BookRepository].
     */
    @Provides
    @Singleton
    fun provideBookRepository(bookDao: BookDao): BookRepository {
        return BookRepository(bookDao)
    }
}