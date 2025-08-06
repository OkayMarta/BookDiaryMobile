package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.BookStatus
import com.example.bookdiarymobile.databinding.FragmentBookDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private val args: BookDetailFragmentArgs by navArgs()
    private val viewModel: BookDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.book.collectLatest { book ->
                    if (book.id == -1) return@collectLatest

                    // === 1. Встановлюємо заголовок в Toolbar ===
                    val activity = requireActivity()
                    val toolbarTitleTextView = activity.findViewById<TextView>(R.id.toolbar_title_text)
                    val toolbarTitle = if (book.title.length > 20) {
                        "${book.title.take(20)}..."
                    } else {
                        book.title
                    }
                    // РІШЕННЯ 2: Використовуємо безпечний виклик `?.`
                    toolbarTitleTextView?.text = toolbarTitle


                    // === 2. Заповнюємо дані через binding ===
                    binding.detailTextTitle.text = book.title
                    binding.detailTextAuthor.text = book.author
                    binding.detailTextGenre.text = book.genre

                    binding.detailTextDescription.text = book.description
                    binding.detailTextDescription.isVisible = book.description.isNotBlank()

                    // === 3. Логіка для статусів READ / TO_READ ===
                    if (book.status == BookStatus.READ) {
                        // Показуємо елементи для прочитаних книг
                        binding.buttonFavorite.isVisible = true
                        binding.labelRating.isVisible = true
                        binding.iconRatingStar.isVisible = true
                        binding.detailTextRating.isVisible = true
                        binding.buttonMoveToRead.isVisible = false

                        book.rating?.let { ratingValue ->
                            binding.detailTextRating.text = ratingValue.toString()

                            when (ratingValue) {
                                5, 4 -> binding.iconRatingStar.setImageResource(R.drawable.ic_star_filled)
                                3 -> binding.iconRatingStar.setImageResource(R.drawable.ic_star_half)
                                else -> binding.iconRatingStar.setImageResource(R.drawable.ic_star_outline)
                            }
                        }

                        book.dateRead?.let {
                            binding.detailTextDate.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                        }

                        // Оновлюємо стан кнопки "вибране"
                        binding.buttonFavorite.isSelected = book.isFavorite
                        val tint = if (book.isFavorite) ContextCompat.getColor(requireContext(), R.color.text_primary) else ContextCompat.getColor(requireContext(), R.color.text_secondary)
                        binding.buttonFavorite.setColorFilter(tint)

                    } else { // TO_READ
                        // Ховаємо елементи для прочитаних книг
                        binding.buttonFavorite.isVisible = false
                        binding.labelRating.isVisible = false
                        binding.iconRatingStar.isVisible = false
                        binding.detailTextRating.isVisible = false

                        // Показуємо кнопку "Прочитано"
                        binding.buttonMoveToRead.isVisible = true

                        // Показуємо дату додавання
                        binding.detailTextDate.text = getString(R.string.added_on_date, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(book.dateAdded)))
                    }

                    // === 4. Завантажуємо обкладинку ===
                    if (book.coverImagePath != null) {
                        Glide.with(this@BookDetailFragment)
                            .load(book.coverImagePath)
                            .placeholder(R.drawable.placeholder_cover)
                            .into(binding.detailImageCover)
                    } else {
                        binding.detailImageCover.setImageResource(R.drawable.placeholder_cover_sharp)
                    }
                }
            }
        }

        // Обробники натискань
        binding.buttonFavorite.setOnClickListener { viewModel.toggleFavoriteStatus() }
        binding.buttonDelete.setOnClickListener { showDeleteDialog() }
        binding.buttonEdit.setOnClickListener { navigateToEdit() }
        binding.buttonMoveToRead.setOnClickListener {
            // Переходимо на екран редагування, передаючи прапорець
            val action = BookDetailFragmentDirections
                .actionBookDetailFragmentToAddEditBookFragment(
                    bookId = viewModel.book.value.id,
                    title = getString(R.string.title_update_read_book),
                    isTransitioningToRead = true
                )
            findNavController().navigate(action)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очищуємо binding
    }
}