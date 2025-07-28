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
}