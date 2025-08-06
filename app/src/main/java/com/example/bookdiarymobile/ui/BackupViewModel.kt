package com.example.bookdiarymobile.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookdiarymobile.data.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Перелічення, що представляють можливі стани процесу експорту.
 */
enum class ExportStatus { IDLE, IN_PROGRESS, SUCCESS, FAILED }

/**
 * Перелічення, що представляють можливі стани процесу імпорту.
 */
enum class ImportStatus { IDLE, IN_PROGRESS, SUCCESS, FAILED }

/**
 * Тег для фільтрації логів, пов'язаних з резервним копіюванням.
 */
private const val TAG = "BackupDebug"

/**
 * ViewModel для екрану резервного копіювання.
 * Відповідає за логіку експорту та імпорту даних.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(private val repository: BookRepository) : ViewModel() {

    /**
     * StateFlow для відстеження статусу експорту. UI підписується на нього,
     * щоб показувати відповідні повідомлення користувачу.
     */
    private val _exportStatus = MutableStateFlow(ExportStatus.IDLE)
    val exportStatus = _exportStatus.asStateFlow()

    /**
     * StateFlow для відстеження статусу імпорту.
     */
    private val _importStatus = MutableStateFlow(ImportStatus.IDLE)
    val importStatus = _importStatus.asStateFlow()

    /**
     * Виконує експорт даних додатку в ZIP-архів.
     */
    fun exportData(context: Context, destinationUri: Uri) {
        // Запускаємо операцію у фоновому потоці, щоб не блокувати UI.
        viewModelScope.launch(Dispatchers.IO) {
            _exportStatus.value = ExportStatus.IN_PROGRESS
            var tempDir: File? = null
            try {
                Log.d(TAG, "--- STARTING EXPORT ---")
                // Створюємо тимчасову папку для збору файлів перед архівацією.
                tempDir = File(context.cacheDir, "backup_temp").apply { mkdirs() }
                Log.d(TAG, "Temp dir created at: ${tempDir.absolutePath}")

                // Створюємо структуру папок, що точно відповідає структурі даних додатку.
                val tempDbDir = File(tempDir, "databases").apply { mkdirs() }
                val tempFilesDir = File(tempDir, "files").apply { mkdirs() }
                val tempCoversDir = File(tempFilesDir, "covers").apply { mkdirs() }

                // Копіюємо файл бази даних у тимчасову папку.
                val dbFile = context.getDatabasePath("books_database")
                if (dbFile.exists()) {
                    dbFile.copyTo(File(tempDbDir, dbFile.name), true)
                    Log.d(TAG, "DB file copied: ${dbFile.name} to ${tempDbDir.absolutePath}")
                } else {
                    Log.w(TAG, "DB file not found at ${dbFile.absolutePath}")
                }

                // Отримуємо список усіх книг та копіюємо їхні обкладинки.
                val books = repository.getAllBooksForBackup()
                Log.d(TAG, "Found ${books.size} books to check for covers.")
                books.forEach { book ->
                    book.coverImagePath?.let { path ->
                        val coverFile = File(path)
                        if (coverFile.exists()) {
                            coverFile.copyTo(File(tempCoversDir, coverFile.name), true)
                            Log.d(TAG, "Cover copied: ${coverFile.name}")
                        }
                    }
                }

                // Архівуємо вміст тимчасової папки у вибраний користувачем файл.
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zos ->
                        zipSubFolder(zos, tempDir, tempDir.path.length)
                    }
                }
                _exportStatus.value = ExportStatus.SUCCESS
                Log.d(TAG, "--- EXPORT SUCCESS ---")
            } catch (e: Exception) {
                Log.e(TAG, "--- EXPORT FAILED ---", e)
                _exportStatus.value = ExportStatus.FAILED
            } finally {
                // Видаляємо тимчасову папку після завершення операції.
                tempDir?.deleteRecursively()
            }
        }
    }

    /**
     * Виконує імпорт даних із ZIP-архіву, повністю замінюючи поточні дані.
     */
    fun importData(context: Context, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _importStatus.value = ImportStatus.IN_PROGRESS
            Log.d(TAG, "--- STARTING IMPORT ---")

            // Отримуємо кореневу папку даних додатку (/data/data/com.example.bookdiarymobile).
            val appDataDir = context.filesDir.parentFile
            if (appDataDir == null) {
                Log.e(TAG, "App data directory is null, cannot proceed.")
                _importStatus.value = ImportStatus.FAILED
                return@launch
            }
            Log.d(TAG, "App data dir: ${appDataDir.absolutePath}")

            val dbFile = context.getDatabasePath("books_database")
            val dbDir = dbFile.parentFile
            val coversDir = File(context.filesDir, "covers")

            try {
                // Видаляємо старі папки з базою даних та обкладинками.
                if (dbDir != null && dbDir.exists()) {
                    val deleted = dbDir.deleteRecursively()
                    Log.d(TAG, "Old DB directory deleted: $deleted at ${dbDir.absolutePath}")
                }
                if (coversDir.exists()) {
                    val deleted = coversDir.deleteRecursively()
                    Log.d(TAG, "Old covers directory deleted: $deleted at ${coversDir.absolutePath}")
                }

                Log.d(TAG, "Unzipping archive...")
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var zipEntry: ZipEntry? = zis.nextEntry
                        while (zipEntry != null) {
                            val newFile = File(appDataDir, zipEntry.name)
                            Log.d(TAG, "Processing zip entry: ${zipEntry.name} -> ${newFile.absolutePath}")

                            if (!newFile.canonicalPath.startsWith(appDataDir.canonicalPath)) {
                                throw SecurityException("Zip Slip vulnerability detected!")
                            }

                            if (zipEntry.isDirectory) {
                                newFile.mkdirs()
                            } else {
                                newFile.parentFile?.mkdirs()
                                FileOutputStream(newFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                            zis.closeEntry()
                            zipEntry = zis.nextEntry
                        }
                    }
                }
                Log.d(TAG, "Unzipping complete.")

                val restoredDbFile = File(appDataDir, "databases/books_database")
                if (restoredDbFile.exists()) {
                    Log.d(TAG, "SUCCESS! DB file exists at ${restoredDbFile.absolutePath} after unzip. Size: ${restoredDbFile.length()} bytes.")
                } else {
                    Log.e(TAG, "FAILURE! DB file NOT FOUND at ${restoredDbFile.absolutePath} after unzip.")
                }

                _importStatus.value = ImportStatus.SUCCESS
                Log.d(TAG, "--- IMPORT SUCCESS (PRE-RESTART) ---")

            } catch (e: Exception) {
                Log.e(TAG, "--- IMPORT FAILED ---", e)
                _importStatus.value = ImportStatus.FAILED
            }
        }
    }

    /**
     * Скидає статус експорту до початкового стану.
     */
    fun resetExportStatus() { _exportStatus.value = ExportStatus.IDLE }

    /**
     * Скидає статус імпорту до початкового стану.
     */
    fun resetImportStatus() { _importStatus.value = ImportStatus.IDLE }

    /**
     * Рекурсивна допоміжна функція для архівації вмісту папки.
     */
    private fun zipSubFolder(zos: ZipOutputStream, folder: File, basePathLength: Int) {
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                zipSubFolder(zos, file, basePathLength)
            } else {
                FileInputStream(file).use { fis ->
                    // Формуємо відносний шлях файлу всередині архіву.
                    val relativePath = file.path.substring(basePathLength + 1).replace('\\', '/')
                    Log.d(TAG, "Zipping: $relativePath")
                    val zipEntry = ZipEntry(relativePath)
                    zos.putNextEntry(zipEntry)
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }
}