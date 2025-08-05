package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookDetailFragment : Fragment(R.layout.fragment_book_detail) {

    private val args: BookDetailFragmentArgs by navArgs()
    private val viewModel: BookDetailViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = (activity?.application as BookApplication).repository
                @Suppress("UNCHECKED_CAST")
                return BookDetailViewModel(repository, args.bookId) as T
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Знаходимо всі UI-елементи
        val titleTextView = view.findViewById<TextView>(R.id.detail_text_title)
        val authorTextView = view.findViewById<TextView>(R.id.detail_text_author)
        val genreTextView = view.findViewById<TextView>(R.id.detail_text_genre)
        val ratingTextView = view.findViewById<TextView>(R.id.detail_text_rating)
        val dateTextView = view.findViewById<TextView>(R.id.detail_text_date)
        val descriptionTextView = view.findViewById<TextView>(R.id.detail_text_description)
        val coverImageView = view.findViewById<ImageView>(R.id.detail_image_cover)

        val ratingLabel = view.findViewById<TextView>(R.id.label_rating)
        val ratingStarIcon = view.findViewById<ImageView>(R.id.icon_rating_star)

        val favoriteButton = view.findViewById<ImageButton>(R.id.button_favorite)
        val editButton = view.findViewById<ImageButton>(R.id.button_edit)
        val deleteButton = view.findViewById<ImageButton>(R.id.button_delete)
        val moveToReadButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_move_to_read)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.book.collectLatest { book ->
                    if (book.id == -1) return@collectLatest // Ігноруємо початковий стан

                    // === 1. Встановлюємо заголовок в Toolbar ===
                    val activity = requireActivity() as AppCompatActivity
                    val shortTitle = if (book.title.length > 15) "${book.title.take(15)}..." else book.title
                    activity.supportActionBar?.title = shortTitle

                    // === 2. Заповнюємо дані ===
                    titleTextView.text = book.title
                    authorTextView.text = book.author
                    genreTextView.text = book.genre

                    descriptionTextView.text = book.description
                    descriptionTextView.isVisible = book.description.isNotBlank()

                    // === 3. Логіка для статусів READ / TO_READ ===
                    if (book.status == BookStatus.READ) {
                        // Показуємо елементи для прочитаних книг
                        favoriteButton.isVisible = true
                        ratingLabel.isVisible = true
                        ratingStarIcon.isVisible = true
                        ratingTextView.isVisible = true
                        moveToReadButton.isVisible = false

                        book.rating?.let { ratingValue ->
                            ratingTextView.text = ratingValue.toString()

                            when (ratingValue) {
                                5, 4 -> ratingStarIcon.setImageResource(R.drawable.ic_star_filled)
                                3 -> ratingStarIcon.setImageResource(R.drawable.ic_star_half)
                                else -> ratingStarIcon.setImageResource(R.drawable.ic_star_outline)
                            }
                        }

                        book.dateRead?.let {
                            dateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                        }

                        // Оновлюємо стан кнопки "вибране" (приклад з візуалізацією)
                        favoriteButton.isSelected = book.isFavorite
                        val tint = if (book.isFavorite) ContextCompat.getColor(requireContext(), R.color.text_primary) else ContextCompat.getColor(requireContext(), R.color.text_secondary)
                        favoriteButton.setColorFilter(tint)

                    } else { // TO_READ
                        // Ховаємо елементи для прочитаних книг
                        favoriteButton.isVisible = false
                        ratingLabel.isVisible = false
                        ratingStarIcon.isVisible = false
                        ratingTextView.isVisible = false

                        // Показуємо кнопку "Прочитано"
                        moveToReadButton.isVisible = true

                        // Показуємо дату додавання
                        dateTextView.text = getString(R.string.added_on_date, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(book.dateAdded)))
                    }

                    // === 4. Завантажуємо обкладинку ===
                    if (book.coverImagePath != null) {
                        Glide.with(this@BookDetailFragment)
                            .load(book.coverImagePath)
                            .placeholder(R.drawable.placeholder_cover)
                            .into(coverImageView)
                    } else {
                        coverImageView.setImageResource(R.drawable.placeholder_cover)
                    }
                }
            }
        }

        // Обробники натискань
        favoriteButton.setOnClickListener { viewModel.toggleFavoriteStatus() }
        deleteButton.setOnClickListener { showDeleteDialog() }
        editButton.setOnClickListener { navigateToEdit() }
        moveToReadButton.setOnClickListener {
            viewModel.markAsRead()
            navigateToEdit(getString(R.string.title_update_read_book))
        }
    }

    private fun navigateToEdit(title: String = "Edit Book") {
        val action = BookDetailFragmentDirections
            .actionBookDetailFragmentToAddEditBookFragment(
                bookId = viewModel.book.value.id,
                title = title
            )
        findNavController().navigate(action)
    }

    private fun showDeleteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_message))
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .setPositiveButton(getString(R.string.dialog_delete)) { _, _ ->
                viewModel.deleteBook()
                findNavController().popBackStack()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Встановлюємо заголовок при поверненні на екран
        (activity as? AppCompatActivity)?.supportActionBar?.title = viewModel.book.value.let { book ->
            if (book.id != -1 && book.title.length > 15) "${book.title.take(15)}..." else if (book.id != -1) book.title else "Book Details"
        }
    }

    override fun onPause() {
        super.onPause()
        // Повертаємо головний заголовок при виході з екрану
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Book Diary"
    }
}