package com.example.app_leaff // Ganti dengan package Anda yang benar

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
import com.example.app_leaff.ml.ImageClassifier // Pastikan path ImageClassifier benar
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
        setContentView(R.layout.activity_main) // Pastikan layout Anda bernama activity_main

        // Inisialisasi ImageClassifier
        // Pastikan file model "model.tflite" dan "labels.txt" ada di folder assets
        try {
            imageClassifier = ImageClassifier(assets)
        } catch (e: IOException) {
            Log.e(TAG, "Gagal inisialisasi ImageClassifier.", e)
            textViewResult.text = "Error: Gagal memuat model klasifikasi. Aplikasi tidak dapat berfungsi."
            // Anda mungkin ingin menonaktifkan tombol jika imageClassifier gagal dimuat
            return // Keluar dari onCreate jika imageClassifier gagal
        }


        // Muat informasi penyakit dari file TXT baru
        // Pastikan file "disease_info.txt" ada di folder assets
        labelInfo = loadDiseaseInfoFromTxt("disease_info.txt")
        Log.d(TAG, "onCreate - labelInfo map initialized. Size: ${labelInfo.size}. Keys: ${labelInfo.keys}")

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
                Log.d(TAG, "loadDiseaseInfoFromTxt - Memulai membaca file: $fileName")
                lines.forEach { line ->
                    if (line.isNotBlank()) { // Lewati baris kosong jika ada
                        val tokens = line.split('|', limit = 3) // Menggunakan '|' sebagai pemisah
                        if (tokens.size == 3) {
                            val label = tokens[0].trim()
                            val cause = tokens[1].trim()
                            val solution = tokens[2].trim()
                            // Log setiap entri yang berhasil diparsing
                            Log.d(TAG, "loadDiseaseInfoFromTxt - Parsed: Label='${label}', Cause='${cause.take(30)}...', Solution='${solution.take(30)}...'")
                            mutableMap[label] = Pair(cause, solution)
                        } else {
                            Log.w(TAG, "loadDiseaseInfoFromTxt - Melewatkan baris TXT (token != 3 setelah split dengan '|'): $line. Tokens found: ${tokens.size}")
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "loadDiseaseInfoFromTxt - Gagal memuat informasi penyakit dari $fileName", e)
            runOnUiThread {
                textViewResult.text = "Error: Gagal memuat data penyakit dari $fileName. Periksa Logcat."
            }
        }
        Log.d(TAG, "loadDiseaseInfoFromTxt - Selesai memuat. Ukuran map: ${mutableMap.size}. Kunci: ${mutableMap.keys}")
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
                        Log.e(TAG, "Gagal membuka gambar dari URI.", e)
                        null
                    }
                }
                REQUEST_IMAGE_CAPTURE -> data?.extras?.get("data") as? Bitmap
                else -> null
            }

            bitmap?.let {
                val resized = Bitmap.createScaledBitmap(it, 224, 224, true) // Sesuaikan ukuran jika model Anda berbeda
                imageView.setImageBitmap(resized)
                classifyImage(resized)
            } ?: run {
                textViewResult.text = "Gagal mengambil atau memproses gambar."
                Log.w(TAG, "onActivityResult - Bitmap null atau gagal diproses")
            }
        }
    }

    private fun classifyImage(bitmap: Bitmap) {
        if (!::imageClassifier.isInitialized) {
            Log.e(TAG, "classifyImage - ImageClassifier belum diinisialisasi.")
            textViewResult.text = "Error: Klasifikasi tidak siap."
            return
        }

        val inputBuffer = convertBitmapToByteBuffer(bitmap)
        val (resultLabel, score) = imageClassifier.classifyImage(inputBuffer)

        Log.d(TAG, "classifyImage - Mencoba mencari label: '$resultLabel'")
        Log.d(TAG, "classifyImage - Kunci yang tersedia di labelInfo: ${labelInfo.keys}")
        if (!labelInfo.containsKey(resultLabel) && resultLabel != "Error" && resultLabel != "Unknown") {
            Log.e(TAG, "classifyImage - KUNCI TIDAK DITEMUKAN DI MAP: '$resultLabel'")
            labelInfo.keys.forEach { key ->
                if (key.equals(resultLabel, ignoreCase = true) && key != resultLabel) {
                    Log.w(TAG, "classifyImage - Kemungkinan masalah case sensitivity atau spasi? Ditemukan kunci mirip: '$key'")
                }
            }
        }

        val (penyebab, solusi) = labelInfo[resultLabel] ?: Pair(
            "Informasi tidak ditemukan untuk label: $resultLabel.",
            "Pastikan label ada di file disease_info.txt dan formatnya benar (NamaLabel|Penyebab|Solusi)."
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
        // Pastikan dimensi ini (224x224) dan jumlah channel (3 untuk RGB)
        // sesuai dengan input yang diharapkan model TFLite Anda.
        val inputSize = 224
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3) // 4 bytes per float
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val `val` = pixels[pixel++]
                // Normalisasi piksel ke [0,1] atau [-1,1] tergantung model Anda
                // Umumnya untuk model image classification adalah [0,1]
                buffer.putFloat(((`val` shr 16) and 0xFF) / 255.0f)
                buffer.putFloat(((`val` shr 8) and 0xFF) / 255.0f)
                buffer.putFloat((`val` and 0xFF) / 255.0f)
            }
        }
        return buffer
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
    }
}