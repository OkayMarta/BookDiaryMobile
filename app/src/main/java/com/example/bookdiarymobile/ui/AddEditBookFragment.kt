package com.example.bookdiarymobile.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
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
import java.util.UUID

/**
 * Фрагмент, що відповідає за екран додавання нової книги або редагування існуючої.
 * Працює у двох режимах: створення та редагування.
 */
class AddEditBookFragment : Fragment(R.layout.fragment_add_edit_book) {

    /**
     * Отримує аргументи, передані через Navigation Component (bookId, bookStatus, title).
     */
    private val navArgs: AddEditBookFragmentArgs by navArgs()

    /**
     * Ініціалізує ViewModel за допомогою кастомної фабрики, передаючи їй репозиторій
     * та необхідні аргументи з navArgs для визначення режиму роботи.
     */
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

    /**
     * Зберігає URI щойно вибраного зображення з галереї.
     * Якщо null, значить нове зображення не вибирали.
     */
    private var selectedImageUri: Uri? = null

    /**
     * Зберігає шлях до поточної обкладинки книги (для режиму редагування).
     */
    private var currentCoverPath: String? = null

    /**
     * ActivityResultLauncher для запуску системної галереї для вибору зображення.
     * Після вибору зображення, його URI зберігається у selectedImageUri та відображається на екрані.
     */
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

        // Ініціалізація всіх UI-елементів на екрані.
        val titleEditText = view.findViewById<TextInputEditText>(R.id.edit_text_title)
        val authorEditText = view.findViewById<TextInputEditText>(R.id.edit_text_author)
        val descriptionEditText = view.findViewById<TextInputEditText>(R.id.edit_text_description)
        val saveFab = view.findViewById<FloatingActionButton>(R.id.fab_save)
        val coverImageView = view.findViewById<ImageView>(R.id.image_view_add_cover)
        val addCoverButton = view.findViewById<Button>(R.id.button_add_cover)

        // Встановлення слухача на кнопку вибору обкладинки.
        addCoverButton.setOnClickListener {
            imagePickerLauncher.launch("image/*") // Запуск галереї для вибору файлів типу "image".
        }

        // Спостереження за станом книги з ViewModel для заповнення полів у режимі редагування.
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { book ->
                    if (book != null) {
                        currentCoverPath = book.coverImagePath
                        // Заповнення полів даними, якщо вони ще не були змінені користувачем.
                        if (titleEditText.text.toString() != book.title) titleEditText.setText(book.title)
                        if (authorEditText.text.toString() != book.author) authorEditText.setText(book.author)
                        if (descriptionEditText.text.toString() != book.description) descriptionEditText.setText(book.description)

                        // Завантаження існуючої обкладинки, якщо вона є і нову ще не вибрали.
                        book.coverImagePath?.let { path ->
                            if (selectedImageUri == null) {
                                Glide.with(this@AddEditBookFragment).load(File(path)).into(coverImageView)
                            }
                        }
                    }
                }
            }
        }

        // Встановлення слухача на кнопку збереження.
        saveFab.setOnClickListener {
            val title = titleEditText.text.toString()
            val author = authorEditText.text.toString()
            val description = descriptionEditText.text.toString()

            // Перевірка, чи заповнені обов'язкові поля.
            if (title.isBlank() || author.isBlank()) {
                Toast.makeText(context, "Title and Author cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Визначення шляху до обкладинки: якщо вибрано нове зображення — копіюємо його;
            // інакше — залишаємо поточний шлях.
            val newCoverPath = selectedImageUri?.let { uri ->
                copyImageToInternalStorage(uri)
            } ?: currentCoverPath

            // Виклик методу збереження у ViewModel та повернення на попередній екран.
            viewModel.saveBook(title, author, description, newCoverPath)
            findNavController().navigateUp()
        }
    }

    /**
     * Копіює файл зображення з наданого URI у внутрішнє сховище додатку.
     * Це необхідно, оскільки доступ до URI з галереї може бути втрачено.
     */
    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val coversDir = File(requireContext().filesDir, "covers")
            if (!coversDir.exists()) coversDir.mkdirs() // Створення папки "covers", якщо її немає.

            // Генерація унікального імені файлу, щоб уникнути перезапису існуючих обкладинок.
            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(coversDir, fileName)

            // Копіювання даних із вхідного потоку у вихідний (у новий файл).
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath // Повернення повного шляху до створеного файлу.
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}