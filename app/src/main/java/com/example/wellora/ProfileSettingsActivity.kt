package com.example.wellora

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProfileSettingsActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var profilePicture: ImageView
    private lateinit var changePhotoButton: Button
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var ageInput: EditText
    private lateinit var habitsGoalInput: EditText
    private lateinit var waterGoalInput: EditText
    private lateinit var saveButton: Button

    private lateinit var userProfileDao: UserProfileDao
    private var userProfile: UserProfileEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        supportActionBar?.hide()
        initializeViews()

        userProfileDao = AppDatabase.getInstance(this).userProfileDao()

        loadProfileData()
        setupClickListeners()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        profilePicture = findViewById(R.id.profilePicture)
        changePhotoButton = findViewById(R.id.changePhotoButton)
        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        ageInput = findViewById(R.id.ageInput)
        habitsGoalInput = findViewById(R.id.habitsGoalInput)
        waterGoalInput = findViewById(R.id.waterGoalInput)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun loadProfileData() {
        lifecycleScope.launch {
            val profiles = userProfileDao.getAllUsers()
            userProfile = profiles.firstOrNull() ?: run {
                val newProfile = UserProfileEntity(
                    name = "",
                    email = "",
                    age = 0,
                    habitsGoal = 4,
                    waterGoal = 8
                )
                val id = userProfileDao.insertUser(newProfile)
                newProfile.copy(id = id.toInt())
            }

            userProfile?.let { profile ->
                nameInput.setText(profile.name)
                emailInput.setText(profile.email)
                ageInput.setText(if (profile.age > 0) profile.age.toString() else "")
                habitsGoalInput.setText(profile.habitsGoal.toString())
                waterGoalInput.setText(profile.waterGoal.toString())
            }
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener { finish() }

        changePhotoButton.setOnClickListener {
            Toast.makeText(this, "Photo picker coming soon", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener { saveProfileData() }
    }

    private fun saveProfileData() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val ageText = ageInput.text.toString().trim()
        val habitsGoal = habitsGoalInput.text.toString().toIntOrNull() ?: 4
        val waterGoal = waterGoalInput.text.toString().toIntOrNull() ?: 8

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }

        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageText.toIntOrNull() ?: 0
        if (age > 0 && (age < 13 || age > 120)) {
            Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            userProfile?.let { profile ->
                profile.name = name
                profile.email = email
                profile.age = age
                profile.habitsGoal = habitsGoal
                profile.waterGoal = waterGoal
                userProfileDao.updateUser(profile)
            }

            runOnUiThread {
                Toast.makeText(this@ProfileSettingsActivity, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
