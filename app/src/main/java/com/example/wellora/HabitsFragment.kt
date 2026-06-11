package com.example.wellora

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class HabitsFragment : Fragment() {

    private lateinit var habitsRecyclerView: RecyclerView
    private lateinit var addHabitButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var dateText: TextView
    private lateinit var emptyState: LinearLayout

    private val habits = mutableListOf<HabitEntity>()
    private lateinit var habitDao: HabitDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_habits, container, false)

        // Initialize views
        habitsRecyclerView = view.findViewById(R.id.habitsRecyclerView)
        addHabitButton = view.findViewById(R.id.addHabitButton)
        progressBar = view.findViewById(R.id.progressBar)
        progressText = view.findViewById(R.id.progressText)
        dateText = view.findViewById(R.id.dateText)
        emptyState = view.findViewById(R.id.emptyState)

        // Initialize Room DAO
        habitDao = AppDatabase.getInstance(requireContext()).habitDao()

        setCurrentDate()
        setupRecyclerView()

        addHabitButton.setOnClickListener { showAddHabitDialog() }

        loadHabits() // Load from database

        return view
    }

    private fun setCurrentDate() {
        val dateFormat = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault())
        dateText.text = dateFormat.format(java.util.Date())
    }

    private fun setupRecyclerView() {
        habitsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Load all habits from Room DB **/
    private fun loadHabits() {
        lifecycleScope.launch {
            habits.clear()
            habits.addAll(habitDao.getAllHabits())
            updateUI()
        }
    }

    /** Update RecyclerView and empty state **/
    private fun updateUI() {
        if (habits.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            habitsRecyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            habitsRecyclerView.visibility = View.VISIBLE
            habitsRecyclerView.adapter = HabitsAdapter(
                habits,
                onCheckedChange = { habit, isChecked ->
                    habit.isCompleted = isChecked
                    updateHabit(habit)
                    updateProgress()
                },
                onEdit = { habit -> showEditHabitDialog(habit) },
                onDelete = { habit -> confirmDeleteHabit(habit) }
            )
        }
        updateProgress()
    }

    /** Add habit **/
    private fun addHabitToDatabase(name: String, emoji: String) {
        val newHabit = HabitEntity(name = name, emoji = emoji)
        lifecycleScope.launch {
            habitDao.insertHabit(newHabit)
            loadHabits()
            updateWidget()
            Toast.makeText(requireContext(), "Habit added!", Toast.LENGTH_SHORT).show()
        }
    }

    /** Update habit in DB **/
    private fun updateHabit(habit: HabitEntity) {
        lifecycleScope.launch {
            habitDao.updateHabit(habit)
            updateWidget()
        }
    }

    /** Delete habit from DB **/
    private fun deleteHabit(habit: HabitEntity) {
        lifecycleScope.launch {
            habitDao.deleteHabit(habit)
            loadHabits()
            updateWidget()
            Toast.makeText(requireContext(), "Habit deleted", Toast.LENGTH_SHORT).show()
        }
    }

    /** Update progress bar and text **/
    @SuppressLint("SetTextI18n")
    private fun updateProgress() {
        val completed = habits.count { it.isCompleted }
        val total = habits.size
        val percentage = if (total == 0) 0 else (completed * 100) / total

        progressBar.progress = percentage
        progressText.text = getString(R.string.habit_progress, completed, total)

        // Optional percentage label (if exists in layout)
        val percentageText = view?.findViewById<TextView>(R.id.percentageText)
        percentageText?.text = "$percentage%"
    }

    /** Update widget whenever habits change **/
    private fun updateWidget() {
        HabitWidgetProvider.updateWidget(requireContext())
    }

    /** Dialog to add new habit **/
    @SuppressLint("InflateParams")
    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_habit, null)
        val habitNameInput = dialogView.findViewById<EditText>(R.id.habitNameInput)
        val emojiInput = dialogView.findViewById<EditText>(R.id.emojiInput)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Add New Habit")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val habitName = habitNameInput.text.toString().trim()
                val emoji = emojiInput.text.toString().trim().ifEmpty { "✓" }

                if (habitName.isNotEmpty()) addHabitToDatabase(habitName, emoji)
                else Toast.makeText(requireContext(), "Please enter a habit name", Toast.LENGTH_SHORT).show()

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /** Dialog to edit habit **/
    @SuppressLint("InflateParams")
    private fun showEditHabitDialog(habit: HabitEntity) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_habit, null)
        val habitNameInput = dialogView.findViewById<EditText>(R.id.habitNameInput)
        val emojiInput = dialogView.findViewById<EditText>(R.id.emojiInput)

        habitNameInput.setText(habit.name)
        emojiInput.setText(habit.emoji)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Edit Habit")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = habitNameInput.text.toString().trim()
                val newEmoji = emojiInput.text.toString().trim().ifEmpty { "✓" }

                if (newName.isNotEmpty()) {
                    habit.name = newName
                    habit.emoji = newEmoji
                    updateHabit(habit)
                    loadHabits()
                    Toast.makeText(requireContext(), "Habit updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Please enter a habit name", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /** Confirm delete **/
    private fun confirmDeleteHabit(habit: HabitEntity) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete '${habit.name}'?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteHabit(habit)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

/** RecyclerView Adapter **/
class HabitsAdapter(
    private val habits: List<HabitEntity>,
    private val onCheckedChange: (HabitEntity, Boolean) -> Unit,
    private val onEdit: (HabitEntity) -> Unit,
    private val onDelete: (HabitEntity) -> Unit
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emojiText: TextView = view.findViewById(R.id.habitEmoji)
        val nameText: TextView = view.findViewById(R.id.habitName)
        val checkBox: CheckBox = view.findViewById(R.id.habitCheckbox)
        val editButton: ImageButton = view.findViewById(R.id.btnEditHabit)
        val deleteButton: ImageButton = view.findViewById(R.id.btnDeleteHabit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.emojiText.text = habit.emoji
        holder.nameText.text = habit.name
        holder.checkBox.isChecked = habit.isCompleted

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(habit, isChecked)
        }
        holder.editButton.setOnClickListener { onEdit(habit) }
        holder.deleteButton.setOnClickListener { onDelete(habit) }
    }

    override fun getItemCount() = habits.size
}
