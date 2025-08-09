package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

/**
 * Фрагмент для перегляду детальної інформації про книгу.
 *
 * Цей екран отримує `book_id` через навігаційні аргументи та підписується на
 * [BookDetailViewModel] для отримання та відображення повних даних про книгу.
 * Інтерфейс адаптується залежно від статусу книги ([BookStatus.READ] чи [BookStatus.TO_READ]).
 * Надає користувачеві можливість редагувати, видаляти, додавати книгу в улюблені
 * або переносити її у статус "Прочитано".
 */
@AndroidEntryPoint
class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    /** Навігаційні аргументи, що містять `book_id` обраної книги. */
    private val args: BookDetailFragmentArgs by navArgs()
    /** ViewModel, що керує логікою та станом цього екрану. */
    private val viewModel: BookDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Налаштовує спостереження за даними книги з ViewModel та ініціалізує обробники
     * натискань на кнопки.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Використовуємо collectLatest, щоб обробляти лише останні дані,
                // що корисно при швидких оновленнях.
                viewModel.book.collectLatest { book ->
                    // Захист від початкового стану ViewModel, коли дані ще не завантажені.
                    if (book.id == -1) return@collectLatest

                    updateToolbarTitle(book.title)
                    populateBookDetails(book)
                }
            }
        }
        setupClickListeners()
    }

    /**
     * Оновлює заголовок у кастомному Toolbar'і головної активності.
     * Обрізає довгі назви для кращого відображення.
     * @param title Назва книги.
     */
    private fun updateToolbarTitle(title: String) {
        val activity = requireActivity()
        val toolbarTitleTextView = activity.findViewById<TextView>(R.id.toolbar_title_text)
        val toolbarTitle = if (title.length > 20) {
            "${title.take(20)}..."
        } else {
            title
        }
        toolbarTitleTextView?.text = toolbarTitle
    }

    /**
     * Заповнює всі елементи UI даними з об'єкта [com.example.bookdiarymobile.data.Book].
     * @param book Об'єкт книги, дані якої потрібно відобразити.
     */
    private fun populateBookDetails(book: com.example.bookdiarymobile.data.Book) {
        binding.detailTextTitle.text = book.title
        binding.detailTextAuthor.text = book.author
        binding.detailTextGenre.text = book.genre

        binding.detailTextDescription.text = book.description
        binding.detailTextDescription.isVisible = book.description.isNotBlank()

        // Адаптація UI залежно від статусу книги
        if (book.status == BookStatus.READ) {
            setupReadStatusUI(book)
        } else { // TO_READ
            setupToReadStatusUI(book)
        }

        // Завантаження обкладинки
        loadCoverImage(book.coverImagePath)
    }

    /**
     * Налаштовує видимість та вміст елементів для книги зі статусом "Прочитано".
     */
    private fun setupReadStatusUI(book: com.example.bookdiarymobile.data.Book) {
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

        updateFavoriteButtonState(book.isFavorite)
    }

    /**
     * Налаштовує видимість та вміст елементів для книги зі статусом "Хочу прочитати".
     */
    private fun setupToReadStatusUI(book: com.example.bookdiarymobile.data.Book) {
        binding.buttonFavorite.isVisible = false
        binding.labelRating.isVisible = false
        binding.iconRatingStar.isVisible = false
        binding.detailTextRating.isVisible = false
        binding.buttonMoveToRead.isVisible = true
        binding.detailTextDate.text = getString(R.string.added_on_date, SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(book.dateAdded)))
    }

    /**
     * Оновлює візуальний стан кнопки "Улюблене" (колір іконки).
     * @param isFavorite `true`, якщо книга є улюбленою.
     */
    private fun updateFavoriteButtonState(isFavorite: Boolean) {
        binding.buttonFavorite.isSelected = isFavorite
        val tint = if (isFavorite) ContextCompat.getColor(requireContext(), R.color.text_primary) else ContextCompat.getColor(requireContext(), R.color.text_secondary)
        binding.buttonFavorite.setColorFilter(tint)
    }

    /**
     * Завантажує зображення обкладинки за допомогою Glide.
     * @param coverPath Шлях до файлу обкладинки або null.
     */
    private fun loadCoverImage(coverPath: String?) {
        if (coverPath != null) {
            Glide.with(this@BookDetailFragment)
                .load(coverPath)
                .placeholder(R.drawable.placeholder_cover)
                .into(binding.detailImageCover)
        } else {
            binding.detailImageCover.setImageResource(R.drawable.placeholder_cover_sharp)
        }
    }

    /**
     * Налаштовує обробники натискань для всіх кнопок на екрані.
     */
    private fun setupClickListeners() {
        binding.buttonFavorite.setOnClickListener { viewModel.toggleFavoriteStatus() }
        binding.buttonDelete.setOnClickListener { showDeleteDialog() }
        binding.buttonEdit.setOnClickListener { navigateToEdit() }
        binding.buttonMoveToRead.setOnClickListener {
            val action = BookDetailFragmentDirections
                .actionBookDetailFragmentToAddEditBookFragment(
                    bookId = viewModel.book.value.id,
                    title = getString(R.string.title_update_read_book),
                    isTransitioningToRead = true
                )
            findNavController().navigate(action)
        }
    }

    /**
     * Створює та виконує дію навігації до екрану редагування [AddEditBookFragment].
     * @param title Заголовок для екрану редагування.
     */
    private fun navigateToEdit(title: String = "Edit Book") {
        val action = BookDetailFragmentDirections
            .actionBookDetailFragmentToAddEditBookFragment(
                bookId = viewModel.book.value.id,
                title = title
            )
        findNavController().navigate(action)
    }

    /**
     * Відображає діалогове вікно для підтвердження видалення книги.
     * При підтвердженні викликає метод видалення у ViewModel та повертається
     * на попередній екран.
     */
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
        _binding = null
    }
}