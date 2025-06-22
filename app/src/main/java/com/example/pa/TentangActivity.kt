package com.example.pa

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class TentangActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tentang)

        val backButton: ImageButton = findViewById(R.id.button_back)
        backButton.setOnClickListener {
            finish() // Kembali ke halaman sebelumnya
        }
    }
}
