package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.SortOrder
import com.example.bookdiarymobile.databinding.FragmentSortOptionsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Фрагмент, що надає користувачеві інтерфейс для вибору параметрів сортування списку книг.
 *
 * Цей екран отримує поточний порядок сортування (`currentSortOrder`) та тип вихідного екрану
 * (`sourceScreen`) через навігаційні аргументи. Він динамічно адаптує доступні
 * опції сортування (наприклад, приховує сортування за рейтингом для списку "Хочу прочитати").
 *
 * Після вибору та підтвердження, він повертає новий [SortOrder] до попереднього
 * фрагмента за допомогою `setFragmentResult`.
 */
@AndroidEntryPoint
class SortOptionsFragment : Fragment() {

    private var _binding: FragmentSortOptionsBinding? = null
    private val binding get() = _binding!!

    /** Навігаційні аргументи, що містять `currentSortOrder` та `sourceScreen`. */
    private val args: SortOptionsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSortOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Ініціалізує UI, встановлює видимість опцій, попередньо вибирає
     * поточний порядок сортування та налаштовує обробник кнопки "Застосувати".
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isForToReadList = args.sourceScreen == "TO_READ"

        setupVisibility(isForToReadList)
        preselectCurrentSortOrder(binding.radioGroupSort, args.currentSortOrder)

        binding.buttonApplySort.setOnClickListener {
            val selectedOptionId = binding.radioGroupSort.checkedRadioButtonId
            val selectedSortOrder = mapIdToSortOrder(selectedOptionId, isForToReadList)

            // Повертаємо результат до попереднього фрагмента
            setFragmentResult("SORT_REQUEST", bundleOf("SORT_ORDER" to selectedSortOrder))
            findNavController().popBackStack()
        }
    }

    /**
     * Налаштовує видимість опцій сортування залежно від того,
     * з якого екрану був здійснений перехід. Сортування за рейтингом
     * не має сенсу для списку "Хочу прочитати".
     *
     * @param isForToReadList `true`, якщо вихідний екран - "Хочу прочитати".
     */
    private fun setupVisibility(isForToReadList: Boolean) {
        binding.labelRating.isVisible = !isForToReadList
        binding.radioRatingAsc.isVisible = !isForToReadList
        binding.radioRatingDesc.isVisible = !isForToReadList
        binding.dividerRating.isVisible = !isForToReadList
    }

    /**
     * Попередньо встановлює активний RadioButton на основі поточного
     * порядку сортування, отриманого з аргументів.
     *
     * @param radioGroup Група кнопок, в якій потрібно зробити вибір.
     * @param currentOrder Поточний [SortOrder], який потрібно відзначити.
     */
    private fun preselectCurrentSortOrder(radioGroup: RadioGroup, currentOrder: SortOrder) {
        val buttonId = when (currentOrder) {
            SortOrder.DATE_READ_DESC, SortOrder.DATE_ADDED_DESC -> R.id.radio_date_desc
            SortOrder.DATE_READ_ASC, SortOrder.DATE_ADDED_ASC -> R.id.radio_date_asc
            SortOrder.TITLE_ASC -> R.id.radio_title_asc
            SortOrder.TITLE_DESC -> R.id.radio_title_desc
            SortOrder.RATING_DESC -> R.id.radio_rating_desc
            SortOrder.RATING_ASC -> R.id.radio_rating_asc
        }
        radioGroup.check(buttonId)
    }

    /**
     * Перетворює ідентифікатор вибраного RadioButton на відповідне значення [SortOrder].
     * Враховує контекст (`isForToReadList`), щоб правильно інтерпретувати
     * сортування за датою (або дата додавання, або дата прочитання).
     *
     * @param selectedId Ідентифікатор вибраної кнопки.
     * @param isForToReadList `true`, якщо сортування застосовується до списку "Хочу прочитати".
     * @return Відповідне значення [SortOrder].
     */
    private fun mapIdToSortOrder(selectedId: Int, isForToReadList: Boolean): SortOrder {
        return when (selectedId) {
            R.id.radio_title_asc -> SortOrder.TITLE_ASC
            R.id.radio_title_desc -> SortOrder.TITLE_DESC
            R.id.radio_rating_asc -> SortOrder.RATING_ASC
            R.id.radio_rating_desc -> SortOrder.RATING_DESC
            R.id.radio_date_asc -> if (isForToReadList) SortOrder.DATE_ADDED_ASC else SortOrder.DATE_READ_ASC
            R.id.radio_date_desc -> if (isForToReadList) SortOrder.DATE_ADDED_DESC else SortOrder.DATE_READ_DESC
            // Значення за замовчуванням на випадок, якщо нічого не вибрано
            else -> if (isForToReadList) SortOrder.DATE_ADDED_DESC else SortOrder.DATE_READ_DESC
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}