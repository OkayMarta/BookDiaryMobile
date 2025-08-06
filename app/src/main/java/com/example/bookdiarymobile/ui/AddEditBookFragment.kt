package com.example.bookdiarymobile.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
class AddEditBookFragment : Fragment() {

    private var _binding: FragmentAddEditBookBinding? = null
    private val binding get() = _binding!!

    private val navArgs: AddEditBookFragmentArgs by navArgs()
    private val viewModel: AddEditBookViewModel by viewModels()

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
                // Перевіряємо, чи binding не null, хоча в цьому життєвому циклі він має бути доступний.
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

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.book_genres,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerGenre.adapter = adapter
        }

        binding.buttonAddFromGallery.setOnClickListener { checkAndRequestPermissions() }
        binding.buttonAddFromCamera.setOnClickListener {
            checkAndRequestPermissions()
            Toast.makeText(requireContext(), "Camera feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        binding.editTextDateRead.setOnClickListener { showDatePickerDialog(binding.editTextDateRead) }

        val shouldSetDefaultDate = (navArgs.isTransitioningToRead || navArgs.bookStatus == "READ")
        if (shouldSetDefaultDate && savedInstanceState == null) {
            selectedDateInMillis = System.currentTimeMillis()
            binding.editTextDateRead.setText(formatDate(selectedDateInMillis!!))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { book ->
                    val isNewBook = (book == null)

                    val statusForVisibility = book?.status ?: navArgs.bookStatus?.let { BookStatus.valueOf(it) } ?: BookStatus.TO_READ
                    val isReadMode = (statusForVisibility == BookStatus.READ || navArgs.isTransitioningToRead)

                    binding.labelDateRead.isVisible = isReadMode
                    binding.editTextDateRead.isVisible = isReadMode
                    binding.labelRatingEdit.isVisible = isReadMode
                    binding.ratingBarEdit.isVisible = isReadMode

                    if (!isNewBook) {
                        book?.let {
                            currentCoverPath = it.coverImagePath
                            if (binding.editTextTitle.text.toString() != it.title) binding.editTextTitle.setText(it.title)
                            if (binding.editTextAuthor.text.toString() != it.author) binding.editTextAuthor.setText(it.author)
                            if (binding.editTextDescription.text.toString() != it.description) binding.editTextDescription.setText(it.description)

                            if (savedInstanceState != null || !shouldSetDefaultDate) {
                                it.dateRead?.let { date ->
                                    selectedDateInMillis = date
                                    binding.editTextDateRead.setText(formatDate(date))
                                }
                            }

                            it.rating?.let { rating ->
                                binding.ratingBarEdit.rating = rating.toFloat()
                            }

                            val genres = resources.getStringArray(R.array.book_genres)
                            val genrePosition = genres.indexOf(it.genre)
                            binding.spinnerGenre.setSelection(if (genrePosition >= 0) genrePosition else 0)

                            if (it.coverImagePath != null) {
                                if (selectedImageUri == null) {
                                    Glide.with(this@AddEditBookFragment).load(it.coverImagePath).into(binding.imageViewAddCover)
                                }
                            } else {
                                binding.imageViewAddCover.setImageResource(R.drawable.placeholder_cover_sharp)
                            }
                        }
                    }
                }
            }
        }

        binding.buttonSave.setOnClickListener {
            val title = binding.editTextTitle.text.toString().trim()
            val author = binding.editTextAuthor.text.toString().trim()
            val description = binding.editTextDescription.text.toString().trim()
            val ratingValue = binding.ratingBarEdit.rating.toInt()
            val selectedGenre = binding.spinnerGenre.selectedItem.toString()

            if (!validateInputs(title, author)) { return@setOnClickListener }

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

            val newCoverPath = selectedImageUri?.let { uri -> copyImageToInternalStorage(uri) } ?: currentCoverPath

            viewModel.saveBook(title, author, description, newCoverPath, selectedDateInMillis, ratingValue, selectedGenre)
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest: Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            openGalleryLauncher.launch("image/*")
        } else {
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
        if (binding.spinnerGenre.selectedItemPosition == 0) {
            Toast.makeText(context, R.string.validation_select_genre, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}