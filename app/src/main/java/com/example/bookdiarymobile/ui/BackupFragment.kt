package com.example.bookdiarymobile.ui

import android.content.Intent
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Фрагмент, що відповідає за функціонал резервного копіювання та відновлення даних.
 */
class BackupFragment : Fragment(R.layout.fragment_backup) {

    /**
     * Ініціалізація ViewModel для цього фрагменту через фабрику.
     */
    private val viewModel: BackupViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    /**
     * ActivityResultLauncher для запуску системного діалогу збереження файлу.
     * Після того як користувач вибере місце та ім'я файлу, викликається метод експорту у ViewModel.
     */
    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { viewModel.exportData(requireContext(), it) }
        }

    /**
     * ActivityResultLauncher для запуску системного діалогу вибору файлу.
     * Після вибору ZIP-архіву для імпорту, відображається діалог-попередження.
     */
    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                showImportConfirmationDialog(it)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exportButton = view.findViewById<Button>(R.id.button_export)
        val importButton = view.findViewById<Button>(R.id.button_import)

        // Обробник натискання на кнопку експорту.
        exportButton.setOnClickListener {
            // Генеруємо ім'я файлу з поточною датою.
            val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val fileName = "book_journal_backup_$timestamp.zip"
            // Запускаємо системний діалог збереження.
            createDocumentLauncher.launch(fileName)
        }

        // Обробник натискання на кнопку імпорту.
        importButton.setOnClickListener {
            // Запускаємо системний діалог вибору ZIP-файлу.
            openDocumentLauncher.launch("application/zip")
        }

        // Запуск спостереження за статусами процесів експорту та імпорту.
        observeExportStatus()
        observeImportStatus()
    }

    /**
     * Відображає діалогове вікно з попередженням про те, що всі поточні дані будуть видалені.
     * Якщо користувач погоджується, запускається процес імпорту.
     */
    private fun showImportConfirmationDialog(uri: android.net.Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.import_warning_title))
            .setMessage(getString(R.string.import_warning_message))
            .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.import_action_confirm)) { dialog, _ ->
                viewModel.importData(requireContext(), uri)
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Спостерігає за станом процесу експорту даних та показує відповідні повідомлення.
     */
    private fun observeExportStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exportStatus.collect { status ->
                    when (status) {
                        ExportStatus.IN_PROGRESS -> Toast.makeText(context, "Exporting data...", Toast.LENGTH_SHORT).show()
                        ExportStatus.SUCCESS -> {
                            Toast.makeText(context, "Export successful!", Toast.LENGTH_LONG).show()
                            viewModel.resetExportStatus()
                        }
                        ExportStatus.FAILED -> {
                            Toast.makeText(context, "Export failed. See logs for details.", Toast.LENGTH_LONG).show()
                            viewModel.resetExportStatus()
                        }
                        ExportStatus.IDLE -> { /* Нічого не робити */ }
                    }
                }
            }
        }
    }

    /**
     * Спостерігає за станом процесу імпорту даних.
     * У разі успішного або невдалого імпорту перезапускає додаток.
     */
    private fun observeImportStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.importStatus.collect { status ->
                    when (status) {
                        ImportStatus.IN_PROGRESS -> Toast.makeText(context, "Importing data...", Toast.LENGTH_SHORT).show()
                        ImportStatus.SUCCESS -> {
                            Toast.makeText(context, "Import successful! Restarting app...", Toast.LENGTH_LONG).show()
                            restartApp()
                        }
                        ImportStatus.FAILED -> {
                            Toast.makeText(context, "Import failed. Data might be corrupted. Restarting app...", Toast.LENGTH_LONG).show()
                            viewModel.resetImportStatus()
                            restartApp()
                        }
                        ImportStatus.IDLE -> { /* Нічого не робити */ }
                    }
                }
            }
        }
    }

    /**
     * Повністю перезапускає додаток. Це необхідно, щоб змусити систему
     * переініціалізувати базу даних з відновленого файлу.
     */
    private fun restartApp() {
        val context = requireContext()
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}