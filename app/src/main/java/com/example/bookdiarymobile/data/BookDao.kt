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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBook(book: Book)

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookById(bookId: Int): Flow<Book>

    @Query("SELECT * FROM books WHERE status = 'READ' ORDER BY date_read DESC")
    fun getAllReadBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = 'TO_READ' ORDER BY date_added DESC")
    fun getAllToReadBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE is_favorite = 1")
    fun getFavoriteBooks(): Flow<List<Book>>

    @Query("SELECT COUNT(*) FROM books WHERE status = 'READ'")
    fun getTotalBooksRead(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books WHERE status = 'READ' AND date_read >= :startDate AND date_read < :endDate")
    fun getBooksReadCountBetween(startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM books WHERE status = 'READ' AND date_read >= :yearStart AND date_read < :yearEnd")
    fun getBooksReadCountForYear(yearStart: Long, yearEnd: Long): Flow<Int>

    @Query("SELECT * FROM books")
    suspend fun getAllBooksForBackup(): List<Book>

    // --- МЕТОДИ ДЛЯ СОРТУВАННЯ ---

    // --- Списки Read та Favorites ---
    @Query("SELECT * FROM books WHERE status = 'READ' ORDER BY title ASC")
    fun getAllReadBooksSortedByTitleAsc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = 'READ' ORDER BY title DESC")
    fun getAllReadBooksSortedByTitleDesc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = 'READ' ORDER BY date_read ASC")
    fun getAllReadBooksSortedByDateAsc(): Flow<List<Book>>
    // Ваш існуючий getAllReadBooks() вже сортує по date_read DESC, ми можемо його перевикористати або створити новий для ясності
    @Query("SELECT * FROM books WHERE status = 'READ' ORDER BY date_read DESC")
    fun getAllReadBooksSortedByDateDesc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = 'READ' ORDER BY rating ASC")
    fun getAllReadBooksSortedByRatingAsc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = 'READ' ORDER BY rating DESC")
    fun getAllReadBooksSortedByRatingDesc(): Flow<List<Book>>

    // Оскільки Favorites - це підмножина Read, ми можемо перевикористати логіку ViewModel
// але для чистоти додамо окремі методи і для Favorites
    @Query("SELECT * FROM books WHERE is_favorite = 1 ORDER BY title ASC")
    fun getFavoriteBooksSortedByTitleAsc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE is_favorite = 1 ORDER BY title DESC")
    fun getFavoriteBooksSortedByTitleDesc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE is_favorite = 1 ORDER BY date_read ASC")
    fun getFavoriteBooksSortedByDateAsc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE is_favorite = 1 ORDER BY date_read DESC")
    fun getFavoriteBooksSortedByDateDesc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE is_favorite = 1 ORDER BY rating ASC")
    fun getFavoriteBooksSortedByRatingAsc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE is_favorite = 1 ORDER BY rating DESC")
    fun getFavoriteBooksSortedByRatingDesc(): Flow<List<Book>>

    // --- Список To Read ---
    @Query("SELECT * FROM books WHERE status = 'TO_READ' ORDER BY title ASC")
    fun getAllToReadBooksSortedByTitleAsc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = 'TO_READ' ORDER BY title DESC")
    fun getAllToReadBooksSortedByTitleDesc(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = 'TO_READ' ORDER BY date_added ASC")
    fun getAllToReadBooksSortedByDateAsc(): Flow<List<Book>>
    // Ваш існуючий getAllToReadBooks() вже сортує по date_added DESC
    @Query("SELECT * FROM books WHERE status = 'TO_READ' ORDER BY date_added DESC")
    fun getAllToReadBooksSortedByDateDesc(): Flow<List<Book>>
}