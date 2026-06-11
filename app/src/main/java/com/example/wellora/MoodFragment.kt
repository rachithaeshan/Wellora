package com.example.wellora

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt

class MoodFragment : Fragment(R.layout.fragment_mood) {

    private lateinit var moodChart: LineChart
    private lateinit var noteInput: EditText
    private lateinit var saveMoodButton: Button
    private var selectedMood: Int = 0
    private var selectedEmojiView: TextView? = null

    private lateinit var db: AppDatabase
    private lateinit var moodDao: MoodDao

    private lateinit var moodEntriesContainer: LinearLayout
    private lateinit var emptyEntriesState: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())
        moodDao = db.moodDao()

        moodChart = view.findViewById(R.id.moodChart)
        noteInput = view.findViewById(R.id.noteInput)
        saveMoodButton = view.findViewById(R.id.saveMoodButton)
        moodEntriesContainer = view.findViewById(R.id.moodEntriesContainer)
        emptyEntriesState = view.findViewById(R.id.emptyEntriesState)

        setupEmojiSelectors(view)
        setupSaveButton()
        loadMoodData()
    }

    private fun setupEmojiSelectors(view: View) {
        val emojiIds = listOf(R.id.emoji1, R.id.emoji2, R.id.emoji3, R.id.emoji4, R.id.emoji5)
        for (emojiId in emojiIds) {
            val emojiView = view.findViewById<TextView>(emojiId)
            emojiView.setOnClickListener { selectEmoji(it as TextView) }
        }
    }

    private fun selectEmoji(emojiView: TextView) {
        selectedEmojiView?.setBackgroundResource(R.drawable.emoji_background)
        selectedEmojiView = emojiView
        emojiView.setBackgroundResource(R.drawable.emoji_selected_background)
        selectedMood = emojiView.tag.toString().toInt()
    }

    private fun setupSaveButton() {
        saveMoodButton.setOnClickListener {
            if (selectedMood == 0) {
                Toast.makeText(requireContext(), "Please select a mood", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emoji = selectedEmojiView?.text.toString()
            val note = noteInput.text.toString()

            val newMood = MoodEntryEntity(
                emoji = emoji,
                moodValue = selectedMood,
                note = note,
                timestamp = System.currentTimeMillis()
            )

            lifecycleScope.launch {
                moodDao.insertMood(newMood)
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Mood saved!", Toast.LENGTH_SHORT).show()
                    noteInput.text.clear()
                    selectedEmojiView?.setBackgroundResource(R.drawable.emoji_background)
                    selectedEmojiView = null
                    selectedMood = 0
                    loadMoodData()
                }
            }
        }
    }

    private fun loadMoodData() {
        lifecycleScope.launch {
            val allEntries = moodDao.getAllMoods().sortedBy { it.timestamp }
            requireActivity().runOnUiThread {
                // Update chart
                if (allEntries.isEmpty()) setupChartWithSampleData()
                else setupChartWithRealData(allEntries)

                // Update recent entries
                updateRecentEntries(allEntries)
            }
        }
    }

    private fun setupChartWithRealData(entries: List<MoodEntryEntity>) {
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val recentEntries = entries.filter { it.timestamp >= sevenDaysAgo }

        val dailyMoods = mutableMapOf<String, MutableList<Int>>()
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        for (entry in recentEntries) {
            val date = dateFormat.format(Date(entry.timestamp))
            dailyMoods.getOrPut(date) { mutableListOf() }.add(entry.moodValue)
        }

        val chartEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        dailyMoods.entries.forEachIndexed { index, entry ->
            chartEntries.add(Entry(index.toFloat(), entry.value.average().toFloat()))
            labels.add(entry.key)
        }

        setupChart(chartEntries, labels)
    }

    @SuppressLint("SetTextI18n")
    private fun updateRecentEntries(entries: List<MoodEntryEntity>) {
        moodEntriesContainer.removeAllViews()

        if (entries.isEmpty()) {
            emptyEntriesState.visibility = View.VISIBLE
            return
        } else {
            emptyEntriesState.visibility = View.GONE
        }

        val recent = entries.takeLast(5).reversed()
        val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

        for (entry in recent) {
            val itemView = TextView(requireContext())
            itemView.text =
                "${entry.emoji} Mood ${entry.moodValue}/5 | ${entry.note ?: ""} (${dateFormat.format(Date(entry.timestamp))})"
            itemView.textSize = 14f
            itemView.setPadding(8, 8, 8, 8)
            moodEntriesContainer.addView(itemView)
        }
    }

    private fun setupChartWithSampleData() {
        val entries = listOf(
            Entry(0f, 3f), Entry(1f, 4f), Entry(2f, 3.5f),
            Entry(3f, 4.5f), Entry(4f, 4f), Entry(5f, 5f), Entry(6f, 4.5f)
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
        val dataSet = LineDataSet(entries, "Mood Level").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            color = "#1976D2".toColorInt()
            setCircleColor("#1976D2".toColorInt())
            lineWidth = 3f
            circleRadius = 6f
            setDrawCircleHole(true)
            circleHoleColor = Color.WHITE
            valueTextSize = 12f
            valueTextColor = "#1976D2".toColorInt()
            setDrawFilled(true)
            fillColor = "#E3F2FD".toColorInt()
        }

        val lineData = LineData(dataSet)
        moodChart.data = lineData

        val xAxis = moodChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = "#666666".toColorInt()
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in labels.indices) labels[index] else ""
            }
        }

        moodChart.axisLeft.axisMinimum = 0f
        moodChart.axisLeft.axisMaximum = 6f
        moodChart.axisLeft.textColor = "#666666".toColorInt()
        moodChart.axisLeft.gridColor = "#E0E0E0".toColorInt()
        moodChart.axisRight.isEnabled = false
        moodChart.legend.isEnabled = false
        moodChart.description.isEnabled = false
        moodChart.animateX(1000)
        moodChart.invalidate()
    }
}
