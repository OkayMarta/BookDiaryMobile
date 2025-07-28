package com.example.bookdiarymobile.data

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Клас-репозиторій, що є єдиним джерелом даних для додатку.
 * Він приховує деталі реалізації джерел даних (у цьому випадку, робота з базою даних Room через BookDao)
 * і надає зручний API для ViewModel.
 */
class BookRepository(private val bookDao: BookDao) {

    /**
     * Надає потік (Flow) зі списком усіх прочитаних книг.
     * Інтерфейс буде автоматично оновлюватися при змінах у базі даних.
     */
    val allReadBooks: Flow<List<Book>> = bookDao.getAllReadBooks()

    /**
     * Надає потік (Flow) зі списком усіх книг, запланованих до прочитання.
     */
    val allToReadBooks: Flow<List<Book>> = bookDao.getAllToReadBooks()

    /**
     * Надає потік (Flow) зі списком усіх улюблених книг.
     */
    val favoriteBooks: Flow<List<Book>> = bookDao.getFavoriteBooks()

    /**
     * Отримує потік (Flow) з однією книгою за її унікальним ідентифікатором (ID).
     */
    fun getBookById(id: Int): Flow<Book> {
        return bookDao.getBookById(id)
    }

    /**
     * Асинхронно додає нову книгу в базу даних.
     */
    suspend fun addBook(book: Book) {
        bookDao.addBook(book)
    }

    /**
     * Асинхронно оновлює існуючу книгу в базі даних.
     */
    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book)
    }

    /**
     * Асинхронно видаляє книгу з бази даних, а також її файл обкладинки зі сховища.
     */
    suspend fun deleteBook(book: Book) {
        // 1. Видалення запису про книгу з бази даних.
        bookDao.deleteBook(book)

        // 2. Видалення файлу обкладинки, якщо він існує.
        book.coverImagePath?.let { path ->
            try {
                val coverFile = File(path)
                if (coverFile.exists()) {
                    coverFile.delete()
                }
            } catch (e: Exception) {
                // 3. Обробка можливих помилок під час видалення файлу, щоб уникнути збою додатку.
                e.printStackTrace()
            }
        }
    }

    /**
     * Асинхронно отримує повний список книг для створення резервної копії.
     */
    suspend fun getAllBooksForBackup(): List<Book> = bookDao.getAllBooksForBackup()

    /**
     * Надає потік (Flow) із загальною кількістю прочитаних книг для екрану статистики.
     */
    fun getTotalBooksRead(): Flow<Int> = bookDao.getTotalBooksRead()

    /**
     * Надає потік (Flow) з кількістю книг, прочитаних у заданому проміжку часу.
     */
    fun getBooksReadCountBetween(startDate: Long, endDate: Long): Flow<Int> =
        bookDao.getBooksReadCountBetween(startDate, endDate)

    /**
     * Надає потік (Flow) з кількістю книг, прочитаних за вказаний рік.
     */
    fun getBooksReadCountForYear(yearStart: Long, yearEnd: Long): Flow<Int> =
        bookDao.getBooksReadCountForYear(yearStart, yearEnd)
}