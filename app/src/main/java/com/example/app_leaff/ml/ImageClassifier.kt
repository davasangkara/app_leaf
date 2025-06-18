package com.example.app_leaff.ml // Ganti dengan package Anda yang benar

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ImageClassifier(assetManager: AssetManager, modelPath: String = "model.tflite", labelPath: String = "labels.txt") {

    private val interpreter: Interpreter
    private val labelList: List<String>
    private val TAG = "ImageClassifier"

    init {
        Log.d(TAG, "Menginisialisasi ImageClassifier dengan model: $modelPath dan label: $labelPath")
        try {
            interpreter = Interpreter(loadModelFile(assetManager, modelPath))
            labelList = loadLabelList(assetManager, labelPath)
            Log.d(TAG, "Label berhasil dimuat. Jumlah label: ${labelList.size}")
            if (labelList.isEmpty()) {
                Log.e(TAG, "Daftar label kosong! Pastikan $labelPath ada dan berisi data.")
            } else {
                Log.d(TAG, "Contoh label pertama (mentah): ${labelList.firstOrNull()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal inisialisasi ImageClassifier atau memuat file aset.", e)
            throw IOException("Gagal memuat model atau label dari assets. Periksa $modelPath dan $labelPath.", e)
        }
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        Log.d(TAG, "Model file '$modelPath' berhasil dibuka.")
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun loadLabelList(assetManager: AssetManager, labelPath: String): List<String> {
        Log.d(TAG, "Mencoba memuat daftar label dari: $labelPath")
        return assetManager.open(labelPath).bufferedReader().readLines().mapNotNull { it.ifBlank { null } }
    }

    fun classifyImage(inputBuffer: ByteBuffer): Pair<String, Float> {
        // Pastikan ukuran outputArray sesuai dengan jumlah label di model Anda
        // Jika model Anda memiliki X kelas, maka FloatArray(X)
        if (labelList.isEmpty()) {
            Log.e(TAG, "classifyImage dipanggil tetapi daftar label kosong.")
            return "Error: Label Kosong" to 0f
        }
        val outputArray = Array(1) { FloatArray(labelList.size) }

        try {
            interpreter.run(inputBuffer, outputArray)
        } catch (e: Exception) {
            Log.e(TAG, "Error saat menjalankan interpreter.run()", e)
            return "Error: Prediksi Gagal" to 0f
        }


        val maxIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] }
        if (maxIndex == null) {
            Log.e(TAG, "Tidak dapat menemukan maxIndex di outputArray.")
            return "Error: Indeks Tidak Ditemukan" to 0f
        }

        val score = outputArray[0][maxIndex]
        val rawLabel = labelList.getOrNull(maxIndex) ?: "Unknown"

        // Memproses rawLabel untuk mengambil nama label sebenarnya (setelah spasi pertama jika ada angka di depan)
        val actualLabel = rawLabel.substringAfter(' ', rawLabel).trim()

        Log.d(TAG, "Klasifikasi: Label Mentah='$rawLabel', Label Aktual='$actualLabel', Skor=$score")
        return actualLabel to score
    }
}