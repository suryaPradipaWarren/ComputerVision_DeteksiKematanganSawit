package com.example.pa

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import com.example.pa.databinding.HomeBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: HomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Klik Mulai Deteksi
        binding.buttonStart.setOnClickListener {
            val intent = Intent(this, PrediksiActivity::class.java)
            startActivity(intent)
        }

        // Klik Histori
        binding.buttonUsage.setOnClickListener {
            val intent = Intent(this, HistoriActivity::class.java)
            startActivity(intent)
        }

        // Klik Tentang
        binding.buttonAbout.setOnClickListener {
            val intent = Intent(this, TentangActivity::class.java)
            startActivity(intent)
        }
    }
}
