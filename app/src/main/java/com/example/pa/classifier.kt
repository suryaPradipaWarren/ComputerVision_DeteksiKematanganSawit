package com.example.pa

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class classifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private lateinit var labels: List<String> // Simpan label supaya tidak baca file berulang-ulang
    private val IMAGE_SIZE = 224
    private val CONFIDENCE_THRESHOLD = 0.75f // Ambang batas klasifikasi

    // Memuat model TensorFlow Lite
    fun loadModel() {
        try {
            val fileDescriptor = context.assets.openFd("model_unquant.tflite")
            val inputStream = fileDescriptor.createInputStream()
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            interpreter = Interpreter(modelBuffer)

            // Load labels
            val reader = BufferedReader(InputStreamReader(context.assets.open("labels.txt")))
            labels = reader.readLines()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Menjalankan prediksi dan mengembalikan label + confidence
    fun classify(bitmap: Bitmap): Pair<String, Float> {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        val output = Array(1) { FloatArray(labels.size) }
        interpreter?.run(byteBuffer, output)

        return getLabelAndConfidence(output[0])
    }

    // Mengubah Bitmap ke ByteBuffer
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixelIndex = 0
        for (i in 0 until IMAGE_SIZE) {
            for (j in 0 until IMAGE_SIZE) {
                val pixel = intValues[pixelIndex++]
                byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
                byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
                byteBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
            }
        }
        return byteBuffer
    }

    // Mengambil label dengan confidence tertinggi dan filter jika di bawah ambang batas
    private fun getLabelAndConfidence(output: FloatArray): Pair<String, Float> {
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: -1
        if (maxIndex != -1) {
            val confidence = output[maxIndex]
            val label = labels[maxIndex]

            Log.d("Classifier", "Confidence Output: ${output.joinToString()}") // Debug (opsional)

            return if (confidence < CONFIDENCE_THRESHOLD) {
                Pair("Tidak dikenali", confidence)
            } else {
                Pair(label, confidence)
            }
        }
        return Pair("Unknown", 0f)
    }
}
