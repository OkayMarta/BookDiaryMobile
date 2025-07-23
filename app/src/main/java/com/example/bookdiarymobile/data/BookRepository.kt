package com.example.bookdiarymobile.data

import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {

    // Функции для получения списков просто "пробрасывают" вызовы к DAO
    val allReadBooks: Flow<List<Book>> = bookDao.getAllReadBooks()
    val allToReadBooks: Flow<List<Book>> = bookDao.getAllToReadBooks()
    val favoriteBooks: Flow<List<Book>> = bookDao.getFavoriteBooks()

    // Функция для получения одной книги по ID
    fun getBookById(id: Int): Flow<Book> {
        return bookDao.getBookById(id)
    }

    // Функции для изменения данных также "пробрасывают" вызовы к DAO
    suspend fun addBook(book: Book) {
        bookDao.addBook(book)
    }

    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book)
    }

    suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(book)
    }
}