package com.example.wellora

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // Hide the action bar
        supportActionBar?.hide()

        // Find buttons
        val skipButton = findViewById<Button>(R.id.skipButton)
        val nextButton = findViewById<Button>(R.id.nextButton)

        // Skip button - go directly to HomeActivity
        skipButton.setOnClickListener {
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Next button - go to next onboarding screen or MainActivity
        nextButton.setOnClickListener {
            // Go to second onboarding screen
            val intent = Intent(this, OnboardingActivity2::class.java)
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