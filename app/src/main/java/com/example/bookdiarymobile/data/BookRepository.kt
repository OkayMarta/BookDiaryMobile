package com.example.bookdiarymobile.data

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Клас-репозиторій, що виступає єдиним джерелом даних (Single Source of Truth) для додатку.
 *
 * Він абстрагує джерела даних від решти додатку (зокрема, від ViewModel).
 * У цьому випадку він є посередником між ViewModel та [BookDao], надаючи зручний
 * API для доступу до даних та приховуючи деталі реалізації запитів до бази даних.
 *
 * @property bookDao Екземпляр DAO, що надається через Hilt для взаємодії з базою даних.
 */
@Singleton
class BookRepository @Inject constructor(private val bookDao: BookDao) {

    /**
     * Отримує потік ([Flow]) з однією книгою за її унікальним ідентифікатором.
     *
     * @param id Унікальний ідентифікатор книги.
     * @return [Flow], що випромінює об'єкт [Book].
     */
    fun getBookById(id: Int): Flow<Book> {
        return bookDao.getBookById(id)
    }

    /**
     * Асинхронно додає нову книгу до бази даних.
     *
     * @param book Об'єкт [Book] для додавання.
     */
    suspend fun addBook(book: Book) {
        bookDao.addBook(book)
    }

    /**
     * Асинхронно оновлює існуючу книгу в базі даних.
     *
     * @param book Об'єкт [Book] з оновленими даними.
     */
    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book)
    }

    /**
     * Асинхронно видаляє книгу з бази даних, а також її файл обкладинки зі сховища, якщо він існує.
     *
     * @param book Об'єкт [Book], який потрібно видалити.
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
                // Логування помилки, якщо файл обкладинки не вдалося видалити
                e.printStackTrace()
            }
        }
    }

    /**
     * Асинхронно отримує повний список книг. Використовується для створення резервної копії.
     * Повертає звичайний список, а не [Flow], оскільки це одноразова операція.
     *
     * @return `List<Book>` зі всіма книгами в базі даних.
     */
    suspend fun getAllBooksForBackup(): List<Book> = bookDao.getAllBooksForBackup()

    /**
     * Надає потік ([Flow]) із загальною кількістю прочитаних книг для екрану статистики.
     *
     * @return [Flow], що випромінює ціле число (кількість книг).
     */
    fun getTotalBooksRead(): Flow<Int> = bookDao.getTotalBooksRead()

    /**
     * Надає потік ([Flow]) з кількістю книг, прочитаних у заданому проміжку часу (наприклад, за місяць).
     *
     * @param startDate Початкова дата у форматі timestamp.
     * @param endDate Кінцева дата у форматі timestamp.
     * @return [Flow], що випромінює кількість книг.
     */
    fun getBooksReadCountBetween(startDate: Long, endDate: Long): Flow<Int> =
        bookDao.getBooksReadCountBetween(startDate, endDate)

    /**
     * Надає потік ([Flow]) з кількістю книг, прочитаних за вказаний рік.
     *
     * @param yearStart Початок року у форматі timestamp.
     * @param yearEnd Кінець року у форматі timestamp.
     * @return [Flow], що випромінює кількість книг.
     */
    fun getBooksReadCountForYear(yearStart: Long, yearEnd: Long): Flow<Int> =
        bookDao.getBooksReadCountForYear(yearStart, yearEnd)


    /**
     * Універсальний метод, який отримує відфільтрований та відсортований список книг.
     *
     * Цей метод динамічно будує SQL-запит на основі переданих параметрів, що дозволяє
     * реалізувати фільтрацію за статусом, повнотекстовий пошук та сортування на рівні
     * бази даних, що є дуже ефективним.
     *
     * @param screenType Тип екрану ([ScreenType]), який визначає основний фільтр (прочитані, хочу прочитати, улюблені).
     * @param sortOrder Параметр сортування ([SortOrder]), що визначає поле та напрямок сортування.
     * @param searchQuery Рядок для повнотекстового пошуку за назвою та автором. Якщо порожній, пошук не виконується.
     * @return [Flow] зі списком книг, що відповідає заданим критеріям.
     */
    fun getFilteredAndSortedBooks(
        screenType: ScreenType,
        sortOrder: SortOrder,
        searchQuery: String
    ): Flow<List<Book>> {
        val queryBuilder = StringBuilder("SELECT * FROM books WHERE ")

        // Крок 1: Додавання фільтрації за типом екрану
        when (screenType) {
            ScreenType.READ -> queryBuilder.append("status = 'READ'")
            ScreenType.TO_READ -> queryBuilder.append("status = 'TO_READ'")
            ScreenType.FAVORITES -> queryBuilder.append("is_favorite = 1")
        }

        // Крок 2: Додавання умови пошуку, якщо пошуковий запит не порожній
        if (searchQuery.isNotEmpty()) {
            queryBuilder.append(" AND (title LIKE '%' || ? || '%' OR author LIKE '%' || ? || '%')")
        }

        // Крок 3: Додавання сортування
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

        // Виконання "сирого" запиту через DAO
        return bookDao.getBooksWithQuery(SimpleSQLiteQuery(sqlQuery, args))
    }
}