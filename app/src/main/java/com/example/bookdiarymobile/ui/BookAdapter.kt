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
        private val dateTextView: TextView = itemView.findViewById(R.id.text_view_date_read)
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

            // Логіка для іконки "вибране" залишається без змін
            if (book.isFavorite && book.status == BookStatus.READ) {
                favoriteIcon.visibility = View.VISIBLE
            } else {
                favoriteIcon.visibility = View.GONE
            }

            // === ОНОВЛЕНА ЛОГІКА ДЛЯ ВІДОБРАЖЕННЯ ДАТИ ===
            if (book.status == BookStatus.READ) {
                // Якщо книга ПРОЧИТАНА, показуємо дату прочитання та рейтинг
                book.dateRead?.let {
                    val formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                    dateTextView.text = itemView.context.getString(R.string.read_on_date, formattedDate)
                    dateTextView.visibility = View.VISIBLE
                } ?: run {
                    dateTextView.visibility = View.GONE // Ховаємо, якщо дати прочитання чомусь немає
                }

                book.rating?.let {
                    ratingBar.rating = it.toFloat()
                    ratingBar.visibility = View.VISIBLE
                } ?: run {
                    ratingBar.visibility = View.GONE // Ховаємо, якщо рейтингу немає
                }

            } else { // Для статусу TO_READ
                // Якщо книга "ДО ПРОЧИТАННЯ", ховаємо рейтинг і показуємо дату ДОДАВАННЯ
                ratingBar.visibility = View.GONE

                val formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(book.dateAdded))
                // Використовуємо новий рядок з ресурсів для правильного форматування
                dateTextView.text = itemView.context.getString(R.string.added_on_date, formattedDate)
                dateTextView.visibility = View.VISIBLE
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