package com.example.wellora

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var signUpButton: Button
    private lateinit var loginText: TextView

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        supportActionBar?.hide()

        initializeViews()
        setupClickListeners()

        db = AppDatabase.getInstance(this)
    }

    private fun initializeViews() {
        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        termsCheckbox = findViewById(R.id.termsCheckbox)
        signUpButton = findViewById(R.id.signUpButton)
        loginText = findViewById(R.id.loginText)
    }

    private fun setupClickListeners() {
        signUpButton.setOnClickListener { performSignUp() }

        loginText.setOnClickListener { finish() } // Go back to login
    }

    private fun performSignUp() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()

        // Validation
        if (name.isEmpty()) { nameInput.error = "Name is required"; nameInput.requestFocus(); return }
        if (email.isEmpty()) { emailInput.error = "Email is required"; emailInput.requestFocus(); return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailInput.error = "Please enter a valid email"; emailInput.requestFocus(); return }
        if (password.isEmpty()) { passwordInput.error = "Password is required"; passwordInput.requestFocus(); return }
        if (password.length < 6) { passwordInput.error = "Password must be at least 6 characters"; passwordInput.requestFocus(); return }
        if (confirmPassword != password) { confirmPasswordInput.error = "Passwords do not match"; confirmPasswordInput.requestFocus(); return }
        if (!termsCheckbox.isChecked) { Toast.makeText(this, "Please accept Terms of Service", Toast.LENGTH_SHORT).show(); return }

        // Check email in DB
        lifecycleScope.launch {
            val existingUser = db.userProfileDao().getUserByEmail(email)
            if (existingUser != null) {
                Toast.makeText(this@SignUpActivity, "User with this email already exists", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Insert new user
            val newUser = UserProfileEntity(
                name = name,
                email = email,
                password = password,
                age = 0,           // default values, can update later
                habitsGoal = 0,
                waterGoal = 0
            )
            db.userProfileDao().insertUser(newUser)

            Toast.makeText(this@SignUpActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()

            // Navigate to Onboarding or Home
            val intent = Intent(this@SignUpActivity, OnboardingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
