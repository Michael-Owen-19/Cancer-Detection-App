package com.dicoding.asclepius.view

import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.icu.text.NumberFormat
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.util.Arrays
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null
    private lateinit var imageClassifierHelper: ImageClassifierHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.galleryButton.setOnClickListener{startGallery()}
        
        binding.analyzeButton.setOnClickListener {
            currentImageUri?.let {
                showLoading(true)
                val results = analyzeImage(it)
                showLoading(false)
                moveToResult(it, results)
            } ?: run {
                showToast(getString(R.string.empty_image_warning))
            }
        }
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
    
    private fun showImage() {
        currentImageUri.let { uri ->
            binding.previewImageView.setImageURI(uri)
        }
    }

    private fun analyzeImage(uri: Uri) : Array<String> {
        var analyzeResults: Array<String> = arrayOf("","")
        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            classifierListener = object : ImageClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                    results?.let { it ->
                        if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {
                            println(it)
                            val sortedCategories =
                                it[0].categories.sortedByDescending { it?.score }
                            val displayResult =
                                sortedCategories.joinToString("\n") {
                                    "${it.label} " + NumberFormat.getPercentInstance()
                                        .format(it.score).trim()
                                }
                            analyzeResults[0] = displayResult
                            analyzeResults[1] = "$inferenceTime ms"
                        }
                    }
                }
                
                
            }
        )
        
        imageClassifierHelper.classifyStaticImage(uri)
        
        return analyzeResults
    }

    private fun moveToResult(uri: Uri, results: Array<String>) {
        
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, uri.toString())
        intent.putExtra(ResultActivity.EXTRA_RESULT, results[0])
        intent.putExtra(ResultActivity.EXTRA_INFERENCE_TIME, results[1])
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}