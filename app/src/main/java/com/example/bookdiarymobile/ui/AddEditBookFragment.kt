package com.example.bookdiarymobile.ui

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.bookdiarymobile.data.BookStatus
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddEditBookFragment : Fragment(R.layout.fragment_add_edit_book) {

    private val navArgs: AddEditBookFragmentArgs by navArgs()
    private val viewModel: AddEditBookViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = (requireActivity().application as BookApplication).repository
                val bookId = navArgs.bookId
                val statusString = navArgs.bookStatus
                val bookStatus = statusString?.let { BookStatus.valueOf(it) }

                @Suppress("UNCHECKED_CAST")
                return AddEditBookViewModel(repository, bookId, bookStatus) as T
            }
        }
    }

    private var selectedImageUri: Uri? = null
    private var currentCoverPath: String? = null
    private var selectedDateInMillis: Long? = null
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                val coverImageView = view?.findViewById<ImageView>(R.id.image_view_add_cover)
                coverImageView?.let { iv -> Glide.with(this).load(it).into(iv) }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ініціалізація UI-елементів
        val titleEditText = view.findViewById<TextInputEditText>(R.id.edit_text_title)
        val authorEditText = view.findViewById<TextInputEditText>(R.id.edit_text_author)
        val descriptionEditText = view.findViewById<TextInputEditText>(R.id.edit_text_description)
        val saveFab = view.findViewById<FloatingActionButton>(R.id.fab_save)
        val coverImageView = view.findViewById<ImageView>(R.id.image_view_add_cover)
        val addCoverButton = view.findViewById<Button>(R.id.button_add_cover)

        // === НОВІ UI-ЕЛЕМЕНТИ ===
        val readDetailsLayout = view.findViewById<LinearLayout>(R.id.read_details_layout)
        val dateEditText = view.findViewById<TextInputEditText>(R.id.edit_text_date_read)
        val ratingBar = view.findViewById<RatingBar>(R.id.rating_bar_edit)

        // === ІНІЦІАЛІЗАЦІЯ SPINNER ===
        val genreSpinner = view.findViewById<Spinner>(R.id.spinner_genre)
        // Створюємо адаптер для спіннера, використовуючи масив з strings.xml
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.book_genres,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            genreSpinner.adapter = adapter
        }

        addCoverButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // === ОБРОБНИК НАТИСКАННЯ НА ПОЛЕ ДАТИ ===
        dateEditText.setOnClickListener {
            showDatePickerDialog(dateEditText)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { book ->
                    // Визначаємо, чи маємо справу з новою книгою чи редагуємо існуючу
                    val currentBook = book ?: // Якщо book == null, створюємо "порожню" книгу
                    // Це потрібно, щоб визначити видимість полів для нових READ книг.
                    com.example.bookdiarymobile.data.Book(
                        id = -1, title = "", author = "", genre = "", description = "",
                        coverImagePath = null, status = navArgs.bookStatus?.let { BookStatus.valueOf(it) } ?: BookStatus.TO_READ,
                        dateAdded = 0L, dateRead = null, rating = null
                    )

                    // === ЛОГІКА ВИДИМОСТІ ПОЛІВ ДАТИ ТА РЕЙТИНГУ ===
                    if (currentBook.status == BookStatus.READ) {
                        readDetailsLayout.visibility = View.VISIBLE
                    } else {
                        readDetailsLayout.visibility = View.GONE
                    }

                    // Заповнюємо поля, якщо це режим редагування
                    book?.let {
                        currentCoverPath = it.coverImagePath
                        if (titleEditText.text.toString() != it.title) titleEditText.setText(it.title)
                        if (authorEditText.text.toString() != it.author) authorEditText.setText(it.author)
                        if (descriptionEditText.text.toString() != it.description) descriptionEditText.setText(it.description)

                        // Заповнюємо дату та рейтинг, якщо вони є
                        it.dateRead?.let { date ->
                            selectedDateInMillis = date
                            dateEditText.setText(formatDate(date))
                        }
                        it.rating?.let { rating ->
                            ratingBar.rating = rating.toFloat()
                        }

                        // === ВСТАНОВЛЕННЯ ПОЧАТКОВОГО ЗНАЧЕННЯ ДЛЯ SPINNER ===
                        val genres = resources.getStringArray(R.array.book_genres)
                        val genrePosition = genres.indexOf(it.genre)
                        // Якщо жанр знайдено, встановлюємо його, інакше - позицію 0 ("Оберіть жанр")
                        genreSpinner.setSelection(if (genrePosition >= 0) genrePosition else 0)

                        it.coverImagePath?.let { path ->
                            if (selectedImageUri == null) {
                                Glide.with(this@AddEditBookFragment).load(File(path)).into(coverImageView)
                            }
                        }
                    }
                }
            }
        }

        saveFab.setOnClickListener {
            val title = titleEditText.text.toString()
            val author = authorEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val ratingValue = ratingBar.rating.toInt()

            // --- ЛОГІКА ОТРИМАННЯ ТА ВАЛІДАЦІЇ ЖАНРУ ---
            val genreSpinner = view.findViewById<Spinner>(R.id.spinner_genre) // Отримуємо Spinner

            // Перевіряємо, чи вибрано перший елемент ("Select genre")
            if (genreSpinner.selectedItemPosition == 0) {
                Toast.makeText(context, R.string.validation_select_genre, Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Перериваємо виконання, якщо жанр не вибрано
            }

            val selectedGenre = genreSpinner.selectedItem.toString()

            if (title.isBlank() || author.isBlank()) {
                Toast.makeText(context, "Title and Author cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newCoverPath = selectedImageUri?.let { uri ->
                copyImageToInternalStorage(uri)
            } ?: currentCoverPath

            // Передаємо нові дані у ViewModel, тепер `selectedGenre` не буде "Select genre"
            viewModel.saveBook(title, author, description, newCoverPath, selectedDateInMillis, ratingValue, selectedGenre)
            findNavController().navigateUp()
        }
    }

    /**
     * Показує системний діалог вибору дати.
     */
    private fun showDatePickerDialog(dateEditText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        // Якщо дата вже була вибрана, встановлюємо її в діалозі
        selectedDateInMillis?.let {
            calendar.timeInMillis = it
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                selectedDateInMillis = selectedCalendar.timeInMillis
                dateEditText.setText(formatDate(selectedDateInMillis!!))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    /**
     * Форматує timestamp у рядок "дд.ММ.рррр".
     */
    private fun formatDate(millis: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val coversDir = File(requireContext().filesDir, "covers")
            if (!coversDir.exists()) coversDir.mkdirs()

            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(coversDir, fileName)

            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}