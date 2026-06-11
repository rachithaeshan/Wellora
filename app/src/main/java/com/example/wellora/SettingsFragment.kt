package com.example.wellora

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.core.content.edit

class SettingsFragment : Fragment() {

    private lateinit var editProfileOption: LinearLayout
    private lateinit var changeGoalsOption: LinearLayout
    private lateinit var habitRemindersSwitch: SwitchCompat
    private lateinit var moodRemindersSwitch: SwitchCompat
    private lateinit var waterRemindersSwitch: SwitchCompat
    private lateinit var exportDataOption: LinearLayout
    private lateinit var clearDataOption: LinearLayout
    private lateinit var aboutAppOption: LinearLayout

    private val sharedPrefsName = "SettingsPrefs"

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize views
        editProfileOption = view.findViewById(R.id.editProfileOption)
        changeGoalsOption = view.findViewById(R.id.changeGoalsOption)
        habitRemindersSwitch = view.findViewById(R.id.habitRemindersSwitch)
        moodRemindersSwitch = view.findViewById(R.id.moodRemindersSwitch)
        waterRemindersSwitch = view.findViewById(R.id.waterRemindersSwitch)
        exportDataOption = view.findViewById(R.id.exportDataOption)
        clearDataOption = view.findViewById(R.id.clearDataOption)
        aboutAppOption = view.findViewById(R.id.aboutAppOption)

        // Load saved settings
        loadSettings()

        // Setup click listeners
        setupClickListeners()

        return view
    }

    private fun loadSettings() {
        val sharedPrefs = requireContext().getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)

        habitRemindersSwitch.isChecked = sharedPrefs.getBoolean("habit_reminders", true)
        moodRemindersSwitch.isChecked = sharedPrefs.getBoolean("mood_reminders", true)
        waterRemindersSwitch.isChecked = sharedPrefs.getBoolean("water_reminders", true)
    }

    private fun setupClickListeners() {
        // Edit Profile
        editProfileOption.setOnClickListener {
            val intent = Intent(requireContext(), ProfileSettingsActivity::class.java)
            startActivity(intent)
        }

        // Change Goals
        changeGoalsOption.setOnClickListener {
            showGoalsDialog()
        }

        // Habit Reminders Switch
        habitRemindersSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveSettingToPrefs("habit_reminders", isChecked)
            Toast.makeText(
                requireContext(),
                "Habit reminders ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Mood Reminders Switch
        moodRemindersSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveSettingToPrefs("mood_reminders", isChecked)
            Toast.makeText(
                requireContext(),
                "Mood reminders ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Water Reminders Switch
        waterRemindersSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveSettingToPrefs("water_reminders", isChecked)
            Toast.makeText(
                requireContext(),
                "Water reminders ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Export Data
        exportDataOption.setOnClickListener {
            exportData()
        }

        // Clear Data
        clearDataOption.setOnClickListener {
            showClearDataDialog()
        }

        // About App
        aboutAppOption.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun saveSettingToPrefs(key: String, value: Boolean) {
        val sharedPrefs = requireContext().getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
        sharedPrefs.edit { putBoolean(key, value) }
    }

    private fun showGoalsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Daily Goals")
            .setMessage("Set your daily wellness goals:\n\n• Habits: Track 4 habits daily\n• Water: Drink 8 glasses daily\n• Mood: Log mood once daily")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun exportData() {
        // Create a simple text summary of data
        val habitPrefs = requireContext().getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)
        val moodPrefs = requireContext().getSharedPreferences("MoodJournalPrefs", Context.MODE_PRIVATE)
        val hydrationPrefs = requireContext().getSharedPreferences("HydrationPrefs", Context.MODE_PRIVATE)

        val summary = """
            WellTrack Data Export
            =====================
            
            Habits: ${habitPrefs.all.size} entries
            Moods: ${moodPrefs.getString("mood_entries", "[]")?.length ?: 0} characters
            Water: ${hydrationPrefs.getInt("water_count", 0)} glasses today
            
            Export Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
        """.trimIndent()

        // Share using intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "WellTrack Data Export")
            putExtra(Intent.EXTRA_TEXT, summary)
        }

        startActivity(Intent.createChooser(shareIntent, "Export Data"))
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear All Data")
            .setMessage("Are you sure you want to delete all your data? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                clearAllData()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun clearAllData() {
        // Clear all SharedPreferences
        val prefsNames = listOf("HabitPrefs", "MoodJournalPrefs", "HydrationPrefs", "SettingsPrefs")

        for (prefsName in prefsNames) {
            requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .edit {
                    clear()
                }
        }

        Toast.makeText(requireContext(), "All data cleared", Toast.LENGTH_SHORT).show()

        // Reload settings
        loadSettings()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("About WellTrack")
            .setMessage("""
                WellTrack - Your Daily Wellness Companion
                
                Version: 1.0.0
                
                Features:
                • Daily Habit Tracker
                • Mood Journal with Charts
                • Hydration Reminder
                • Customizable Settings
                
                Developed with ❤️ for your wellness journey
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}