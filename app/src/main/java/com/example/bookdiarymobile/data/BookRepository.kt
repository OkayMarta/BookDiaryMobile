package com.example.bookdiarymobile.data

import androidx.sqlite.db.SimpleSQLiteQuery
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
        bookDao.deleteBook(book)
        book.coverImagePath?.let { path ->
            try {
                val coverFile = File(path)
                if (coverFile.exists()) {
                    coverFile.delete()
                }
            } catch (e: Exception) {
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


    /**
     * Універсальний метод, який отримує відфільтрований та відсортований список книг.
     * Використовується всіма ViewModel'ами для відображення списків.
     */
    fun getFilteredAndSortedBooks(
        screenType: ScreenType,
        sortOrder: SortOrder,
        searchQuery: String
    ): Flow<List<Book>> {
        val queryBuilder = StringBuilder("SELECT * FROM books WHERE ")

        // 1. Додаємо фільтр за типом екрану
        when (screenType) {
            ScreenType.READ -> queryBuilder.append("status = 'READ'")
            ScreenType.TO_READ -> queryBuilder.append("status = 'TO_READ'")
            ScreenType.FAVORITES -> queryBuilder.append("is_favorite = 1")
        }

        // 2. Додаємо фільтр за пошуковим запитом, якщо він не порожній
        if (searchQuery.isNotEmpty()) {
            queryBuilder.append(" AND (title LIKE '%' || ? || '%' OR author LIKE '%' || ? || '%')")
        }

        // 3. Додаємо сортування
        queryBuilder.append(" ORDER BY ")
        when (sortOrder) {
            SortOrder.TITLE_ASC -> queryBuilder.append("title ASC")
            SortOrder.TITLE_DESC -> queryBuilder.append("title DESC")
            SortOrder.DATE_READ_ASC -> queryBuilder.append("date_read ASC")
            SortOrder.DATE_READ_DESC -> queryBuilder.append("date_read DESC")
            SortOrder.RATING_ASC -> queryBuilder.append("rating ASC")
            SortOrder.RATING_DESC -> queryBuilder.append("rating DESC")
            SortOrder.DATE_ADDED_ASC -> queryBuilder.append("date_added ASC")
            SortOrder.DATE_ADDED_DESC -> queryBuilder.append("date_added DESC")
        }

        val sqlQuery = queryBuilder.toString()
        val args = if (searchQuery.isNotEmpty()) arrayOf(searchQuery, searchQuery) else emptyArray()

        return bookDao.getBooksWithQuery(SimpleSQLiteQuery(sqlQuery, args))
    }
}