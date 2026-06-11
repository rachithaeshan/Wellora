package com.example.wellora

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity



class OnboardingActivity3 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding3)

        supportActionBar?.hide()

        val skipButton = findViewById<Button>(R.id.skipButton)
        val getStartedButton = findViewById<Button>(R.id.nextButton)

        skipButton.setOnClickListener {
            val intent = Intent(this, OnboardingActivity2::class.java)
            startActivity(intent)
            finish()
        }
        getStartedButton.setOnClickListener {
            // Go to third onboarding screen
            val intent = Intent(this, HomeActivity::class.java)
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