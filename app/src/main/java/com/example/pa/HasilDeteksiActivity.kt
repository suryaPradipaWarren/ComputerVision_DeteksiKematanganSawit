package com.example.pa

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HasilDeteksiActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hasil_deteksi)

        val backButton: ImageButton = findViewById(R.id.button_back_result)
        val resultImageView: ImageView = findViewById(R.id.resultImageView)
        val resultTextView: TextView = findViewById(R.id.resultTextView)
        val detailTextView: TextView = findViewById(R.id.textPenjelasanDeteksi)
        val confidenceTextView: TextView = findViewById(R.id.textConfidence)

        // Ambil data dari intent
        val byteArray = intent.getByteArrayExtra("image")
        val rawResult = intent.getStringExtra("result") ?: "Tidak dikenali"
        val confidence = intent.getFloatExtra("confidence", 0f)

        // Tampilkan gambar hasil
        byteArray?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            resultImageView.setImageBitmap(bitmap)
        }

        // Bersihkan hasil prediksi (misal "0 Mentah" menjadi "Mentah")
        val result = rawResult.replace(Regex("^\\d+\\s*"), "").trim()

        // Tampilkan hasil deteksi
        resultTextView.text = "Hasil Deteksi: $result"

        // Tampilkan confidence
        val formattedConfidence = String.format("%.2f", confidence)
        confidenceTextView.text = "Akurasi: $formattedConfidence%"

        // Penjelasan per kelas
        val penjelasan = when (result.lowercase()) {
            "mentah" -> "Buah sawit dalam kondisi mentah, belum menunjukkan warna matang, dan kadar minyaknya rendah. Disarankan menunggu sebelum panen."
            "mengkal" -> "Buah sawit mulai berubah warna namun belum sepenuhnya matang. Tunggu beberapa hari agar hasil panen optimal."
            "matang" -> "Buah sawit matang sempurna dengan warna merah tua. Ini adalah waktu terbaik untuk panen agar hasil maksimal."
            "busuk" -> "Buah sawit sudah membusuk dan tidak layak panen. Kandungan minyaknya telah menurun drastis."
            else -> "Sistem tidak dapat mengenali gambar. Pastikan kualitas gambar baik dan coba ulangi proses deteksi."
        }

        detailTextView.text = penjelasan

        // Tombol kembali
        backButton.setOnClickListener { finish() }
    }
}
