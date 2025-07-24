package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.BookStatus
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class AddEditBookFragment : Fragment(R.layout.fragment_add_edit_book) {

    private val viewModel: AddEditBookViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    // Ліниво отримуємо аргументи, передані через навігацію
    private val navArgs: AddEditBookFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleEditText = view.findViewById<TextInputEditText>(R.id.edit_text_title)
        val authorEditText = view.findViewById<TextInputEditText>(R.id.edit_text_author)
        val descriptionEditText = view.findViewById<TextInputEditText>(R.id.edit_text_description)
        val saveFab = view.findViewById<FloatingActionButton>(R.id.fab_save)

        // Зчитуємо статус з аргументів
        val bookStatusString = navArgs.bookStatus
        val bookStatus = BookStatus.valueOf(bookStatusString) // Перетворюємо рядок "READ" на об'єкт BookStatus.READ

        saveFab.setOnClickListener {
            val title = titleEditText.text.toString()
            val author = authorEditText.text.toString()
            val description = descriptionEditText.text.toString()

            if (title.isBlank() || author.isBlank()) {
                Toast.makeText(context, "Title and Author cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Передаємо отриманий статус у ViewModel
            viewModel.saveBook(title, author, description, bookStatus)

            findNavController().navigateUp()
        }
    }
}