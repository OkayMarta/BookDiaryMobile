package com.example.bookdiarymobile

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.bookdiarymobile.data.BookDatabase
import com.example.bookdiarymobile.data.BookRepository

/**
 * Основний клас додатку, що успадковується від Application.
 * Є центральною точкою для ініціалізації глобальних ресурсів,
 * таких як база даних та репозиторій, які доступні протягом усього життєвого циклу додатку.
 */
class BookApplication : Application() {

    /**
     * Екземпляр бази даних Room. Використовується `lateinit`, оскільки ініціалізація
     * відбувається в методі `onCreate`. `private set` обмежує можливість зміни
     * цього екземпляра ззовні класу.
     */
    lateinit var database: BookDatabase
        private set

    /**
     * Екземпляр репозиторію. Використовується `lazy` для відкладеної ініціалізації:
     * репозиторій буде створено лише при першому зверненні до нього.
     */
    val repository: BookRepository by lazy {
        BookRepository(database.bookDao())
    }

    /**
     * Цей метод викликається при запуску додатку.
     * Тут відбувається первинна ініціалізація бази даних.
     */
    override fun onCreate() {
        super.onCreate()
        initializeDatabase()
    }

    /**
     * Приватний метод, що інкапсулює логіку створення екземпляра бази даних Room.
     */
    private fun initializeDatabase() {
        database = Room.databaseBuilder(
            this,
            BookDatabase::class.java,
            "books_database"
        )
            // Встановлює режим журналу "TRUNCATE". Це змушує Room записувати всі зміни
            // безпосередньо в основний файл бази даних, що критично важливо для
            // стабільної роботи функції експорту.
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            // Дозволяє коректно оновлювати дані, якщо до файлу бази даних звертаються
            // з кількох місць, що корисно під час імпорту.
            .enableMultiInstanceInvalidation()
            .build()
    }

    /**
     * Публічний метод для безпечного закриття з'єднання з базою даних.
     * Необхідний для процесу імпорту, щоб звільнити файл БД перед його заміною.
     */
    fun closeDatabase() {
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
    }

    /**
     * Публічний метод для повторного створення екземпляра бази даних.
     * Використовується після імпорту, щоб додаток почав працювати з новою, відновленою БД.
     */
    fun reinitializeDatabase() {
        initializeDatabase()
    }
}