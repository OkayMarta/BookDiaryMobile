// --- Оновіть файл: app/src/main/java/com/example/bookdiarymobile/ui/BookDetailFragment.kt ---

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

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
        val favoriteButton = view.findViewById<ImageButton>(R.id.button_favorite) // Знаходимо кнопку "сердечко"
        val editButton = view.findViewById<Button>(R.id.button_edit)
        val deleteButton = view.findViewById<Button>(R.id.button_delete)

        // Запускаємо спостереження за об'єктом книги з ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.book.collect { book ->
                    // Коли дані про книгу завантажаться, оновлюємо UI
                    updateUi(book, titleTextView, authorTextView, descriptionTextView, ratingBar, coverImageView, favoriteButton)
                }
            }
        }

        // Встановлюємо слухача натискання на кнопку "сердечко"
        favoriteButton.setOnClickListener {
            // При натисканні викликаємо відповідний метод у ViewModel
            viewModel.toggleFavoriteStatus()
        }


        // Обробник для кнопки "Delete"
        deleteButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_delete_title))
                .setMessage(getString(R.string.dialog_delete_message))
                .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.dialog_delete)) { dialog, _ ->
                    viewModel.deleteBook()
                    findNavController().navigateUp()
                }
                .show()
        }

        // Обробник для кнопки "Edit"
        editButton.setOnClickListener {
            val action = BookDetailFragmentDirections
                .actionBookDetailFragmentToAddEditBookFragment(
                    bookId = viewModel.book.value.id,
                    title = "Edit Book"
                )
            findNavController().navigate(action)
        }
    }

    // Допоміжна функція для оновлення інтерфейсу (додали нові параметри)
    private fun updateUi(
        book: Book,
        titleTextView: TextView,
        authorTextView: TextView,
        descriptionTextView: TextView,
        ratingBar: RatingBar,
        coverImageView: ImageView,
        favoriteButton: ImageButton
    ) {
        // Заповнюємо текстові поля
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

        // Завантажуємо обкладинку
        if (book.coverImagePath != null) {
            Glide.with(this)
                .load(File(book.coverImagePath))
                .placeholder(R.color.black)
                .into(coverImageView)
        } else {
            coverImageView.setImageResource(R.color.black)
        }

        // === НОВИЙ БЛОК: ОНОВЛЕННЯ ІКОНКИ "ВИБРАНЕ" ===
        // Перевіряємо статус isFavorite поточної книги
        if (book.isFavorite) {
            // Якщо книга у вибраному, встановлюємо заповнену іконку
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            // Інакше - встановлюємо порожню іконку
            favoriteButton.setImageResource(R.drawable.ic_favorite_border)
        }
    }
}