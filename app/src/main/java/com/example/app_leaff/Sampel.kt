package com.example.app_leaff

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import androidx.cardview.widget.CardView
import com.example.app_leaff.R.id.cardView1
import com.example.app_leaff.R.id.cardView3
import com.example.app_leaff.R.id.cardview2
import com.example.app_leaff.R.id.cardview4
import com.example.apple.blackrot
import com.example.apple.cedar
import com.example.apple.healthy
import com.example.apple.scab

class Sampel : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sampel)

        // Ambil CardView by ID
        val card1 = findViewById<CardView>(cardView1)
        val card2 = findViewById<CardView>(cardview2)
        val card3 = findViewById<CardView>(cardView3)
        val card4 = findViewById<CardView>(cardview4)

        // Set onClickListener
        card1.setOnClickListener {
            val intent = Intent(this, scab::class.java)
            intent.putExtra("title", "Apple Scab")
            intent.putExtra("desc", "Apple Scab adalah penyakit jamur...")
            startActivity(intent)
        }

        card2.setOnClickListener {
            val intent = Intent(this, blackrot::class.java)
            intent.putExtra("title", "Apple Blackrot")
            intent.putExtra("desc", "Black Rot adalah penyakit...")
            startActivity(intent)
        }

        card3.setOnClickListener {
            val intent = Intent(this, cedar::class.java)
            intent.putExtra("title", "Apple Cedar")
            intent.putExtra("desc", "Cedar Apple Rust adalah penyakit...")
            startActivity(intent)
        }

        card4.setOnClickListener {
            val intent = Intent(this, healthy::class.java)
            intent.putExtra("title", "Apple Healthy")
            intent.putExtra("desc", "Daun healthy adalah kondisi...")
            startActivity(intent)
        }
    }
}
