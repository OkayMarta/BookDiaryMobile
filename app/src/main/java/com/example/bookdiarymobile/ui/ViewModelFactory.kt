package com.example.bookdiarymobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bookdiarymobile.data.BookRepository

class ViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Залишаємо тільки ті ViewModel, які не потребують параметрів з навігації
        if (modelClass.isAssignableFrom(ReadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReadViewModel(repository) as T
        }

        if (modelClass.isAssignableFrom(ToReadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ToReadViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}