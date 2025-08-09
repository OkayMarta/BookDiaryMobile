package com.example.bookdiarymobile.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.BookStatus
import com.example.bookdiarymobile.databinding.FragmentAddEditBookBinding
import com.google.android.material.textfield.TextInputEditText
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Фрагмент для додавання нової книги або редагування існуючої.
 *
 * Цей екран є універсальним і адаптує свій вигляд та логіку залежно від
 * переданих навігаційних аргументів (`book_id`, `book_status`).
 * Він відповідає за:
 * - Відображення полів для введення даних про книгу.
 * - Завантаження та відображення даних існуючої книги для редагування.
 * - Взаємодію з галереєю та камерою для вибору обкладинки.
 * - Обрізку зображення за допомогою бібліотеки UCrop.
 * - Збереження даних (нових або оновлених) через [AddEditBookViewModel].
 * - Валідацію введених даних.
 */
@AndroidEntryPoint
class AddEditBookFragment : Fragment() {

    private var _binding: FragmentAddEditBookBinding? = null
    private val binding get() = _binding!!

    /** Навігаційні аргументи, що передаються з попереднього екрану. */
    private val navArgs: AddEditBookFragmentArgs by navArgs()
    /** ViewModel, що керує логікою та станом цього екрану. */
    private val viewModel: AddEditBookViewModel by viewModels()

    /** URI нового зображення, вибраного користувачем (після обрізки). */
    private var selectedImageUri: Uri? = null
    /** Шлях до поточного файлу обкладинки (для режиму редагування). */
    private var currentCoverPath: String? = null
    /** Вибрана дата прочитання у форматі мілісекунд (timestamp). */
    private var selectedDateInMillis: Long? = null

    /** URI для тимчасового зберігання фотографії, зробленої камерою. */
    private var cameraPhotoUri: Uri? = null

    /**
     * Перелічення для визначення дії, яку потрібно виконати після
     * отримання дозволів від користувача.
     */
    private enum class PendingAction { NONE, SELECT_FROM_GALLERY, TAKE_PHOTO }
    private var pendingAction = PendingAction.NONE

