package com.example.bookdiarymobile.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.Book
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 1. Додаємо в конструктор функцію-обробник кліку: onBookClicked
class BookAdapter(
    private val onBookClicked: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    // 2. Клас ViewHolder тепер також приймає цю функцію в конструктор
    class BookViewHolder(itemView: View, private val onBookClicked: (Book) -> Unit) : RecyclerView.ViewHolder(itemView) {
        // Поля залишаються без змін
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)
        private val dateReadTextView: TextView = itemView.findViewById(R.id.text_view_date_read)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar_book)
        private val coverImageView: ImageView = itemView.findViewById(R.id.image_view_cover)

        // Додаємо змінну для зберігання поточної книги
        private var currentBook: Book? = null

        init {
            // 3. Встановлюємо слухача кліків на всю картку
            itemView.setOnClickListener {
                // При кліку викликаємо передану функцію onBookClicked
                currentBook?.let { book ->
                    onBookClicked(book)
                }
            }
        }

        fun bind(book: Book) {
            // Зберігаємо поточну книгу, щоб можна було використати її ID при кліку
            currentBook = book

            titleTextView.text = book.title
            authorTextView.text = book.author

            // 4. Додаємо логіку видимості для полів дати та рейтингу
            if (book.dateRead != null && book.rating != null) {
                // Якщо книга прочитана, показуємо дату та рейтинг
                val formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(book.dateRead))
                dateReadTextView.text = itemView.context.getString(R.string.read_on_date, formattedDate)
                ratingBar.rating = book.rating.toFloat()

                dateReadTextView.visibility = View.VISIBLE
                ratingBar.visibility = View.VISIBLE
            } else {
                // Якщо книга "До прочитання", ховаємо ці поля
                dateReadTextView.visibility = View.GONE
                ratingBar.visibility = View.GONE
            }

            // Тут пізніше буде код для завантаження обкладинки
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_book, parent, false)
        // 5. Передаємо обробник кліку при створенні ViewHolder
        return BookViewHolder(view, onBookClicked)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book)
    }
}

// Цей клас залишається без змін
class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem == newItem
    }
}