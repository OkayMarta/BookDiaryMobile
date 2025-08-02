package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

class BookDetailFragment : Fragment(R.layout.fragment_book_detail) {

    private val args: BookDetailFragmentArgs by navArgs()
    private val viewModel: BookDetailViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BookDetailViewModel::class.java)) {
                    val repository = (activity?.application as BookApplication).repository
                    @Suppress("UNCHECKED_CAST")
                    return BookDetailViewModel(repository, args.bookId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Знаходимо всі UI-елементи
        val titleTextView = view.findViewById<TextView>(R.id.detail_text_title)
        val authorTextView = view.findViewById<TextView>(R.id.detail_text_author)
        val descriptionTextView = view.findViewById<TextView>(R.id.detail_text_description)
        val ratingBar = view.findViewById<RatingBar>(R.id.detail_rating_bar)
        val coverImageView = view.findViewById<ImageView>(R.id.detail_image_cover)
        val favoriteButton = view.findViewById<ImageButton>(R.id.button_favorite)
        val moveToReadButton = view.findViewById<Button>(R.id.button_move_to_read) // Знаходимо кнопку
        val editButton = view.findViewById<Button>(R.id.button_edit)
        val deleteButton = view.findViewById<Button>(R.id.button_delete)
        val genreLabel = view.findViewById<TextView>(R.id.label_genre)
        val genreTextView = view.findViewById<TextView>(R.id.detail_text_genre)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.book.collect { book ->
                    // Передаємо нові елементи в функцію оновлення UI
                    updateUi(book, titleTextView, authorTextView, descriptionTextView, ratingBar, coverImageView, favoriteButton, moveToReadButton, genreLabel, genreTextView)
                }
            }
        }

        favoriteButton.setOnClickListener {
            viewModel.toggleFavoriteStatus()
        }

        // === ОБРОБНИК НАТИСКАННЯ ===
        moveToReadButton.setOnClickListener {
            // 1. Викликаємо метод ViewModel для зміни статусу книги в базі даних
            viewModel.markAsRead()

            // 2. Створюємо дію для переходу на екран редагування, як вимагає специфікація
            val action = BookDetailFragmentDirections
                .actionBookDetailFragmentToAddEditBookFragment(
                    bookId = viewModel.book.value.id, // Передаємо ID поточної книги
                    title = getString(R.string.title_update_read_book) // Передаємо новий заголовок
                )
            // 3. Виконуємо навігацію
            findNavController().navigate(action)
        }

        deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_delete_title))
                .setMessage(getString(R.string.dialog_delete_message))
                .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(getString(R.string.dialog_delete)) { dialog, _ ->
                    viewModel.deleteBook()
                    findNavController().navigateUp()
                }
                .show()
        }

        editButton.setOnClickListener {
            val action = BookDetailFragmentDirections
                .actionBookDetailFragmentToAddEditBookFragment(
                    bookId = viewModel.book.value.id,
                    title = "Edit Book"
                )
            findNavController().navigate(action)
        }
    }

    // Допоміжна функція для оновлення інтерфейсу (додали moveToReadButton)
    private fun updateUi(
        book: Book,
        titleTextView: TextView,
        authorTextView: TextView,
        descriptionTextView: TextView,
        ratingBar: RatingBar,
        coverImageView: ImageView,
        favoriteButton: ImageButton,
        moveToReadButton: Button,
        genreLabel: TextView,
        genreTextView: TextView
    ) {
        titleTextView.text = book.title
        authorTextView.text = book.author
        descriptionTextView.text = book.description

        if (book.status == BookStatus.TO_READ) {
            // Якщо книга ще не прочитана:
            moveToReadButton.visibility = View.VISIBLE
            ratingBar.visibility = View.GONE
            // === ГОЛОВНЕ ВИПРАВЛЕННЯ: Ховаємо кнопку "вибране" для книг TO_READ ===
            favoriteButton.visibility = View.GONE
        } else { // Для статусу READ
            // Якщо книга прочитана:
            moveToReadButton.visibility = View.GONE
            // Показуємо кнопку "вибране", щоб можна було керувати статусом
            favoriteButton.visibility = View.VISIBLE

            if (book.rating != null) {
                ratingBar.visibility = View.VISIBLE
                ratingBar.rating = book.rating.toFloat()
            } else {
                ratingBar.visibility = View.GONE
            }
        }

        // Код для обкладинки залишається без змін
        if (book.coverImagePath != null) {
            Glide.with(this)
                .load(File(book.coverImagePath))
                .placeholder(R.drawable.placeholder_cover) // Використовуємо placeholder з drawable
                .error(R.drawable.placeholder_cover)       // Показуємо placeholder, якщо сталася помилка завантаження
                .into(coverImageView)
        } else {
            // Встановлюємо placeholder, якщо шляху до обкладинки немає
            coverImageView.setImageResource(R.drawable.placeholder_cover)
        }

        // Логіка для іконки "вибране" тепер буде викликатись тільки для READ книг
        if (book.isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border)
        }

        if (book.genre.isNotBlank()) {
            // Якщо жанр вказано, робимо поля видимими та встановлюємо текст
            genreLabel.visibility = View.VISIBLE
            genreTextView.visibility = View.VISIBLE
            genreTextView.text = book.genre
        } else {
            // Якщо жанр не вказано, ховаємо поля
            genreLabel.visibility = View.GONE
            genreTextView.visibility = View.GONE
        }
    }
}