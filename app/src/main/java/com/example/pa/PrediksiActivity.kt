package com.example.pa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
import java.io.ByteArrayOutputStream
import java.io.File

class PrediksiActivity : AppCompatActivity() {

    private lateinit var classifier: classifier
    private lateinit var imageView: ImageView
    private var selectedBitmap: Bitmap? = null
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private val db = FirebaseFirestore.getInstance()

    private lateinit var selectImageLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    private var isCameraRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.deteksi)

        val backButton: ImageButton = findViewById(R.id.button_back)
        val btnUpload: Button = findViewById(R.id.btnUpload)
        val btnKamera: Button = findViewById(R.id.btnKamera)
        val btnDeteksi: Button = findViewById(R.id.btnDeteksi)
        imageView = findViewById(R.id.imageView)

        backButton.setOnClickListener { finish() }

        classifier = classifier(this)
        classifier.loadModel()

        selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

                selectedBitmap = resizedBitmap
                imageView.setImageBitmap(resizedBitmap)
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

                selectedBitmap = resizedBitmap
                imageView.setImageBitmap(resizedBitmap)
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted && isCameraRequested) {
                openCamera()
            } else {
                Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
            }
        }

        btnUpload.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnKamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                isCameraRequested = true
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnDeteksi.setOnClickListener {
            selectedBitmap?.let { bitmap ->
                val (rawResult, confidence) = classifier.classify(bitmap)
                val confidencePercent = confidence * 100

                // Tambahkan logika untuk hasil tidak dikenali
                val result = if (confidence < 0.5f) "Tidak dikenali" else rawResult

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()
                val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)

                val currentDate = Timestamp.now()

                FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseId = task.result

                        val data = hashMapOf(
                            "result" to result,
                            "confidence" to confidencePercent,
                            "imageBase64" to base64Image,
                            "date" to currentDate,
                            "deviceId" to firebaseId
                        )

                        db.collection("predictions").add(data)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal menyimpan data: $e", Toast.LENGTH_SHORT).show()
                            }

                        // Kirim data ke HasilDeteksiActivity
                        val intent = Intent(this, HasilDeteksiActivity::class.java)
                        intent.putExtra("result", result)
                        intent.putExtra("confidence", confidencePercent)
                        intent.putExtra("image", byteArray)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Gagal mendapatkan Firebase ID", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: run {
                Toast.makeText(this, "Pilih gambar terlebih dahulu.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        photoFile = File.createTempFile("photo_", ".jpg", picturesDir)
        photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
        cameraLauncher.launch(photoUri)
    }
}
