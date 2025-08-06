package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class StatsFragment : Fragment(R.layout.fragment_stats) {

    private val viewModel: StatsViewModel by viewModels()

    private lateinit var months: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Завантажуємо назви місяців один раз при створенні фрагмента
        months = resources.getStringArray(R.array.months)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val periodSelector = view.findViewById<LinearLayout>(R.id.period_selector)
        val selectedPeriodTextView = view.findViewById<TextView>(R.id.text_selected_period)
        val monthLabel = view.findViewById<TextView>(R.id.label_stats_month)
        val monthStats = view.findViewById<TextView>(R.id.text_stats_month)
        val yearLabel = view.findViewById<TextView>(R.id.label_stats_year)
        val yearStats = view.findViewById<TextView>(R.id.text_stats_year)
        val totalStats = view.findViewById<TextView>(R.id.text_stats_total)

        periodSelector.setOnClickListener {
            showMonthYearPickerDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stats.collect { state ->
                    if (state.isLoading) return@collect

                    // Оновлюємо текст обраного періоду
                    val period = state.selectedPeriod
                    selectedPeriodTextView.text = "${months[period.month]} ${period.year}"

                    // Оновлюємо лейбли
                    monthLabel.text = getString(R.string.stats_in_month_year, months[period.month], period.year)
                    yearLabel.text = getString(R.string.stats_in_year, period.year)

                    // Оновлюємо значення статистики
                    monthStats.text = state.booksInMonth.toString()
                    yearStats.text = state.booksInYear.toString()
                    totalStats.text = state.totalBooks.toString()
                }
            }
        }
    }

    private fun showMonthYearPickerDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_year_picker, null)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.picker_month)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.picker_year)
        val currentPeriod = viewModel.stats.value.selectedPeriod

        // Налаштовуємо вибір місяця
        monthPicker.minValue = 0
        monthPicker.maxValue = months.size - 1
        monthPicker.displayedValues = months
        monthPicker.value = currentPeriod.month

        // Налаштовуємо вибір року
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = 2000
        yearPicker.maxValue = currentYear
        yearPicker.value = currentPeriod.year

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_select_period_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.dialog_ok)) { _, _ ->
                viewModel.updateSelectedPeriod(yearPicker.value, monthPicker.value)
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }
}