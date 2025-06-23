package com.example.app_leaff

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.app_leaff.ml.ImageClassifier
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

    private lateinit var imageClassifier: ImageClassifier
    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private lateinit var labelInfo: Map<String, Pair<String, String>>

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            imageClassifier = ImageClassifier(assets)
        } catch (e: IOException) {
            Log.e(TAG, "Gagal inisialisasi ImageClassifier.", e)
            return
        }

        labelInfo = loadDiseaseInfoFromTxt("disease_info.txt")

        imageView = findViewById(R.id.imageView)
        textViewResult = findViewById(R.id.textView_result)

        val buttonSelectImage: Button = findViewById(R.id.button_select_image)
        val buttonCaptureImage: Button = findViewById(R.id.button_capture_image)

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        buttonCaptureImage.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun loadDiseaseInfoFromTxt(fileName: String): Map<String, Pair<String, String>> {
        val mutableMap = mutableMapOf<String, Pair<String, String>>()
        try {
            assets.open(fileName).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        val tokens = line.split('|', limit = 3)
                        if (tokens.size == 3) {
                            val label = tokens[0].trim()
                            val cause = tokens[1].trim()
                            val solution = tokens[2].trim()
                            mutableMap[label] = Pair(cause, solution)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Gagal membaca file: $fileName", e)
        }
        return mutableMap
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val bitmap: Bitmap? = when (requestCode) {
                REQUEST_IMAGE_PICK -> data?.data?.let { uri ->
                    try {
                        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    } catch (e: Exception) {
                        Log.e(TAG, "Gagal membuka gambar dari galeri.", e)
                        null
                    }
                }
                REQUEST_IMAGE_CAPTURE -> data?.extras?.get("data") as? Bitmap
                else -> null
            }

            bitmap?.let {
                val resized = Bitmap.createScaledBitmap(it, 224, 224, true)
                imageView.setImageBitmap(resized)
                classifyImage(resized)
            } ?: run {
                textViewResult.text = "Gagal memuat gambar."
            }
        }
    }

    private fun classifyImage(bitmap: Bitmap) {
        val inputBuffer = convertBitmapToByteBuffer(bitmap)
        val (resultLabel, score) = imageClassifier.classifyImage(inputBuffer)

        // 🔒 Batasi hanya jika gambar benar-benar daun
        val allowedLabels = listOf("Apple Scab", "Apple Cedar", "Apple Black Rot", "Healthy")
        if (resultLabel !in allowedLabels) {
            textViewResult.text = """
                ❌ Gambar bukan daun atau tidak dikenali.
                Harap unggah gambar daun apel yang valid.
            """.trimIndent()
            return
        }

        val (penyebab, solusi) = labelInfo[resultLabel] ?: Pair(
            "Informasi tidak ditemukan.",
            "Periksa kembali label di file disease_info.txt."
        )

        textViewResult.text = """
            🧾 Hasil Prediksi: $resultLabel
            🎯 Tingkat Keyakinan: ${(score * 100).toInt()}%
            
            🧠 Penyebab:
            $penyebab

            ✅ Solusi:
            $solusi
        """.trimIndent()
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 224
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            buffer.putFloat((pixel and 0xFF) / 255.0f)
        }

        return buffer
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
    }
}
