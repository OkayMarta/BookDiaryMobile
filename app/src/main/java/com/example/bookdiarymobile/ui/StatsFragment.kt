package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.databinding.DialogMonthYearPickerBinding
import com.example.bookdiarymobile.databinding.FragmentStatsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels()

    private lateinit var months: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        months = resources.getStringArray(R.array.months)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.periodSelector.setOnClickListener {
            showMonthYearPickerDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stats.collect { state ->
                    if (state.isLoading) return@collect

                    val period = state.selectedPeriod
                    binding.textSelectedPeriod.text = "${months[period.month]} ${period.year}"
                    binding.labelStatsMonth.text = getString(R.string.stats_in_month_year, months[period.month], period.year)
                    binding.labelStatsYear.text = getString(R.string.stats_in_year, period.year)
                    binding.textStatsMonth.text = state.booksInMonth.toString()
                    binding.textStatsYear.text = state.booksInYear.toString()
                    binding.textStatsTotal.text = state.totalBooks.toString()
                }
            }
        }
    }

    private fun showMonthYearPickerDialog() {
        // Використовуємо View Binding для макету діалогового вікна
        val dialogBinding = DialogMonthYearPickerBinding.inflate(LayoutInflater.from(requireContext()))
        val currentPeriod = viewModel.stats.value.selectedPeriod

        // Налаштовуємо вибір місяця
        dialogBinding.pickerMonth.minValue = 0
        dialogBinding.pickerMonth.maxValue = months.size - 1
        dialogBinding.pickerMonth.displayedValues = months
        dialogBinding.pickerMonth.value = currentPeriod.month

        // Налаштовуємо вибір року
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        dialogBinding.pickerYear.minValue = 2000
        dialogBinding.pickerYear.maxValue = currentYear
        dialogBinding.pickerYear.value = currentPeriod.year

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_select_period_title))
            .setView(dialogBinding.root) // Передаємо кореневий елемент binding
            .setPositiveButton(getString(R.string.dialog_ok)) { _, _ ->
                viewModel.updateSelectedPeriod(dialogBinding.pickerYear.value, dialogBinding.pickerMonth.value)
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очищуємо binding
    }
}