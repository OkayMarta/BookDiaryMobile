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
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)
        private val dateReadTextView: TextView = itemView.findViewById(R.id.text_view_date_read)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar_book)
        private val coverImageView: ImageView = itemView.findViewById(R.id.image_view_cover)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.icon_favorite_indicator)

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

            // === Показуємо іконку тільки для прочитаних та вибраних книг ===
            // Згідно зі специфікацією, isFavorite може бути true тільки для статусу READ.
            // Ця перевірка гарантує, що сердечко не з'явиться на книгах зі списку "To Read".
            if (book.isFavorite && book.status == BookStatus.READ) {
                favoriteIcon.visibility = View.VISIBLE
            } else {
                favoriteIcon.visibility = View.GONE
            }

            // Логіка для дати та рейтингу (включаючи вашу пропозицію для дати додавання)
            if (book.status == BookStatus.READ) {
                // Якщо книга прочитана, показуємо дату прочитання та рейтинг
                book.dateRead?.let {
                    val formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                    dateReadTextView.text = itemView.context.getString(R.string.read_on_date, formattedDate)
                    dateReadTextView.visibility = View.VISIBLE
                } ?: run {
                    dateReadTextView.visibility = View.GONE
                }

                book.rating?.let {
                    ratingBar.rating = it.toFloat()
                    ratingBar.visibility = View.VISIBLE
                } ?: run {
                    ratingBar.visibility = View.GONE
                }
            } else { // Для статусу TO_READ
                // Якщо книга "До прочитання", ховаємо рейтинг і показуємо дату додавання
                ratingBar.visibility = View.GONE
                val formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(book.dateAdded))
                // Можна створити новий рядок "Added: %1$s" у strings.xml для кращого вигляду
                dateReadTextView.text = "Added: $formattedDate" // Тимчасове рішення
                dateReadTextView.visibility = View.VISIBLE
            }

            // Код для обкладинки
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