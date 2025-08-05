package com.example.bookdiarymobile.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookdiarymobile.BookApplication
import com.example.bookdiarymobile.R
import com.example.bookdiarymobile.data.Book
import com.example.bookdiarymobile.data.BookStatus
import com.google.android.material.textfield.TextInputEditText
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddEditBookFragment : Fragment(R.layout.fragment_add_edit_book) {

    private val navArgs: AddEditBookFragmentArgs by navArgs()
    private val viewModel: AddEditBookViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = (requireActivity().application as BookApplication).repository
                val bookId = navArgs.bookId
                val statusString = navArgs.bookStatus
                val bookStatus = statusString?.let { BookStatus.valueOf(it) }
                val isTransitioning = navArgs.isTransitioningToRead

                @Suppress("UNCHECKED_CAST")
                return AddEditBookViewModel(repository, bookId, bookStatus, isTransitioning) as T
            }
        }
    }

    private var selectedImageUri: Uri? = null
    private var currentCoverPath: String? = null
    private var selectedDateInMillis: Long? = null

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            if (allPermissionsGranted) {
                openGalleryLauncher.launch("image/*")
            } else {
                Toast.makeText(requireContext(), "Permissions required to select an image.", Toast.LENGTH_LONG).show()
            }
        }

    private val openGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { sourceUri ->
                launchUCrop(sourceUri)
            }
        }

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            resultUri?.let {
                selectedImageUri = it
                val coverImageView = view?.findViewById<ImageView>(R.id.image_view_add_cover)
                coverImageView?.let { iv -> Glide.with(this).load(it).into(iv) }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = result.data?.let { UCrop.getError(it) }
            Toast.makeText(requireContext(), "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ініціалізація нових UI елементів
        val titleEditText = view.findViewById<TextInputEditText>(R.id.edit_text_title)
        val authorEditText = view.findViewById<TextInputEditText>(R.id.edit_text_author)
        val descriptionEditText = view.findViewById<TextInputEditText>(R.id.edit_text_description)
        val saveButton = view.findViewById<Button>(R.id.button_save)
        val coverImageView = view.findViewById<ImageView>(R.id.image_view_add_cover)
        val galleryButton = view.findViewById<ImageButton>(R.id.button_add_from_gallery)
        val cameraButton = view.findViewById<ImageButton>(R.id.button_add_from_camera)
        val readDetailsLayout = view.findViewById<LinearLayout>(R.id.read_details_layout)
        val dateEditText = view.findViewById<TextInputEditText>(R.id.edit_text_date_read)
        val ratingBar = view.findViewById<RatingBar>(R.id.rating_bar_edit)
        val genreSpinner = view.findViewById<Spinner>(R.id.spinner_genre)

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.book_genres,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            genreSpinner.adapter = adapter
        }

        // Обробники для кнопок вибору зображення
        galleryButton.setOnClickListener {
            checkAndRequestPermissions()
        }
        cameraButton.setOnClickListener {
            // TODO: Реалізувати логіку для камери або поки що використовувати той самий вибір з галереї
            checkAndRequestPermissions()
            Toast.makeText(requireContext(), "Camera feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        dateEditText.setOnClickListener {
            showDatePickerDialog(dateEditText)
        }

        // Логіка встановлення дати за замовчуванням
        val shouldSetDefaultDate = (navArgs.isTransitioningToRead || navArgs.bookStatus == "READ")
        if (shouldSetDefaultDate && savedInstanceState == null) {
            selectedDateInMillis = System.currentTimeMillis()
            dateEditText.setText(formatDate(selectedDateInMillis!!))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { book ->
                    val isNewBook = (book == null)

                    val statusForVisibility = book?.status ?: navArgs.bookStatus?.let { BookStatus.valueOf(it) } ?: BookStatus.TO_READ
                    if (statusForVisibility == BookStatus.READ || navArgs.isTransitioningToRead) {
                        readDetailsLayout.visibility = View.VISIBLE
                    } else {
                        readDetailsLayout.visibility = View.GONE
                    }

                    if (!isNewBook) {
                        book?.let {
                            currentCoverPath = it.coverImagePath
                            if (titleEditText.text.toString() != it.title) titleEditText.setText(it.title)
                            if (authorEditText.text.toString() != it.author) authorEditText.setText(it.author)
                            if (descriptionEditText.text.toString() != it.description) descriptionEditText.setText(it.description)

                            if (savedInstanceState != null || !shouldSetDefaultDate) {
                                it.dateRead?.let { date ->
                                    selectedDateInMillis = date
                                    dateEditText.setText(formatDate(date))
                                }
                            }

                            it.rating?.let { rating ->
                                ratingBar.rating = rating.toFloat()
                            }

                            val genres = resources.getStringArray(R.array.book_genres)
                            val genrePosition = genres.indexOf(it.genre)
                            genreSpinner.setSelection(if (genrePosition >= 0) genrePosition else 0)

                            if (it.coverImagePath != null) {
                                if (selectedImageUri == null) {
                                    Glide.with(this@AddEditBookFragment).load(it.coverImagePath).into(coverImageView)
                                }
                            } else {
                                coverImageView.setImageResource(R.drawable.placeholder_cover_sharp)
                            }
                        }
                    }
                }
            }
        }

        saveButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val author = authorEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()
            val ratingValue = ratingBar.rating.toInt()
            val selectedGenre = genreSpinner.selectedItem.toString()

            if (!validateInputs(title, author)) {
                return@setOnClickListener
            }

            val isBecomingRead = (viewModel.isCurrentBookRead() || navArgs.isTransitioningToRead)

            if (isBecomingRead) {
                if (selectedDateInMillis == null) {
                    Toast.makeText(context, "Please select a read date", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (ratingValue == 0) {
                    Toast.makeText(context, "Please provide a rating (1 to 5 stars)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val newCoverPath = selectedImageUri?.let { uri ->
                copyImageToInternalStorage(uri)
            } ?: currentCoverPath

            viewModel.saveBook(title, author, description, newCoverPath, selectedDateInMillis, ratingValue, selectedGenre)
            findNavController().navigateUp()
        }
    }

// --- ФУНКЦІЯ ДЛЯ ПЕРЕВІРКИ ДОЗВОЛІВ ---
    private fun checkAndRequestPermissions() {
        val permissionsToRequest: Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // На Android 13+ запитуємо тільки дозвіл на зображення.
                // Система сама покаже діалог з опцією "Select photos"
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                // На старіших версіях - старий дозвіл
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        // Перевіряємо, чи дозволи вже надано
        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            // Якщо дозволи є, одразу запускаємо галерею
            openGalleryLauncher.launch("image/*")
        } else {
            // Якщо дозволів немає, запускаємо системний діалог для їх запиту
            requestPermissionsLauncher.launch(permissionsToRequest)
        }
    }


    private fun showDatePickerDialog(dateEditText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        selectedDateInMillis?.let {
            calendar.timeInMillis = it
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
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

    private fun formatDate(millis: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return formatter.format(Date(millis))
    }

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
            .withAspectRatio(2f, 3f)
            .withMaxResultSize(450, 675)
            .getIntent(requireContext())
        cropImageLauncher.launch(uCropIntent)
    }

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
            // Показуємо користувачеві повідомлення про помилку
            Toast.makeText(requireContext(), "Failed to save cover image. Please try again.", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun validateInputs(title: String, author: String): Boolean {
        if (title.isBlank()) {
            Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if (author.isBlank()) {
            Toast.makeText(context, "Author cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        val genreSpinner = view?.findViewById<Spinner>(R.id.spinner_genre)
        if (genreSpinner?.selectedItemPosition == 0) {
            Toast.makeText(context, R.string.validation_select_genre, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}