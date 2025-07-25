package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.Book
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookDetailFragment : Fragment(R.layout.fragment_book_detail) {

    // Отримуємо аргументи навігації (book_id)
    private val args: BookDetailFragmentArgs by navArgs()

    // Створюємо ViewModel за допомогою спеціальної фабрики
    private val viewModel: BookDetailViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BookDetailViewModel::class.java)) {
                    val repository = (activity?.application as BookApplication).repository
                    @Suppress("UNCHECKED_CAST")
                    // Передаємо ID книги прямо в конструктор ViewModel
                    return BookDetailViewModel(repository, args.bookId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Знаходимо всі елементи на макеті
        val titleTextView = view.findViewById<TextView>(R.id.detail_text_title)
        val authorTextView = view.findViewById<TextView>(R.id.detail_text_author)
        val descriptionTextView = view.findViewById<TextView>(R.id.detail_text_description)
        val ratingBar = view.findViewById<RatingBar>(R.id.detail_rating_bar)
        val coverImageView = view.findViewById<ImageView>(R.id.detail_image_cover)
        val editButton = view.findViewById<Button>(R.id.button_edit)
        val deleteButton = view.findViewById<Button>(R.id.button_delete)

        // Запускаємо спостереження за об'єктом книги з ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.book.collect { book ->
                    // Коли дані про книгу завантажаться, оновлюємо UI
                    updateUi(book, titleTextView, authorTextView, descriptionTextView, ratingBar)
                }
            }
        }

        // Обробник для кнопки "Delete"
        deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_delete_title)) // "Delete Book"
                .setMessage(getString(R.string.dialog_delete_message)) // "Are you sure you want to delete this book? This action cannot be undone."
                .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                    // Нічого не робимо, просто закриваємо діалог
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.dialog_delete)) { dialog, _ ->
                    // Викликаємо функцію видалення у ViewModel
                    viewModel.deleteBook()
                    // Повертаємось на попередній екран
                    findNavController().navigateUp()
                }
                .show()
        }

        // Обробник для кнопки "Edit"
        editButton.setOnClickListener {
            // Створюємо дію для переходу, передаючи ID поточної книги
            val action = BookDetailFragmentDirections
                .actionBookDetailFragmentToAddEditBookFragment(
                    bookId = viewModel.book.value.id, // Беремо ID з ViewModel
                    title = "Edit Book" // Передаємо заголовок для екрану
                )
            findNavController().navigate(action)
        }
    }

    // Допоміжна функція для оновлення інтерфейсу
    private fun updateUi(
        book: Book,
        titleTextView: TextView,
        authorTextView: TextView,
        descriptionTextView: TextView,
        ratingBar: RatingBar
    ) {
        titleTextView.text = book.title
        authorTextView.text = book.author
        descriptionTextView.text = book.description

        // Показуємо або ховаємо рейтинг в залежності від статусу
        if (book.rating != null) {
            ratingBar.visibility = View.VISIBLE
            ratingBar.rating = book.rating.toFloat()
        } else {
            ratingBar.visibility = View.GONE
        }

        // --- КОД ДЛЯ ОБКЛАДИНКИ ---
        val coverImageView = view?.findViewById<ImageView>(R.id.detail_image_cover)
        if (book.coverImagePath != null) {
            coverImageView?.let {
                Glide.with(this)
                    .load(File(book.coverImagePath))
                    .placeholder(R.color.black)
                    .into(it)
            }
        } else {
            coverImageView?.setImageResource(R.color.black)
        }
    }
}