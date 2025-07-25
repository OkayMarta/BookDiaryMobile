package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.BookStatus
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleEditText = view.findViewById<TextInputEditText>(R.id.edit_text_title)
        val authorEditText = view.findViewById<TextInputEditText>(R.id.edit_text_author)
        val descriptionEditText = view.findViewById<TextInputEditText>(R.id.edit_text_description)
        val saveFab = view.findViewById<FloatingActionButton>(R.id.fab_save)

        // Спостерігаємо за станом книги у ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // === ВИПРАВЛЕНО ТУТ: bookState -> uiState ===
                viewModel.uiState.collect { book ->
                    // Якщо книга завантажилась (в режимі редагування), заповнюємо поля
                    if (book != null) {
                        // Щоб уникнути зациклення, перевіряємо, чи текст вже такий самий
                        if (titleEditText.text.toString() != book.title) {
                            titleEditText.setText(book.title)
                        }
                        if (authorEditText.text.toString() != book.author) {
                            authorEditText.setText(book.author)
                        }
                        if (descriptionEditText.text.toString() != book.description) {
                            descriptionEditText.setText(book.description)
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

            viewModel.saveBook(title, author, description)
            // Повертаємося на попередній екран
            findNavController().navigateUp()
        }
    }
}