    /**
     * ActivityResultLauncher для запиту дозволів (камера, доступ до сховища).
     * Після отримання результату, він перевіряє, чи надано дозвіл для
     * відкладеної дії (`pendingAction`), і виконує її.
     */
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isGranted = when (pendingAction) {
                PendingAction.TAKE_PHOTO -> permissions[Manifest.permission.CAMERA] ?: false
                PendingAction.SELECT_FROM_GALLERY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
                    } else {
                        permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
                    }
                }
                else -> false
            }

            if (isGranted) {
                when (pendingAction) {
                    PendingAction.SELECT_FROM_GALLERY -> openGalleryLauncher.launch("image/*")
                    PendingAction.TAKE_PHOTO -> launchCamera()
                    else -> {}
                }
            } else {
                Toast.makeText(requireContext(), "Permissions are required to continue.", Toast.LENGTH_LONG).show()
            }
            pendingAction = PendingAction.NONE
        }

    /**
     * ActivityResultLauncher для вибору зображення з галереї.
     * Отриманий URI передається в UCrop для обрізки.
     */
    private val openGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { sourceUri ->
                launchUCrop(sourceUri)
            }
        }

    /**
     * ActivityResultLauncher для запуску камери.
     * Якщо фото зроблено успішно, його URI передається в UCrop для обрізки.
     */
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraPhotoUri?.let { launchUCrop(it) }
        } else {
            Toast.makeText(requireContext(), "Failed to capture image.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ActivityResultLauncher для отримання результату від UCrop.
     * Якщо обрізка успішна, оновлює `selectedImageUri` та відображає
     * нове зображення в `ImageView`.
     */
    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            resultUri?.let {
                selectedImageUri = it
                _binding?.let { b -> Glide.with(this).load(it).into(b.imageViewAddCover) }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = result.data?.let { UCrop.getError(it) }
            Toast.makeText(requireContext(), "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()
        setupClickListeners()
        setDefaultDateIfNeeded(savedInstanceState)
        observeUiState(savedInstanceState)
    }

    /**
     * Ініціалізує випадаючий список (Spinner) для вибору жанру.
     */
    private fun setupSpinner() {
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.book_genres,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerGenre.adapter = adapter
        }
    }

    /**
     * Налаштовує обробники натискань для кнопок та полів вводу.
     */
    private fun setupClickListeners() {
        binding.editTextDateRead.setOnClickListener { showDatePickerDialog(binding.editTextDateRead) }
        binding.buttonAddFromGallery.setOnClickListener {
            pendingAction = PendingAction.SELECT_FROM_GALLERY
            checkAndRequestPermissions()
        }
        binding.buttonAddFromCamera.setOnClickListener {
            pendingAction = PendingAction.TAKE_PHOTO
            checkAndRequestPermissions()
        }
        binding.buttonSave.setOnClickListener { saveBook() }
    }

    /**
     * Встановлює поточну дату як дату прочитання за замовчуванням, якщо
     * книга створюється зі статусом "Прочитано" або переноситься у цей статус.
     */
    private fun setDefaultDateIfNeeded(savedInstanceState: Bundle?) {
        val shouldSetDefaultDate = (navArgs.isTransitioningToRead || navArgs.bookStatus == "READ")
        if (shouldSetDefaultDate && savedInstanceState == null) {
            selectedDateInMillis = System.currentTimeMillis()
            binding.editTextDateRead.setText(formatDate(selectedDateInMillis!!))
        }
    }

    /**
     * Підписується на оновлення стану UI ([AddEditBookViewModel.uiState])
     * та заповнює поля форми даними книги в режимі редагування.
     */
    private fun observeUiState(savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { book ->
                    val isNewBook = (book == null)
                    val statusForVisibility = book?.status ?: navArgs.bookStatus?.let { BookStatus.valueOf(it) } ?: BookStatus.TO_READ
                    val isReadMode = (statusForVisibility == BookStatus.READ || navArgs.isTransitioningToRead)

                    // Адаптація UI залежно від статусу книги
                    binding.labelDateRead.isVisible = isReadMode
                    binding.editTextDateRead.isVisible = isReadMode
                    binding.labelRatingEdit.isVisible = isReadMode
                    binding.ratingBarEdit.isVisible = isReadMode

                    if (!isNewBook) {
                        populateForm(book!!, savedInstanceState)
                    }
                }
            }
        }
    }

    /**
     * Заповнює поля форми даними з об'єкта [com.example.bookdiarymobile.data.Book].
     */
    private fun populateForm(book: com.example.bookdiarymobile.data.Book, savedInstanceState: Bundle?) {
        val shouldSetDefaultDate = (navArgs.isTransitioningToRead || navArgs.bookStatus == "READ")

        currentCoverPath = book.coverImagePath
        binding.editTextTitle.setText(book.title)
        binding.editTextAuthor.setText(book.author)
        binding.editTextDescription.setText(book.description)

        // Відновлюємо дату лише якщо це не перехід в "прочитано" або після зміни конфігурації
        if (savedInstanceState != null || !shouldSetDefaultDate) {
            book.dateRead?.let { date ->
                selectedDateInMillis = date
                binding.editTextDateRead.setText(formatDate(date))
            }
        }

        book.rating?.let { rating -> binding.ratingBarEdit.rating = rating.toFloat() }

        val genres = resources.getStringArray(R.array.book_genres)
        val genrePosition = genres.indexOf(book.genre)
        binding.spinnerGenre.setSelection(if (genrePosition >= 0) genrePosition else 0)

        // Завантажуємо обкладинку, якщо вона є і користувач не вибрав нову
        if (book.coverImagePath != null) {
            if (selectedImageUri == null) {
                Glide.with(this@AddEditBookFragment).load(book.coverImagePath).into(binding.imageViewAddCover)
            }
        } else {
            binding.imageViewAddCover.setImageResource(R.drawable.placeholder_cover_sharp)
        }
    }

    /**
     * Збирає дані з полів, валідує їх та викликає метод збереження у ViewModel.
     */
    private fun saveBook() {
        val title = binding.editTextTitle.text.toString().trim()
        val author = binding.editTextAuthor.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val ratingValue = binding.ratingBarEdit.rating.toInt()
        val selectedGenre = binding.spinnerGenre.selectedItem.toString()

        if (!validateInputs(title, author)) return

        val isBecomingRead = (viewModel.isCurrentBookRead() || navArgs.isTransitioningToRead)

        // Додаткова валідація для книг зі статусом "Прочитано"
        if (isBecomingRead) {
            if (selectedDateInMillis == null) {
                Toast.makeText(context, "Please select a read date", Toast.LENGTH_SHORT).show()
                return
            }
            if (ratingValue == 0) {
                Toast.makeText(context, "Please provide a rating (1 to 5 stars)", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val newCoverPath = selectedImageUri?.let { uri -> copyImageToInternalStorage(uri) } ?: currentCoverPath

        viewModel.saveBook(title, author, description, newCoverPath, selectedDateInMillis, ratingValue, selectedGenre)
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Запобігання витоку пам'яті
    }

    /**
     * Перевіряє наявність необхідних дозволів. Якщо вони є, виконує дію.
     * Якщо немає, запускає запит дозволів.
     */
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = when (pendingAction) {
            PendingAction.TAKE_PHOTO -> listOf(Manifest.permission.CAMERA)
            PendingAction.SELECT_FROM_GALLERY -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> return
        }

        val permissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (permissionsGranted) {
            when (pendingAction) {
                PendingAction.SELECT_FROM_GALLERY -> openGalleryLauncher.launch("image/*")
                PendingAction.TAKE_PHOTO -> launchCamera()
                else -> {}
            }
        } else {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /**
     * Створює тимчасовий файл для фото та запускає камеру.
     */
    private fun launchCamera() {
        createImageFileUri()?.let { uri ->
            cameraPhotoUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    /**
     * Створює тимчасовий файл у зовнішньому сховищі та повертає його URI.
     * Використовує [FileProvider] для безпечного надання доступу до файлу для додатку камери.
     * @return [Uri] створеного файлу або `null` у разі помилки.
     */
    @Throws(IOException::class)
    private fun createImageFileUri(): Uri? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        val authority = "${requireContext().packageName}.fileprovider"
        return FileProvider.getUriForFile(requireContext(), authority, file)
    }

    /**
     * Відображає діалогове вікно для вибору дати.
     * Після вибору дати оновлює `selectedDateInMillis` та відповідне поле вводу.
     * @param dateEditText Поле, в яке буде вставлена вибрана дата.
     */
    private fun showDatePickerDialog(dateEditText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        selectedDateInMillis?.let {
            calendar.timeInMillis = it
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.App_DatePickerDialogTheme,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                selectedDateInMillis = selectedCalendar.timeInMillis
                dateEditText.setText(formatDate(selectedDateInMillis!!))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    /**
     * Форматує час у мілісекундах у рядок дати формату "dd.MM.yyyy".
     * @param millis Час у мілісекундах.
     * @return Відформатований рядок з датою.
     */
    private fun formatDate(millis: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    /**
     * Запускає екран обрізки зображення (UCrop) з заданими параметрами.
     * @param sourceUri URI вихідного зображення.
     */
    private fun launchUCrop(sourceUri: Uri) {
        val destinationFileName = "${UUID.randomUUID()}.jpg"
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, destinationFileName))
        val options = UCrop.Options().apply {
            val primaryColor = ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_primary)
            val primaryDarkColor = ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_primary_dark)
            val textColor = ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_on_primary)
            setStatusBarColor(primaryDarkColor)
            setToolbarColor(primaryColor)
            setActiveControlsWidgetColor(primaryColor)
            setToolbarWidgetColor(textColor)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(false)
        }
        val uCropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(2f, 3f) // Співвідношення сторін для обкладинки
            .withMaxResultSize(450, 675) // Максимальний розмір зображення
            .getIntent(requireContext())
        cropImageLauncher.launch(uCropIntent)
    }

    /**
     * Копіює зображення з вхідного URI у внутрішнє сховище додатку (папка 'files/covers').
     * @param uri URI зображення, яке потрібно скопіювати.
     * @return Абсолютний шлях до збереженого файлу або `null` у разі помилки.
     */
    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val coversDir = File(requireContext().filesDir, "covers")
            if (!coversDir.exists()) coversDir.mkdirs()
            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(coversDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to save cover image. Please try again.", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Перевіряє, чи заповнені обов'язкові поля.
     * @param title Назва книги.
     * @param author Автор книги.
     * @return `true`, якщо валідація пройшла успішно, інакше `false`.
     */
    private fun validateInputs(title: String, author: String): Boolean {
        if (title.isBlank()) {
            Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if (author.isBlank()) {
            Toast.makeText(context, "Author cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.spinnerGenre.selectedItemPosition == 0) { // 0 - це "Select genre"
            Toast.makeText(context, R.string.validation_select_genre, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}