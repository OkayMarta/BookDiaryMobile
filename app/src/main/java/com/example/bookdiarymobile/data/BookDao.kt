package com.example.bookdiarymobile.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    // === Базові операції ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBook(book: Book)

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    // === Запити на отримання даних ===

    // Отримуємо одну книгу за її ID
    @Query("SELECT * FROM books_table WHERE id = :bookId")
    fun getBookById(bookId: Int): Flow<Book>

    // Отримуємо всі прочитані книги, відсортовані за датою (нові згори)
    @Query("SELECT * FROM books_table WHERE status = 'READ' ORDER BY dateRead DESC")
    fun getAllReadBooks(): Flow<List<Book>>

    // Отримуємо всі книги, які потрібно прочитати, відсортовані за датою додавання
    @Query("SELECT * FROM books_table WHERE status = 'TO_READ' ORDER BY dateAdded DESC")
    fun getAllToReadBooks(): Flow<List<Book>>

    // Отримуємо всі вибрані книги
    @Query("SELECT * FROM books_table WHERE isFavorite = 1") // 1 означає true
    fun getFavoriteBooks(): Flow<List<Book>>


    // === Запити для статистики ===

    // Отримуємо загальну кількість прочитаних книг
    @Query("SELECT COUNT(*) FROM books_table WHERE status = 'READ'")
    fun getTotalBooksRead(): Flow<Int>

    // Отримуємо кількість книг, прочитаних після певної дати (в Unix Timestamp)
    @Query("SELECT COUNT(*) FROM books_table WHERE status = 'READ' AND dateRead >= :startDate")
    fun getBooksReadCountSince(startDate: Long): Flow<Int>

    // === Запити для Бекапу ===

    // Отримуємо абсолютно всі книги для бекапу
    @Query("SELECT * FROM books_table")
    suspend fun getAllBooksForBackup(): List<Book>
}