package com.example.pa

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.installations.FirebaseInstallations
import java.text.SimpleDateFormat
import java.util.*

data class Prediction(
    val result: String = "",
    val imageBase64: String = "",
    val confidence: Float = 0f,
    val timestamp: Timestamp? = null
)

class HistoriActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PredictionAdapter
    private lateinit var btnFilterTanggal: Button
    private val db = FirebaseFirestore.getInstance()
    private var allPredictions: List<Prediction> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.histori)

        recyclerView = findViewById(R.id.recyclerView)
        btnFilterTanggal = findViewById(R.id.btnFilterTanggal)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PredictionAdapter()
        recyclerView.adapter = adapter

        // Ganti pemanggilan Android ID dengan Firebase Installation ID
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseId = task.result
                Log.d("HistoriActivity", "Firebase Installation ID: $firebaseId")
                loadPredictions(firebaseId)
            } else {
                Toast.makeText(this, "Gagal mendapatkan Firebase ID", Toast.LENGTH_SHORT).show()
                Log.e("HistoriActivity", "Gagal mendapatkan Firebase ID: ${task.exception?.message}")
            }
        }

        btnFilterTanggal.setOnClickListener {
            showDatePicker()
        }

        findViewById<ImageButton>(R.id.button_back_result).setOnClickListener {
            finish()
        }
    }

    private fun loadPredictions(deviceId: String) {
        db.collection("predictions")
            .whereEqualTo("deviceId", deviceId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("HistoriActivity", "Jumlah dokumen ditemukan: ${documents.size()}")

                allPredictions = documents.mapNotNull { doc ->
                    val result = doc.getString("result")
                    val imageBase64 = doc.getString("imageBase64")
                    val confidence = doc.getDouble("confidence")?.toFloat()

                    val rawDate = doc.get("date")
                    val timestamp = when (rawDate) {
                        is Timestamp -> rawDate
                        is Date -> Timestamp(rawDate)
                        is Long -> Timestamp(Date(rawDate))
                        else -> {
                            Log.w("HistoriActivity", "Field 'date' tidak terbaca di dokumen ${doc.id}")
                            null
                        }
                    }

                    if (result != null && imageBase64 != null && confidence != null) {
                        Prediction(result, imageBase64, confidence, timestamp)
                    } else {
                        Log.w("HistoriActivity", "Data tidak lengkap di dokumen ${doc.id}")
                        null
                    }
                }

                adapter.setData(allPredictions)
            }
            .addOnFailureListener { e ->
                Log.e("HistoriActivity", "Gagal mengambil data Firestore: ${e.message}", e)
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this,
            { _, year, month, day ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                filterByDate(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun filterByDate(selectedDate: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val filtered = allPredictions.filter { pred ->
            val dateStr = pred.timestamp?.toDate()?.let { sdf.format(it) }
            dateStr == selectedDate
        }

        adapter.setData(filtered)

        if (filtered.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk tanggal $selectedDate", Toast.LENGTH_SHORT).show()
        }
    }

    class PredictionAdapter : RecyclerView.Adapter<PredictionAdapter.ViewHolder>() {
        private var predictions: List<Prediction> = emptyList()

        fun setData(newData: List<Prediction>) {
            predictions = newData
            notifyDataSetChanged()
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imagePrediction)
            val resultView: TextView = itemView.findViewById(R.id.textResult)
            val accuracyView: TextView = itemView.findViewById(R.id.textAccuracy)
            val dateView: TextView = itemView.findViewById(R.id.textDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_prediction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val prediction = predictions[position]

            val imageBytes = Base64.decode(prediction.imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            holder.imageView.setImageBitmap(bitmap)

            val resultClean = prediction.result.substringAfter(' ', prediction.result)
            holder.resultView.text = resultClean
            holder.accuracyView.text = "Akurasi: %.2f%%".format(prediction.confidence)

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            holder.dateView.text = prediction.timestamp?.toDate()?.let { sdf.format(it) } ?: "-"
        }

        override fun getItemCount(): Int = predictions.size
    }
}
