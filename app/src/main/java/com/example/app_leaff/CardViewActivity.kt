package com.example.app_leaff

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


import android.content.Intent
import android.widget.Toast

import androidx.cardview.widget.CardView



class CardViewActivity : AppCompatActivity() {

    private lateinit var profile: CardView
    private lateinit var dataset: CardView
    private lateinit var simulasi: CardView
    private lateinit var sampel: CardView
    private lateinit var logout: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_view)

        profile = findViewById(R.id.profileCard)
        dataset = findViewById(R.id.datasetCard)
        simulasi = findViewById(R.id.Simulasi)
        sampel = findViewById(R.id.Sampel) // ✅ fix: sebelumnya salah
        logout = findViewById(R.id.logoutCard)

        profile.setOnClickListener {
            showToast("Profil")
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        dataset.setOnClickListener {
            showToast("Dataset")
            val intent = Intent(this, Dataset::class.java)
            startActivity(intent)
        }

        simulasi.setOnClickListener {
            showToast("Simulasi")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        sampel.setOnClickListener {
            showToast("Sampel")
            val intent = Intent(this, Sampel::class.java)
            startActivity(intent)
        }

        logout.setOnClickListener {
            showToast("Berhasil Keluar")
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
