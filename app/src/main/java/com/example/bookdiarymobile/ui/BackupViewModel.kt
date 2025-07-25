package com.example.bookdiarymobile.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportStatus { IDLE, IN_PROGRESS, SUCCESS, FAILED }

class BackupViewModel(private val repository: BookRepository) : ViewModel() {

    private val _exportStatus = MutableStateFlow(ExportStatus.IDLE)
    val exportStatus = _exportStatus.asStateFlow()

    fun exportData(context: Context, destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _exportStatus.value = ExportStatus.IN_PROGRESS
            var tempDir: File? = null
            try {
                // 1. Створюємо тимчасову папку
                tempDir = File(context.cacheDir, "backup_temp").apply { mkdirs() }
                val coversDir = File(tempDir, "covers").apply { mkdirs() }

                // 2. Копіюємо файл бази даних
                val dbFile = context.getDatabasePath("books_database")
                dbFile.copyTo(File(tempDir, dbFile.name), true)

                // 3. Копіюємо всі обкладинки
                val books = repository.getAllBooksForBackup()
                books.forEach { book ->
                    book.coverImagePath?.let { path ->
                        val coverFile = File(path)
                        if (coverFile.exists()) {
                            coverFile.copyTo(File(coversDir, coverFile.name), true)
                        }
                    }
                }

                // 4. Архівуємо все у ZIP-файл прямо в обране користувачем місце
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zos ->
                        zipSubFolder(zos, tempDir, tempDir.path.length)
                    }
                }
                _exportStatus.value = ExportStatus.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                _exportStatus.value = ExportStatus.FAILED
            } finally {
                // 5. Видаляємо тимчасову папку
                tempDir?.deleteRecursively()
            }
        }
    }
    fun resetStatus() {
        _exportStatus.value = ExportStatus.IDLE
    }

    private fun zipSubFolder(zos: ZipOutputStream, folder: File, basePathLength: Int) {
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                zipSubFolder(zos, file, basePathLength)
            } else {
                FileInputStream(file).use { fis ->
                    val relativePath = file.path.substring(basePathLength + 1)
                    val zipEntry = ZipEntry(relativePath)
                    zos.putNextEntry(zipEntry)
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }
}