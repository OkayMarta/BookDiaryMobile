package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import kotlinx.coroutines.launch

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private val viewModel: StatsViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val totalTextView = view.findViewById<TextView>(R.id.stats_text_total)
        val yearTextView = view.findViewById<TextView>(R.id.stats_text_year)
        val monthTextView = view.findViewById<TextView>(R.id.stats_text_month)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stats.collect { stats ->
                    // Оновлюємо текст, використовуючи рядки з ресурсів
                    totalTextView.text = getString(R.string.stats_total_books, stats.totalBooks)
                    yearTextView.text = getString(R.string.stats_books_this_year, stats.booksThisYear)
                    monthTextView.text = getString(R.string.stats_books_this_month, stats.booksThisMonth)
                }
            }
        }
    }
}