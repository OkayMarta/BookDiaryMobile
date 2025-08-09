package com.example.bookdiarymobile.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для роботи з сутністю [Book] в базі даних Room.
 *
 * Надає методи для виконання CRUD-операцій (створення, читання, оновлення, видалення),
 * а також для отримання даних для статистики та реалізації динамічного сортування/пошуку.
 * Більшість методів повертають [Flow], що дозволяє UI реагувати на зміни в базі даних.
 */
@Dao
interface BookDao {

    /**
     * Додає нову книгу до бази даних.
     *
     * Якщо книга з таким же `id` вже існує, вона буде замінена завдяки
     * стратегії [OnConflictStrategy.REPLACE].
     * @param book Об'єкт [Book], який потрібно додати.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBook(book: Book)

    /**
     * Оновлює існуючу книгу в базі даних.
     *
     * @param book Об'єкт [Book] з оновленими даними.
     */
    @Update
    suspend fun updateBook(book: Book)

    /**
     * Видаляє книгу з бази даних.
     *
     * @param book Об'єкт [Book], який потрібно видалити.
     */
    @Delete
    suspend fun deleteBook(book: Book)

    /**
     * Отримує одну книгу за її унікальним ідентифікатором.
     *
     * @param bookId Ідентифікатор книги.
     * @return [Flow], що випромінює об'єкт [Book] при кожній зміні даних цієї книги.
     */
    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookById(bookId: Int): Flow<Book>

    /**
     * Отримує список всіх прочитаних книг, відсортованих за датою прочитання (новіші спочатку).
     *
     * @return [Flow], що випромінює список книг зі статусом [BookStatus.READ].
     */
    @Query("SELECT * FROM books WHERE status = 'READ' ORDER BY date_read DESC")
    fun getAllReadBooks(): Flow<List<Book>>

    /**
     * Отримує список всіх книг, запланованих до прочитання, відсортованих за датою додавання (новіші спочатку).
     *
     * @return [Flow], що випромінює список книг зі статусом [BookStatus.TO_READ].
     */
    @Query("SELECT * FROM books WHERE status = 'TO_READ' ORDER BY date_added DESC")
    fun getAllToReadBooks(): Flow<List<Book>>

    /**
     * Отримує список улюблених книг.
     *
     * @return [Flow], що випромінює список книг, у яких поле `is_favorite` має значення `true`.
     */
    @Query("SELECT * FROM books WHERE is_favorite = 1")
    fun getFavoriteBooks(): Flow<List<Book>>

    /**
     * Повертає загальну кількість прочитаних книг.
     *
     * @return [Flow], що випромінює загальну кількість книг зі статусом [BookStatus.READ].
     */
    @Query("SELECT COUNT(*) FROM books WHERE status = 'READ'")
    fun getTotalBooksRead(): Flow<Int>

    /**
     * Повертає кількість книг, прочитаних у певному часовому проміжку (наприклад, за місяць).
     *
     * @param startDate Початкова дата (timestamp), включно.
     * @param endDate Кінцева дата (timestamp), не включно.
     * @return [Flow], що випромінює кількість книг.
     */
    @Query("SELECT COUNT(*) FROM books WHERE status = 'READ' AND date_read >= :startDate AND date_read < :endDate")
    fun getBooksReadCountBetween(startDate: Long, endDate: Long): Flow<Int>

    /**
     * Повертає кількість книг, прочитаних за певний рік.
     *
     * @param yearStart Початок року (timestamp).
     * @param yearEnd Кінець року (timestamp).
     * @return [Flow], що випромінює кількість книг.
     */
    @Query("SELECT COUNT(*) FROM books WHERE status = 'READ' AND date_read >= :yearStart AND date_read < :yearEnd")
    fun getBooksReadCountForYear(yearStart: Long, yearEnd: Long): Flow<Int>

    /**
     * Отримує повний список книг для створення резервної копії.
     *
     * Це `suspend` функція, оскільки вона виконує одноразову операцію, і немає потреби
     * спостерігати за змінами даних.
     *
     * @return `List<Book>` зі всіма книгами в базі даних.
     */
    @Query("SELECT * FROM books")
    suspend fun getAllBooksForBackup(): List<Book>

    /**
     * Виконує динамічний SQL-запит для фільтрації та сортування книг.
     *
     * Цей метод дозволяє гнучко будувати запити на основі вибору користувача
     * (порядок сортування, пошуковий запит).
     * `observedEntities = [Book::class]` гарантує, що [Flow] буде оновлюватися
     * при будь-яких змінах у таблиці `books`.
     *
     * @param query Об'єкт [SupportSQLiteQuery], що містить готовий SQL-запит.
     * @return [Flow], що випромінює список книг, які відповідають запиту.
     */
    @RawQuery(observedEntities = [Book::class])
    fun getBooksWithQuery(query: SupportSQLiteQuery): Flow<List<Book>>
}