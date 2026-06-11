package com.example.wellora

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.*

class HydrationFragment : Fragment() {

    private lateinit var waterProgressText: TextView
    private lateinit var waterProgressBar: ProgressBar
    private lateinit var remainingText: TextView
    private lateinit var addWaterButton: Button
    private lateinit var add2GlassesButton: Button
    private lateinit var add3GlassesButton: Button
    private lateinit var reminderSwitch: SwitchCompat
    private lateinit var intervalSeekBar: SeekBar
    private lateinit var intervalText: TextView

    private lateinit var hydrationDao: HydrationDao
    private var todayEntry: HydrationEntity? = null

    private val reminderHandler = Handler()
    private var reminderRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_hydration, container, false)

        // Bind UI elements
        waterProgressText = view.findViewById(R.id.waterProgressText)
        waterProgressBar = view.findViewById(R.id.waterProgressBar)
        remainingText = view.findViewById(R.id.remainingText)
        addWaterButton = view.findViewById(R.id.addWaterButton)
        add2GlassesButton = view.findViewById(R.id.add2GlassesButton)
        add3GlassesButton = view.findViewById(R.id.add3GlassesButton)
        reminderSwitch = view.findViewById(R.id.reminderSwitch)
        intervalSeekBar = view.findViewById(R.id.intervalSeekBar)
        intervalText = view.findViewById(R.id.intervalText)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Initialize DAO
        hydrationDao = AppDatabase.getInstance(requireContext()).hydrationDao()
        setupUi()
        return view
    }

    private fun setupUi() {
        intervalSeekBar.max = 5 // 10–60 minutes

        lifecycleScope.launch {
            loadTodayEntry()
            updateUi()
        }

        // Water buttons
        addWaterButton.setOnClickListener { incrementWater(1) }
        add2GlassesButton.setOnClickListener { incrementWater(2) }
        add3GlassesButton.setOnClickListener { incrementWater(3) }

        // Reminder switch
        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            todayEntry?.let {
                it.reminderEnabled = isChecked
                lifecycleScope.launch { saveTodayEntry(it) }
                if (isChecked) startReminders() else stopReminders()
            }
        }

        // Interval SeekBar
        intervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = (progress + 1) * 10
                updateIntervalText(minutes)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val minutes = ((seekBar?.progress ?: 0) + 1) * 10
                todayEntry?.let {
                    it.reminderInterval = minutes
                    lifecycleScope.launch { saveTodayEntry(it) }
                    if (it.reminderEnabled) {
                        stopReminders()
                        startReminders()
                    }
                }
            }
        })
    }

    /** Load or create today's entry **/
    private suspend fun loadTodayEntry() {
        val today = getStartOfDay(System.currentTimeMillis())
        todayEntry = hydrationDao.getEntryForDate(today) ?: run {
            val newEntry = HydrationEntity(
                glassesDrunk = 0,
                reminderEnabled = false,
                reminderInterval = 30,
                date = today
            )
            val id = hydrationDao.insertHydration(newEntry)
            newEntry.copy(id = id.toInt())
        }
    }

    /** Increment water **/
    private fun incrementWater(amount: Int) {
        todayEntry?.let {
            it.glassesDrunk = (it.glassesDrunk + amount).coerceAtMost(8)
            lifecycleScope.launch { saveTodayEntry(it) }
            updateUi()
        }
    }

    /** Update UI **/
    @SuppressLint("SetTextI18n")
    private fun updateUi() {
        val count = todayEntry?.glassesDrunk ?: 0
        val goal = 8
        waterProgressBar.max = goal
        waterProgressBar.progress = count
        waterProgressText.text = "$count / $goal"
        remainingText.text = "${(goal - count).coerceAtLeast(0)} glasses remaining"

        reminderSwitch.isChecked = todayEntry?.reminderEnabled ?: false
        val intervalMinutes = todayEntry?.reminderInterval ?: 30
        intervalSeekBar.progress = (intervalMinutes / 10) - 1
        updateIntervalText(intervalMinutes)
    }

    /** Save entry **/
    private suspend fun saveTodayEntry(entry: HydrationEntity) {
        hydrationDao.updateHydration(entry)
    }

    /** Update interval text **/
    private fun updateIntervalText(minutes: Int) {
        intervalText.text = if (minutes == 1) "Every 1 minute" else "Every $minutes minutes"
    }

    /** Show notification **/
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showHydrationNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(requireContext(), "hydration_channel")
            .setContentTitle("💧 Time to Drink Water")
            .setContentText("Stay hydrated! Take a sip now.")
            .setSmallIcon(R.drawable.ic_water)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(requireContext()).notify(1, notification)
    }

    /** Start reminders **/
    private fun startReminders() {
        stopReminders()
        val intervalMillis = (todayEntry?.reminderInterval ?: 30) * 60 * 1000L
        reminderRunnable = object : Runnable {
            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
            override fun run() {
                showHydrationNotification()
                reminderHandler.postDelayed(this, intervalMillis)
            }
        }
        reminderHandler.postDelayed(reminderRunnable!!, intervalMillis)
    }

    /** Stop reminders **/
    private fun stopReminders() {
        reminderRunnable?.let { reminderHandler.removeCallbacks(it) }
    }

    /** Get start of day **/
    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopReminders()
    }
}
