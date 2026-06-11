package com.example.wellora

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding2)

        supportActionBar?.hide()

        val skipButton = findViewById<Button>(R.id.skipButton)
        val nextButton = findViewById<Button>(R.id.nextButton)

        skipButton.setOnClickListener {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }

        nextButton.setOnClickListener {
            // Go to third onboarding screen
            val intent = Intent(this, OnboardingActivity3::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}