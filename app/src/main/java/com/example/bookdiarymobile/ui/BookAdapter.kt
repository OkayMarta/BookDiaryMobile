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

                // === ЛОГІКА ДЛЯ ДИНАМІЧНОЇ ІКОНКИ ЗІРКИ ===
                book.rating?.let { ratingValue ->
                    ratingTextView.text = ratingValue.toString()
                    ratingTextView.visibility = View.VISIBLE
                    ratingStarIcon.visibility = View.VISIBLE

                    // Вибираємо іконку залежно від рейтингу
                    when (ratingValue) {
                        5, 4 -> ratingStarIcon.setImageResource(R.drawable.ic_star_filled)
                        3 -> ratingStarIcon.setImageResource(R.drawable.ic_star_half)
                        2, 1 -> ratingStarIcon.setImageResource(R.drawable.ic_star_outline)
                        else -> ratingStarIcon.visibility = View.GONE // Ховаємо, якщо рейтинг 0 або інший
                    }
                } ?: run {
                    // Ховаємо рейтинг, якщо його немає
                    ratingStarIcon.visibility = View.GONE
                    ratingTextView.visibility = View.GONE
                }
            } else { // Для статусу TO_READ
                // Ховаємо рейтинг
                ratingStarIcon.visibility = View.GONE
                ratingTextView.visibility = View.GONE

                // Показуємо дату додавання
                dateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(book.dateAdded))
                dateTextView.visibility = View.VISIBLE
            }

            // Логіка для обкладинки
            if (book.coverImagePath != null) {
                Glide.with(itemView.context)
                    .load(File(book.coverImagePath))
                    .placeholder(R.drawable.placeholder_cover)
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