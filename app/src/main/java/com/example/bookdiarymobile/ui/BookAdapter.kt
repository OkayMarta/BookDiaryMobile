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
import com.bumptech.glide.Glide
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.Book
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookAdapter(
    private val onBookClicked: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    class BookViewHolder(itemView: View, private val onBookClicked: (Book) -> Unit) : RecyclerView.ViewHolder(itemView) {
        // Знаходимо всі елементи View, включаючи нову іконку
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)
        private val dateReadTextView: TextView = itemView.findViewById(R.id.text_view_date_read)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar_book)
        private val coverImageView: ImageView = itemView.findViewById(R.id.image_view_cover)
        // === НОВИЙ ЕЛЕМЕНТ: Іконка-індикатор ===
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.icon_favorite_indicator)

        private var currentBook: Book? = null

        init {
            // Обробник кліків на всю картку залишається без змін
            itemView.setOnClickListener {
                currentBook?.let { book ->
                    onBookClicked(book)
                }
            }
        }

        fun bind(book: Book) {
            // Зберігаємо поточну книгу
            currentBook = book
            titleTextView.text = book.title
            authorTextView.text = book.author

            // === НОВА ЛОГІКА: Показуємо або ховаємо іконку "вибране" ===
            if (book.isFavorite) {
                // Якщо книга у вибраному, робимо іконку видимою
                favoriteIcon.visibility = View.VISIBLE
            } else {
                // В іншому випадку - ховаємо її
                favoriteIcon.visibility = View.GONE
            }

            // Логіка для дати та рейтингу залишається, як ви пропонували раніше
            // (можна буде реалізувати в наступному кроці)
            if (book.dateRead != null && book.rating != null) {
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

            // Код для обкладинки залишається без змін
            if (book.coverImagePath != null) {
                Glide.with(itemView.context)
                    .load(File(book.coverImagePath))
                    .placeholder(R.color.black)
                    .into(coverImageView)
            } else {
                coverImageView.setImageResource(R.color.black)
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

class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem == newItem
    }
}