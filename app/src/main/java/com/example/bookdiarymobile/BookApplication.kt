package com.example.bookdiarymobile

import android.app.Application
import androidx.room.Room
import com.example.bookdiarymobile.data.BookDatabase
import com.example.bookdiarymobile.data.BookRepository

class BookApplication : Application() {

    // Використовуємо lazy, щоб база даних і репозиторій створювалися
    // тільки тоді, коли вони справді потрібні.
    val database: BookDatabase by lazy {
        Room.databaseBuilder(
            this,
            BookDatabase::class.java,
            "books_database" // Назва файлу бази даних
        ).build()
    }

    val repository: BookRepository by lazy {
        BookRepository(database.bookDao())
    }
}