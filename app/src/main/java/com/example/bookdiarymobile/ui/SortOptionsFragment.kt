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

@AndroidEntryPoint
class SortOptionsFragment : Fragment() {

    private var _binding: FragmentSortOptionsBinding? = null
    private val binding get() = _binding!!

    private val args: SortOptionsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSortOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isForToReadList = args.sourceScreen == "TO_READ"
        setupVisibility(isForToReadList)
        preselectCurrentSortOrder(binding.radioGroupSort, args.currentSortOrder)

        binding.buttonApplySort.setOnClickListener {
            val selectedOptionId = binding.radioGroupSort.checkedRadioButtonId
            val selectedSortOrder = mapIdToSortOrder(selectedOptionId, isForToReadList)

            setFragmentResult("SORT_REQUEST", bundleOf("SORT_ORDER" to selectedSortOrder))
            findNavController().popBackStack()
        }
    }

    private fun setupVisibility(isForToReadList: Boolean) {
        // Використовуємо binding для доступу до елементів
        binding.labelRating.isVisible = !isForToReadList
        binding.radioRatingAsc.isVisible = !isForToReadList
        binding.radioRatingDesc.isVisible = !isForToReadList
        binding.dividerRating.isVisible = !isForToReadList
    }

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

    private fun mapIdToSortOrder(selectedId: Int, isForToReadList: Boolean): SortOrder {
        return when (selectedId) {
            R.id.radio_title_asc -> SortOrder.TITLE_ASC
            R.id.radio_title_desc -> SortOrder.TITLE_DESC
            R.id.radio_rating_asc -> SortOrder.RATING_ASC
            R.id.radio_rating_desc -> SortOrder.RATING_DESC
            R.id.radio_date_asc -> if (isForToReadList) SortOrder.DATE_ADDED_ASC else SortOrder.DATE_READ_ASC
            R.id.radio_date_desc -> if (isForToReadList) SortOrder.DATE_ADDED_DESC else SortOrder.DATE_READ_DESC
            else -> if (isForToReadList) SortOrder.DATE_ADDED_DESC else SortOrder.DATE_READ_DESC // Значення за замовчуванням
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очищуємо binding
    }
}