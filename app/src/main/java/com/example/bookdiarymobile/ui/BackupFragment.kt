package com.example.bookdiarymobile.ui

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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

class BackupFragment : Fragment(R.layout.fragment_backup) {

    private val viewModel: BackupViewModel by viewModels {
        ViewModelFactory((activity?.application as BookApplication).repository)
    }

    private var progressDialog: Dialog? = null

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { viewModel.exportData(requireContext(), it) }
        }

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { showImportConfirmationDialog(it) }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exportButton = view.findViewById<Button>(R.id.button_export)
        val importButton = view.findViewById<Button>(R.id.button_import)

        exportButton.setOnClickListener {
            val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val fileName = "book_journal_backup_$timestamp.zip"
            createDocumentLauncher.launch(fileName)
        }

        importButton.setOnClickListener {
            openDocumentLauncher.launch("application/zip")
        }

        observeExportStatus()
        observeImportStatus()
    }

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
                            // Скидання статусу відбувається після натискання на кнопку в діалозі
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

    private fun showProgressDialog(message: String) {
        if (progressDialog == null) {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null)
            dialogView.findViewById<TextView>(R.id.text_progress_message).text = message

            progressDialog = MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create()
        }
        progressDialog?.show()
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showResultDialog(isSuccess: Boolean, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isSuccess) "Success" else "Failed")
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok), null)
            .show()
    }

    private fun showRestartDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok)) { _, _ ->
                viewModel.resetImportStatus() // Скидаємо статус перед перезапуском
                restartApp()
            }
            .setCancelable(false)
            .show()
    }

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
        // Важливо, щоб уникнути витоку пам'яті, якщо фрагмент знищується під час показу діалогу
        dismissProgressDialog()
    }
}