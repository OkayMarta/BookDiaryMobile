package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    // Ініціалізуємо ViewModel за допомогою нашої кастомної фабрики
    private val viewModel: FavoritesViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Знаходимо RecyclerView у нашому layout-файлі
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_favorites)

        // Створюємо екземпляр адаптера. Він універсальний і підходить для всіх списків.
        // В обробник кліку передаємо логіку навігації.
        val adapter = BookAdapter { clickedBook ->
            // Створюємо "дію" для переходу, використовуючи автозгенерований клас FavoritesFragmentDirections.
            // Передаємо ID книги, на яку натиснули.
            val action = FavoritesFragmentDirections.actionFavoritesFragmentToBookDetailFragment(
                bookId = clickedBook.id
            )
            // Виконуємо перехід на екран деталей книги
            findNavController().navigate(action)
        }

        // Встановлюємо адаптер для RecyclerView
        recyclerView.adapter = adapter

        // Запускаємо корутину для спостереження за змінами даних у ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            // Цей блок буде виконуватися, лише коли фрагмент видимий (STARTED)
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Підписуємося на потік 'favoriteBooks' з ViewModel
                viewModel.favoriteBooks.collect { books ->
                    // Коли надходить новий список книг, передаємо його в адаптер.
                    // ListAdapter сам ефективно визначить, що змінилося, і оновить RecyclerView.
                    adapter.submitList(books)
                }
            }
        }
    }
}