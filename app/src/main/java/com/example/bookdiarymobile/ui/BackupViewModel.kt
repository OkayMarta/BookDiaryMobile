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
 * Перелічення, що представляє можливі стани процесу експорту даних.
 * Використовується для інформування UI про хід операції.
 */
enum class ExportStatus { IDLE, IN_PROGRESS, SUCCESS, FAILED }

/**
 * Перелічення, що представляє можливі стани процесу імпорту даних.
 * Використовується для інформування UI про хід операції.
 */
enum class ImportStatus { IDLE, IN_PROGRESS, SUCCESS, FAILED }

/**
 * Тег для фільтрації логів, пов'язаних з процесами резервного копіювання.
 */
private const val TAG = "BackupDebug"

/**
 * ViewModel для екрану резервного копіювання ([BackupFragment]).
 *
 * Відповідає за всю бізнес-логіку експорту та імпорту даних, включаючи:
 * - Збір файлів (база даних, обкладинки) для експорту.
 * - Архівацію файлів у ZIP-архів.
 * - Видалення поточних даних перед імпортом.
 * - Розархівацію даних з ZIP-архіву у відповідні папки додатку.
 * - Управління станом операцій через [StateFlow].
 *
 * @param repository Репозиторій для доступу до даних, зокрема до списку книг для пошуку обкладинок.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(private val repository: BookRepository) : ViewModel() {

    /**
     * Внутрішній [MutableStateFlow] для відстеження статусу експорту.
     */
    private val _exportStatus = MutableStateFlow(ExportStatus.IDLE)
    /**
     * Публічний, незмінний [StateFlow], на який UI підписується для отримання
     * актуального стану процесу експорту.
     */
    val exportStatus = _exportStatus.asStateFlow()

    /**
     * Внутрішній [MutableStateFlow] для відстеження статусу імпорту.
     */
    private val _importStatus = MutableStateFlow(ImportStatus.IDLE)
    /**
     * Публічний, незмінний [StateFlow] для відстеження статусу імпорту.
     */
    val importStatus = _importStatus.asStateFlow()

    /**
     * Виконує експорт даних додатку (бази даних та обкладинок) в один ZIP-архів.
     * Операція виконується у фоновому потоці (`Dispatchers.IO`).
     *
     * @param context Контекст додатку для доступу до файлової системи.
     * @param destinationUri URI файлу, вибраного користувачем для збереження архіву.
     */
    fun exportData(context: Context, destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _exportStatus.value = ExportStatus.IN_PROGRESS
            var tempDir: File? = null
            try {
                Log.d(TAG, "--- STARTING EXPORT ---")
                // Створюємо тимчасову папку для збору всіх файлів перед їх архівацією.
                tempDir = File(context.cacheDir, "backup_temp").apply { mkdirs() }
                Log.d(TAG, "Temp dir created at: ${tempDir.absolutePath}")

                // Відтворюємо структуру папок додатку (`databases`, `files/covers`) у тимчасовій директорії.
                val tempDbDir = File(tempDir, "databases").apply { mkdirs() }
                val tempFilesDir = File(tempDir, "files").apply { mkdirs() }
                val tempCoversDir = File(tempFilesDir, "covers").apply { mkdirs() }

                // Копіюємо файл бази даних Room.
                val dbFile = context.getDatabasePath("books_database")
                if (dbFile.exists()) {
                    dbFile.copyTo(File(tempDbDir, dbFile.name), true)
                    Log.d(TAG, "DB file copied: ${dbFile.name} to ${tempDbDir.absolutePath}")
                } else {
                    Log.w(TAG, "DB file not found at ${dbFile.absolutePath}")
                }

                // Отримуємо список усіх книг, щоб знайти та скопіювати їхні обкладинки.
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

                // Архівуємо вміст тимчасової папки у ZIP-файл за вказаним URI.
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
                // Обов'язково видаляємо тимчасову папку після завершення операції.
                tempDir?.deleteRecursively()
            }
        }
    }

    /**
     * Виконує імпорт даних із ZIP-архіву, повністю замінюючи поточні дані.
     * Операція є деструктивною і вимагає перезапуску додатку.
     *
     * @param context Контекст додатку для доступу до файлової системи.
     * @param sourceUri URI ZIP-архіву, вибраного користувачем для імпорту.
     */
    fun importData(context: Context, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _importStatus.value = ImportStatus.IN_PROGRESS
            Log.d(TAG, "--- STARTING IMPORT ---")

            // Отримуємо кореневу папку даних додатку (напр., /data/data/com.example.bookdiarymobile).
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
                // ПОВНЕ ВИДАЛЕННЯ ПОТОЧНИХ ДАНИХ.
                if (dbDir != null && dbDir.exists()) {
                    val deleted = dbDir.deleteRecursively()
                    Log.d(TAG, "Old DB directory deleted: $deleted at ${dbDir.absolutePath}")
                }
                if (coversDir.exists()) {
                    val deleted = coversDir.deleteRecursively()
                    Log.d(TAG, "Old covers directory deleted: $deleted at ${coversDir.absolutePath}")
                }

                // Розархівація файлів з архіву безпосередньо у папку даних додатку.
                Log.d(TAG, "Unzipping archive...")
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var zipEntry: ZipEntry? = zis.nextEntry
                        while (zipEntry != null) {
                            val newFile = File(appDataDir, zipEntry.name)
                            Log.d(TAG, "Processing zip entry: ${zipEntry.name} -> ${newFile.absolutePath}")

                            // Захист від уразливості "Zip Slip", щоб уникнути запису файлів за межами папки додатку.
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

                // Перевірка, чи файл бази даних успішно відновлено.
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
     * Скидає статус експорту до початкового стану [ExportStatus.IDLE].
     * Зазвичай викликається з UI після показу діалогу з результатом.
     */
    fun resetExportStatus() { _exportStatus.value = ExportStatus.IDLE }

    /**
     * Скидає статус імпорту до початкового стану [ImportStatus.IDLE].
     * Зазвичай викликається з UI перед перезапуском додатку.
     */
    fun resetImportStatus() { _importStatus.value = ImportStatus.IDLE }

    /**
     * Рекурсивна допоміжна функція для архівації вмісту папки.
     *
     * @param zos Вихідний потік ZIP-архіву.
     * @param folder Папка, вміст якої потрібно заархівувати.
     * @param basePathLength Довжина базового шляху для створення відносних шляхів всередині архіву.
     */
    private fun zipSubFolder(zos: ZipOutputStream, folder: File, basePathLength: Int) {
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                zipSubFolder(zos, file, basePathLength)
            } else {
                FileInputStream(file).use { fis ->
                    // Створюємо відносний шлях, щоб зберегти структуру папок в архіві.
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