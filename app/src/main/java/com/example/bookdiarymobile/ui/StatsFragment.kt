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

/**
 * Фрагмент, що відповідає за відображення статистики прочитаних книг.
 *
 * Надає користувачеві можливість переглядати кількість прочитаних книг за:
 * - Вибраний місяць та рік.
 * - Вибраний рік.
 * - За весь час ведення щоденника.
 *
 * Користувач може змінити період для перегляду статистики, викликавши
 * діалогове вікно з вибором місяця та року. Взаємодіє з [StatsViewModel]
 * для отримання даних та оновлення періоду.
 */
@AndroidEntryPoint
class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    /** Захищене посилання на ViewBinding, що гарантує non-null доступ після `onViewCreated`. */
    private val binding get() = _binding!!

    /** ViewModel, що керує логікою та станом екрану статистики. */
    private val viewModel: StatsViewModel by viewModels()

    /** Масив рядків з назвами місяців, завантажений з ресурсів. */
    private lateinit var months: Array<String>

    /**
     * Ініціалізує не-UI компоненти, такі як масив назв місяців,
     * які потрібні для роботи фрагмента.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        months = resources.getStringArray(R.array.months)
    }

    /**
     * Створює та повертає ієрархію View для фрагмента, інфлейтячи
     * макет за допомогою ViewBinding.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Налаштовує слухачі та запускає спостереження за станом ViewModel
     * після того, як View було створено.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.periodSelector.setOnClickListener {
            showMonthYearPickerDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle гарантує, що збір даних відбувається лише коли UI видимий (STARTED).
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Підписуємося на оновлення стану статистики з ViewModel.
                viewModel.stats.collect { state ->
                    if (state.isLoading) return@collect

                    val period = state.selectedPeriod
                    // Оновлюємо UI останніми даними зі стану.
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

    /**
     * Створює та відображає кастомне діалогове вікно для вибору місяця та року.
     *
     * Використовує макет `dialog_month_year_picker.xml` та налаштовує
     * `NumberPicker` для місяців та років. Поточний вибраний період
     * встановлюється як початкове значення. При підтвердженні вибору,
     * викликає метод у ViewModel для оновлення статистики.
     */
    private fun showMonthYearPickerDialog() {
        val dialogBinding = DialogMonthYearPickerBinding.inflate(LayoutInflater.from(requireContext()))
        val currentPeriod = viewModel.stats.value.selectedPeriod

        // Налаштування NumberPicker для вибору місяця
        dialogBinding.pickerMonth.minValue = 0
        dialogBinding.pickerMonth.maxValue = months.size - 1
        dialogBinding.pickerMonth.displayedValues = months
        dialogBinding.pickerMonth.value = currentPeriod.month

        // Налаштування NumberPicker для вибору року
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        dialogBinding.pickerYear.minValue = 2000
        dialogBinding.pickerYear.maxValue = currentYear
        dialogBinding.pickerYear.value = currentPeriod.year

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_select_period_title))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.dialog_ok)) { _, _ ->
                viewModel.updateSelectedPeriod(dialogBinding.pickerYear.value, dialogBinding.pickerMonth.value)
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    /**
     * Очищує посилання на ViewBinding, коли View фрагмента знищується,
     * для запобігання витокам пам'яті.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}