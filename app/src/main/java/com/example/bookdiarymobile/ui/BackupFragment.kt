package com.example.bookdiarymobile.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupFragment : Fragment(R.layout.fragment_backup) {

    private val viewModel: BackupViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    // Створюємо лаунчер для системного діалогу збереження файлу
    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            // Цей код виконається, коли користувач вибере місце і назву файлу
            uri?.let {
                viewModel.exportData(requireContext(), it)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exportButton = view.findViewById<Button>(R.id.button_export)

        exportButton.setOnClickListener {
            // Генеруємо ім'я файлу з поточною датою
            val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val fileName = "book_journal_backup_$timestamp.zip"
            // Запускаємо системний діалог
            createDocumentLauncher.launch(fileName)
        }

        // Спостерігаємо за статусом експорту
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exportStatus.collect { status ->
                    when (status) {
                        ExportStatus.IN_PROGRESS -> Toast.makeText(context, "Exporting data...", Toast.LENGTH_SHORT).show()
                        ExportStatus.SUCCESS -> {
                            Toast.makeText(context, "Export successful!", Toast.LENGTH_LONG).show()
                            viewModel.resetStatus()
                        }
                        ExportStatus.FAILED -> {
                            Toast.makeText(context, "Export failed. See logs for details.", Toast.LENGTH_LONG).show()
                            viewModel.resetStatus()
                        }
                        ExportStatus.IDLE -> { /* do nothing */ }
                    }
                }
            }
        }
    }
}