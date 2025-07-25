package com.example.bookdiarymobile.data

import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {

    // Функції для отримання списків просто "прокидають" виклики до DAO
    val allReadBooks: Flow<List<Book>> = bookDao.getAllReadBooks()
    val allToReadBooks: Flow<List<Book>> = bookDao.getAllToReadBooks()
    val favoriteBooks: Flow<List<Book>> = bookDao.getFavoriteBooks()

    // Функція для отримання однієї книги за ID
    fun getBookById(id: Int): Flow<Book> {
        return bookDao.getBookById(id)
    }

    // Функції для зміни даних також "прокидають" виклики до DAO
    suspend fun addBook(book: Book) {
        bookDao.addBook(book)
    }

    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book)
    }

    suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(book)
    }

    // Функції для статистики
    fun getTotalBooksRead(): Flow<Int> = bookDao.getTotalBooksRead()

    fun getBooksReadCountSince(startDate: Long): Flow<Int> = bookDao.getBooksReadCountSince(startDate)
}