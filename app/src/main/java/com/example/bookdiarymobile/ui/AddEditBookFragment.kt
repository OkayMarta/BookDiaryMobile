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

    // Лаунчер для вибору зображення з галереї
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                // Показуємо вибране зображення
                val coverImageView = view?.findViewById<ImageView>(R.id.image_view_add_cover)
                coverImageView?.let { iv -> Glide.with(this).load(it).into(iv) }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleEditText = view.findViewById<TextInputEditText>(R.id.edit_text_title)
        val authorEditText = view.findViewById<TextInputEditText>(R.id.edit_text_author)
        val descriptionEditText = view.findViewById<TextInputEditText>(R.id.edit_text_description)
        val saveFab = view.findViewById<FloatingActionButton>(R.id.fab_save)
        val coverImageView = view.findViewById<ImageView>(R.id.image_view_add_cover)
        val addCoverButton = view.findViewById<Button>(R.id.button_add_cover)

        addCoverButton.setOnClickListener {
            imagePickerLauncher.launch("image/*") // Запускаємо галерею
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { book ->
                    if (book != null) {
                        currentCoverPath = book.coverImagePath
                        if (titleEditText.text.toString() != book.title) titleEditText.setText(book.title)
                        if (authorEditText.text.toString() != book.author) authorEditText.setText(book.author)
                        if (descriptionEditText.text.toString() != book.description) descriptionEditText.setText(book.description)

                        // Завантажуємо існуючу обкладинку, якщо вона є
                        book.coverImagePath?.let { path ->
                            if (selectedImageUri == null) { // Показуємо, тільки якщо не вибрано нову
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

            if (title.isBlank() || author.isBlank()) {
                Toast.makeText(context, "Title and Author cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Копіюємо зображення, якщо було вибрано нове
            val newCoverPath = selectedImageUri?.let { uri ->
                copyImageToInternalStorage(uri)
            } ?: currentCoverPath // Якщо нове не вибрано, залишаємо старе

            viewModel.saveBook(title, author, description, newCoverPath)
            findNavController().navigateUp()
        }
    }

    // Функція для копіювання файлу у внутрішнє сховище
    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val coversDir = File(requireContext().filesDir, "covers")
            if (!coversDir.exists()) coversDir.mkdirs()
            val file = File(coversDir, "${System.currentTimeMillis()}.jpg")
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