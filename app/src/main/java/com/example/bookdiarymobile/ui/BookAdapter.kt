package com.example.bookdiarymobile.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Адаптер для [RecyclerView], призначений для відображення списку об'єктів [Book].
 *
 * Використовує [ListAdapter] разом з [BookDiffCallback] для ефективного оновлення
 * списку. Це дозволяє автоматично розраховувати зміни між старим та новим
 * списком і оновлювати лише ті елементи, які змінилися, що є значно
 * продуктивнішим, ніж повне оновлення списку.
 *
 * @param onBookClicked Лямбда-функція, що викликається при натисканні на елемент списку.
 *                      Приймає об'єкт [Book], на який було натиснуто.
 */
class BookAdapter(
    private val onBookClicked: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    /**
     * ViewHolder, що представляє один елемент у списку книг.
     *
     * Відповідає за зберігання посилань на UI-елементи макету `list_item_book.xml`
     * та за наповнення цих елементів даними з об'єкта [Book].
     *
     * @param itemView View-компонент одного елемента списку.
     * @param onBookClicked Лямбда-функція, що передається з адаптера для обробки кліків.
     */
    class BookViewHolder(itemView: View, private val onBookClicked: (Book) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.text_view_description)
        private val dateTextView: TextView = itemView.findViewById(R.id.text_view_date)
        private val ratingStarIcon: ImageView = itemView.findViewById(R.id.icon_rating_star)
        private val ratingTextView: TextView = itemView.findViewById(R.id.text_view_rating)
        private val coverImageView: ImageView = itemView.findViewById(R.id.image_view_cover)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.icon_favorite_crown)

        private var currentBook: Book? = null

        init {
            itemView.setOnClickListener {
                currentBook?.let { book ->
                    onBookClicked(book)
                }
            }
        }

        /**
         * Наповнює UI-елементи даними з наданого об'єкта [Book].
         *
         * Логіка включає:
         * - Встановлення назви, автора та опису (опис ховається, якщо порожній).
         * - Відображення іконки "улюбленого", якщо `isFavorite` є `true`.
         * - Адаптивне відображення дати та рейтингу залежно від статусу книги:
         *   - Для [BookStatus.READ]: показує дату прочитання та рейтинг. Іконка зірки
         *     змінюється залежно від значення рейтингу (заповнена, наполовину, порожня).
         *   - Для [BookStatus.TO_READ]: ховає рейтинг і показує дату додавання.
         * - Завантаження обкладинки за допомогою [Glide], або встановлення
         *   зображення-заглушки, якщо шлях до обкладинки відсутній.
         *
         * @param book Об'єкт [Book], дані якого потрібно відобразити.
         */
        fun bind(book: Book) {
            currentBook = book
            titleTextView.text = book.title
            authorTextView.text = book.author

            if (book.description.isNotBlank()) {
                descriptionTextView.text = book.description
                descriptionTextView.visibility = View.VISIBLE
            } else {
                descriptionTextView.visibility = View.GONE
            }

            favoriteIcon.visibility = if (book.isFavorite) View.VISIBLE else View.GONE

            if (book.status == BookStatus.READ) {
                book.dateRead?.let {
                    dateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                    dateTextView.visibility = View.VISIBLE
                } ?: run {
                    dateTextView.visibility = View.GONE
                }

                book.rating?.let { ratingValue ->
                    ratingTextView.text = ratingValue.toString()
                    ratingTextView.visibility = View.VISIBLE
                    ratingStarIcon.visibility = View.VISIBLE

                    // Вибираємо іконку зірки залежно від рейтингу
                    when (ratingValue) {
                        5, 4 -> ratingStarIcon.setImageResource(R.drawable.ic_star_filled)
                        3 -> ratingStarIcon.setImageResource(R.drawable.ic_star_half)
                        2, 1 -> ratingStarIcon.setImageResource(R.drawable.ic_star_outline)
                        else -> ratingStarIcon.visibility = View.GONE
                    }
                } ?: run {
                    ratingStarIcon.visibility = View.GONE
                    ratingTextView.visibility = View.GONE
                }
            } else { // Для статусу TO_READ
                ratingStarIcon.visibility = View.GONE
                ratingTextView.visibility = View.GONE

                dateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(book.dateAdded))
                dateTextView.visibility = View.VISIBLE
            }

            if (book.coverImagePath != null) {
                Glide.with(itemView.context)
                    .load(book.coverImagePath)
                    .placeholder(R.drawable.placeholder_cover)
                    .into(coverImageView)
            } else {
                coverImageView.setImageResource(R.drawable.placeholder_cover_sharp)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_book, parent, false)
        return BookViewHolder(view, onBookClicked)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book)
    }
}

/**
 * Реалізація [DiffUtil.ItemCallback] для [Book].
 *
 * Дозволяє [ListAdapter] ефективно визначати, які елементи списку
 * були додані, видалені або змінені, та оновлювати лише їх,
 * а не весь список.
 */
class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    /**
     * Перевіряє, чи два об'єкти представляють один і той самий елемент.
     * Порівняння відбувається за унікальним ідентифікатором.
     */
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem.id == newItem.id
    }

    /**
     * Перевіряє, чи вміст двох об'єктів однаковий.
     * Використовує згенерований метод `equals()` для data-класу [Book],
     * що порівнює всі поля.
     */
    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem == newItem
    }
}