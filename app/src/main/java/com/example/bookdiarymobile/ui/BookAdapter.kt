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
import com.example.bookdiarymobile.data.BookStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookAdapter(
    private val onBookClicked: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    class BookViewHolder(itemView: View, private val onBookClicked: (Book) -> Unit) : RecyclerView.ViewHolder(itemView) {
        // --- 1. Оновлюємо посилання на елементи з нового layout ---
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.text_view_description)
        private val dateTextView: TextView = itemView.findViewById(R.id.text_view_date) // Новий ID
        private val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar_book)
        private val ratingTextView: TextView = itemView.findViewById(R.id.text_view_rating) // Новий елемент
        private val coverImageView: ImageView = itemView.findViewById(R.id.image_view_cover)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.icon_favorite_crown) // Новий ID

        private var currentBook: Book? = null

        init {
            itemView.setOnClickListener {
                currentBook?.let { book ->
                    onBookClicked(book)
                }
            }
        }

        fun bind(book: Book) {
            currentBook = book
            titleTextView.text = book.title
            authorTextView.text = book.author

            // Показуємо опис, якщо він не порожній
            if (book.description.isNotBlank()) {
                descriptionTextView.text = book.description
                descriptionTextView.visibility = View.VISIBLE
            } else {
                descriptionTextView.visibility = View.GONE
            }

            // Показуємо іконку-корону для вибраних книг
            favoriteIcon.visibility = if (book.isFavorite) View.VISIBLE else View.GONE

            if (book.status == BookStatus.READ) {
                // Якщо книга ПРОЧИТАНА, показуємо дату прочитання та рейтинг
                book.dateRead?.let {
                    dateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                    dateTextView.visibility = View.VISIBLE
                } ?: run {
                    dateTextView.visibility = View.GONE
                }

                book.rating?.let { ratingValue ->
                    // Для нового дизайну показуємо одну зірку і число поруч
                    ratingBar.rating = 1f // Завжди показуємо одну заповнену зірку
                    ratingTextView.text = ratingValue.toString()
                    ratingBar.visibility = View.VISIBLE
                    ratingTextView.visibility = View.VISIBLE
                } ?: run {
                    ratingBar.visibility = View.GONE
                    ratingTextView.visibility = View.GONE
                }
            } else { // Для статусу TO_READ
                // Якщо книга "ДО ПРОЧИТАННЯ", показуємо дату ДОДАВАННЯ і ховаємо рейтинг
                ratingBar.visibility = View.GONE
                ratingTextView.visibility = View.GONE

                dateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(book.dateAdded))
                dateTextView.visibility = View.VISIBLE
            }

            // --- 2. Виправляємо завантаження обкладинки ---
            // Створюємо placeholder, якщо його немає, і видаляємо дублювання коду.
            if (book.coverImagePath != null) {
                Glide.with(itemView.context)
                    .load(File(book.coverImagePath))
                    .placeholder(R.drawable.placeholder_cover) // Використовуємо новий placeholder
                    .into(coverImageView)
            } else {
                coverImageView.setImageResource(R.drawable.placeholder_cover)
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