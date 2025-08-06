package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.SortOrder
import com.google.android.material.divider.MaterialDivider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SortOptionsFragment : Fragment(R.layout.fragment_sort_options) {

    private val args: SortOptionsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_sort)
        val applyButton = view.findViewById<Button>(R.id.button_apply_sort)

        val isForToReadList = args.sourceScreen == "TO_READ"
        setupVisibility(view, isForToReadList)
        preselectCurrentSortOrder(radioGroup, args.currentSortOrder)

        applyButton.setOnClickListener {
            val selectedOptionId = radioGroup.checkedRadioButtonId
            val selectedSortOrder = mapIdToSortOrder(selectedOptionId, isForToReadList)

            setFragmentResult("SORT_REQUEST", bundleOf("SORT_ORDER" to selectedSortOrder))
            findNavController().popBackStack()
        }
    }

    private fun setupVisibility(view: View, isForToReadList: Boolean) {
        // Ховаємо блок рейтингу для списку "To Read"
        view.findViewById<TextView>(R.id.label_rating).isVisible = !isForToReadList
        view.findViewById<RadioButton>(R.id.radio_rating_asc).isVisible = !isForToReadList
        view.findViewById<RadioButton>(R.id.radio_rating_desc).isVisible = !isForToReadList
        view.findViewById<MaterialDivider>(R.id.divider_rating).isVisible = !isForToReadList
    }

    private fun preselectCurrentSortOrder(radioGroup: RadioGroup, currentOrder: SortOrder) {
        val buttonId = when (currentOrder) {
            SortOrder.DATE_READ_DESC -> R.id.radio_date_desc
            SortOrder.DATE_READ_ASC -> R.id.radio_date_asc
            SortOrder.DATE_ADDED_DESC -> R.id.radio_date_desc
            SortOrder.DATE_ADDED_ASC -> R.id.radio_date_asc
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
            else -> if (isForToReadList) SortOrder.DATE_ADDED_DESC else SortOrder.DATE_READ_DESC
        }
    }
}