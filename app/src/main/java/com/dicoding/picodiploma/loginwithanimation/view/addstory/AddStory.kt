package com.dicoding.picodiploma.loginwithanimation.view.addstory

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.picodiploma.loginwithanimation.R
import com.dicoding.picodiploma.loginwithanimation.databinding.ActivityAddStoryBinding
import com.dicoding.picodiploma.loginwithanimation.helper.getImageUri
import com.dicoding.picodiploma.loginwithanimation.helper.reduceFileImage
import com.dicoding.picodiploma.loginwithanimation.helper.uriToFile
import com.dicoding.picodiploma.loginwithanimation.view.ViewModelFactory
import com.dicoding.picodiploma.loginwithanimation.view.main.MainActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class AddStory : AppCompatActivity() {

    private val addStoryViewModel by viewModels<AddStoryViewModel> {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var binding : ActivityAddStoryBinding
    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Add Story"

        binding.galleryButton.setOnClickListener {
            startGallery()
        }
        binding.cameraButton.setOnClickListener {
            startCamera()
        }

        uploadImage()

    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }

    private fun uploadImage() {
        binding.buttonAdd.setOnClickListener {
            val description = binding.edAddDescription.text.toString().trim()

            currentImageUri?.let { uri ->
                // Check if the Uri scheme is content or file
                if (uri.scheme == "content" || uri.scheme == "file") {
                    try {
                        val imageFile = uriToFile(uri, this)
                        if (imageFile != null) {
                            val reducedFile = imageFile.reduceFileImage()
                            Log.d("Image File", "showImage: ${reducedFile.path}")

                            showLoading(true)

                            val requestBody = description.toRequestBody("text/plain".toMediaType())
                            val requestImageFile = reducedFile.asRequestBody("image/jpeg".toMediaType())
                            val multipartBody = MultipartBody.Part.createFormData(
                                "photo", reducedFile.name, requestImageFile
                            )

                            addStoryViewModel.getUser().observe(this) { user ->
                                addStoryViewModel.addStory(multipartBody, requestBody, lat = null, lon = null)
                            }

                            addStoryViewModel.addStoryResponse.observe(this) { response ->
                                showLoading(false)
                                if (response.error == true) {
                                    showToast(response.message.toString())
                                } else {
                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                    showToast(response.message.toString())
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        } else {
                            showToast(getString(R.string.error_image_file))
                        }
                    } catch (e: Exception) {
                        showToast(getString(R.string.error_processing_image))
                        e.printStackTrace()
                    }
                } else {
                    showToast(getString(R.string.error_invalid_uri))
                }
            } ?: showToast(getString(R.string.empty_image))
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}