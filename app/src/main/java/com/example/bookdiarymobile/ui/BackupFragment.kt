package com.example.bookdiarymobile.ui

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.databinding.FragmentBackupBinding
import com.example.bookdiarymobile.databinding.DialogProgressBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Фрагмент, що відповідає за функціонал резервного копіювання (експорт)
 * та відновлення (імпорт) даних додатку.
 *
 * Надає користувачеві інтерфейс для:
 * - Експорту всієї бази даних та обкладинок у один ZIP-архів.
 * - Імпорту даних з раніше створеного ZIP-архіву.
 *
 * Взаємодіє з [BackupViewModel] для виконання цих операцій та
 * відображає їх прогрес і результат за допомогою діалогових вікон.
 */
@AndroidEntryPoint
class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BackupViewModel by viewModels()

    /** Діалогове вікно для відображення прогресу операцій експорту/імпорту. */
    private var progressDialog: Dialog? = null

    /**
     * ActivityResultLauncher для запуску системного діалогу "Зберегти як...".
     * Користувач вибирає місце та ім'я файлу для збереження ZIP-архіву.
     * Отриманий URI передається у ViewModel для експорту даних.
     */
    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { viewModel.exportData(requireContext(), it) }
        }

    /**
     * ActivityResultLauncher для запуску системного діалогу "Відкрити...".
     * Користувач вибирає ZIP-архів для імпорту.
     * Перед початком імпорту відображається діалог з попередженням.
     */
    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { showImportConfirmationDialog(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonExport.setOnClickListener {
            val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val fileName = "book_journal_backup_$timestamp.zip"
            createDocumentLauncher.launch(fileName)
        }

        binding.buttonImport.setOnClickListener {
            openDocumentLauncher.launch("application/zip")
        }

        observeExportStatus()
        observeImportStatus()
    }

    /**
     * Відображає діалог-попередження перед початком імпорту.
     * Імпорт є деструктивною операцією, оскільки він замінює всі поточні дані.
     * @param uri URI ZIP-архіву, який буде імпортовано.
     */
    private fun showImportConfirmationDialog(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.import_warning_title))
            .setMessage(getString(R.string.import_warning_message))
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .setPositiveButton(getString(R.string.import_action_confirm)) { _, _ ->
                viewModel.importData(requireContext(), uri)
            }
            .show()
    }

    /**
     * Спостерігає за станом процесу експорту ([BackupViewModel.exportStatus])
     * та оновлює UI відповідно (показує/ховає діалоги).
     */
    private fun observeExportStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exportStatus.collect { status ->
                    when (status) {
                        ExportStatus.IN_PROGRESS -> showProgressDialog(getString(R.string.export_in_progress))
                        ExportStatus.SUCCESS -> {
                            dismissProgressDialog()
                            showResultDialog(isSuccess = true, message = getString(R.string.export_success_message))
                            viewModel.resetExportStatus()
                        }
                        ExportStatus.FAILED -> {
                            dismissProgressDialog()
                            showResultDialog(isSuccess = false, message = getString(R.string.export_failed_message))
                            viewModel.resetExportStatus()
                        }
                        ExportStatus.IDLE -> dismissProgressDialog()
                    }
                }
            }
        }
    }

    /**
     * Спостерігає за станом процесу імпорту ([BackupViewModel.importStatus])
     * та оновлює UI. У разі успіху або невдачі показує діалог,
     * що вимагає перезапуску додатку.
     */
    private fun observeImportStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.importStatus.collect { status ->
                    when (status) {
                        ImportStatus.IN_PROGRESS -> showProgressDialog(getString(R.string.import_in_progress))
                        ImportStatus.SUCCESS -> {
                            dismissProgressDialog()
                            showRestartDialog(
                                title = getString(R.string.import_success_title),
                                message = getString(R.string.import_success_message)
                            )
                        }
                        ImportStatus.FAILED -> {
                            dismissProgressDialog()
                            showRestartDialog(
                                title = getString(R.string.import_failed_title),
                                message = getString(R.string.import_failed_message)
                            )
                        }
                        ImportStatus.IDLE -> dismissProgressDialog()
                    }
                }
            }
        }
    }

    /**
     * Відображає невідміняємий діалог з індикатором прогресу.
     * @param message Текст, що буде відображено в діалозі.
     */
    private fun showProgressDialog(message: String) {
        if (progressDialog == null) {
            val dialogBinding = DialogProgressBinding.inflate(LayoutInflater.from(requireContext()))
            dialogBinding.textProgressMessage.text = message

            progressDialog = MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.root)
                .setCancelable(false)
                .create()
        }
        progressDialog?.show()
    }

    /**
     * Закриває та очищує діалог прогресу.
     */
    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    /**
     * Відображає діалог з результатом операції (успіх/невдача).
     */
    private fun showResultDialog(isSuccess: Boolean, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isSuccess) "Success" else "Failed")
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok), null)
            .show()
    }

    /**
     * Відображає невідміняємий діалог, що інформує про результат імпорту
     * і вимагає перезапуску додатку для застосування змін.
     */
    private fun showRestartDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok)) { _, _ ->
                viewModel.resetImportStatus()
                restartApp()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Виконує повний перезапуск додатку.
     * Це необхідно після імпорту, щоб Hilt та Room коректно ініціалізували
     * нову базу даних.
     */
    private fun restartApp() {
        val context = requireContext().applicationContext
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissProgressDialog()
        _binding = null
    }
}