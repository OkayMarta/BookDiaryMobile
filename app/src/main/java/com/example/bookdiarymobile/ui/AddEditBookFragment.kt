package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class AddEditBookFragment : Fragment(R.layout.fragment_add_edit_book) {

    // Створюємо ViewModel за допомогою нашої фабрики
    private val viewModel: AddEditBookViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Знаходимо наші поля для вводу та кнопку
        val titleEditText = view.findViewById<TextInputEditText>(R.id.edit_text_title)
        val authorEditText = view.findViewById<TextInputEditText>(R.id.edit_text_author)
        val descriptionEditText = view.findViewById<TextInputEditText>(R.id.edit_text_description)
        val saveFab = view.findViewById<FloatingActionButton>(R.id.fab_save)

        // Встановлюємо слухача на кнопку збереження
        saveFab.setOnClickListener {
            val title = titleEditText.text.toString()
            val author = authorEditText.text.toString()
            val description = descriptionEditText.text.toString()

            // Проста перевірка, чи заповнені основні поля
            if (title.isBlank() || author.isBlank()) {
                Toast.makeText(context, "Title and Author cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Викликаємо функцію ViewModel для збереження
            viewModel.saveBook(title, author, description)

            // Повертаємось на попередній екран
            findNavController().navigateUp()
        }
    }
}