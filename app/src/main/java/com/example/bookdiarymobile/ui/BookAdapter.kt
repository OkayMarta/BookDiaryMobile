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

// Використовуємо ListAdapter для ефективного оновлення списку
class BookAdapter : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    // Цей клас описує одну картку книги та знаходить елементи всередині неї
    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.text_view_title)
        private val authorTextView: TextView = itemView.findViewById(R.id.text_view_author)
        private val dateReadTextView: TextView = itemView.findViewById(R.id.text_view_date_read)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar_book)
        private val coverImageView: ImageView = itemView.findViewById(R.id.image_view_cover)

        // Функція для заповнення картки даними з об'єкта Book
        fun bind(book: Book) {
            titleTextView.text = book.title
            authorTextView.text = book.author
            ratingBar.rating = book.rating?.toFloat() ?: 0f

            // Форматуємо дату для гарного відображення
            book.dateRead?.let {
                val formattedDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                dateReadTextView.text = itemView.context.getString(R.string.read_on_date, formattedDate)
            }

            // Тут пізніше буде код для завантаження обкладинки
            // coverImageView. ...
        }
    }

    // Створює новий ViewHolder (картку), коли RecyclerView це потрібно
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_book, parent, false)
        return BookViewHolder(view)
    }

    // Заповнює існуючий ViewHolder даними для конкретної позиції у списку
    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book)
    }
}

// Цей клас допомагає ListAdapter зрозуміти, які елементи у списку змінилися,
// щоб оновлювати тільки їх, а не весь список.
class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem == newItem
    }
}