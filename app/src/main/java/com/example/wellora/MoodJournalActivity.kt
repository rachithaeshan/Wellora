package com.example.wellora

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt

class MoodJournalActivity : AppCompatActivity() {

    private lateinit var moodChart: LineChart
    private lateinit var noteInput: EditText
    private lateinit var saveMoodButton: Button
    private var selectedMood: Int = 0
    private var selectedEmojiView: TextView? = null

    // Database DAO
    private lateinit var moodDao: MoodDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_journal)

        supportActionBar?.hide()

        // Initialize Room DAO
        moodDao = AppDatabase.getInstance(this).moodDao()

        // Initialize views
        moodChart = findViewById(R.id.moodChart)
        noteInput = findViewById(R.id.noteInput)
        saveMoodButton = findViewById(R.id.saveMoodButton)

        // Setup back button
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Setup emoji selectors
        setupEmojiSelectors()

        // Save mood button
        saveMoodButton.setOnClickListener {
            saveMoodEntry()
        }

        // Load chart data
        loadMoodChart()
    }

    private fun setupEmojiSelectors() {
        val emojiIds = listOf(
            R.id.emoji1,
            R.id.emoji2,
            R.id.emoji3,
            R.id.emoji4,
            R.id.emoji5
        )

        for (emojiId in emojiIds) {
            val emojiView = findViewById<TextView>(emojiId)
            emojiView.setOnClickListener {
                selectEmoji(it as TextView)
            }
        }
    }

    private fun selectEmoji(emojiView: TextView) {
        // Reset previous selection
        selectedEmojiView?.setBackgroundResource(R.drawable.emoji_background)

        // Highlight selected one
        selectedEmojiView = emojiView
        emojiView.setBackgroundResource(R.drawable.emoji_selected_background)

        selectedMood = emojiView.tag.toString().toInt()
    }

    private fun saveMoodEntry() {
        if (selectedMood == 0) {
            Toast.makeText(this, "Please select a mood", Toast.LENGTH_SHORT).show()
            return
        }

        val emoji = selectedEmojiView?.text.toString()
        val note = noteInput.text.toString()
        val timestamp = System.currentTimeMillis()

        val moodEntry = MoodEntryEntity(
            emoji = emoji,
            moodValue = selectedMood,
            note = note,
            timestamp = timestamp
        )

        lifecycleScope.launch(Dispatchers.IO) {
            moodDao.insertMood(moodEntry)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MoodJournalActivity, "Mood saved!", Toast.LENGTH_SHORT).show()
                resetForm()
                loadMoodChart()
            }
        }
    }

    private fun resetForm() {
        selectedEmojiView?.setBackgroundResource(R.drawable.emoji_background)
        selectedEmojiView = null
        selectedMood = 0
        noteInput.text.clear()
    }

    private fun loadMoodChart() {
        lifecycleScope.launch(Dispatchers.IO) {
            val entries = moodDao.getAllMoods().sortedBy { it.timestamp }

            withContext(Dispatchers.Main) {
                if (entries.isEmpty()) {
                    setupChartWithSampleData()
                    return@withContext
                }

                // Filter for last 7 days
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                val recentEntries = entries.filter { it.timestamp >= sevenDaysAgo }

                val dailyMoods = mutableMapOf<String, MutableList<Int>>()
                val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

                for (entry in recentEntries) {
                    val date = dateFormat.format(Date(entry.timestamp))
                    if (!dailyMoods.containsKey(date)) {
                        dailyMoods[date] = mutableListOf()
                    }
                    dailyMoods[date]?.add(entry.moodValue)
                }

                val chartEntries = mutableListOf<Entry>()
                val labels = mutableListOf<String>()

                dailyMoods.entries.forEachIndexed { index, entry ->
                    val avgMood = entry.value.average().toFloat()
                    chartEntries.add(Entry(index.toFloat(), avgMood))
                    labels.add(entry.key)
                }

                setupChart(chartEntries, labels)
            }
        }
    }

    private fun setupChartWithSampleData() {
        val entries = listOf(
            Entry(0f, 3f),
            Entry(1f, 4f),
            Entry(2f, 3.5f),
            Entry(3f, 4.5f),
            Entry(4f, 4f),
            Entry(5f, 5f),
            Entry(6f, 4.5f)
        )

        val calendar = Calendar.getInstance()
        val labels = mutableListOf<String>()
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        for (i in 6 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            labels.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, i)
        }

        setupChart(entries, labels)
    }

    private fun setupChart(entries: List<Entry>, labels: List<String>) {
        val dataSet = LineDataSet(entries, "Mood Level")
        dataSet.color = "#1976D2".toColorInt()
        dataSet.setCircleColor("#1976D2".toColorInt())
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 6f
        dataSet.setDrawCircleHole(true)
        dataSet.circleHoleColor = Color.WHITE
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = "#1976D2".toColorInt()
        dataSet.setDrawFilled(true)
        dataSet.fillColor = "#E3F2FD".toColorInt()
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(dataSet)
        moodChart.data = lineData

        moodChart.description.isEnabled = false
        moodChart.setDrawGridBackground(false)
        moodChart.setTouchEnabled(true)
        moodChart.isDragEnabled = true
        moodChart.setScaleEnabled(false)
        moodChart.legend.isEnabled = false

        val xAxis = moodChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = "#666666".toColorInt()
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value.toInt() < labels.size) labels[value.toInt()] else ""
            }
        }

        moodChart.axisLeft.axisMinimum = 0f
        moodChart.axisLeft.axisMaximum = 6f
        moodChart.axisLeft.setDrawGridLines(true)
        moodChart.axisLeft.gridColor = "#E0E0E0".toColorInt()
        moodChart.axisLeft.textColor = "#666666".toColorInt()

        moodChart.axisRight.isEnabled = false

        moodChart.animateX(1000)
        moodChart.invalidate()
    }
}
