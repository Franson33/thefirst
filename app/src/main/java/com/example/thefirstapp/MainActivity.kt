package com.example.thefirstapp

import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thefirstapp.adapter.TaskAdapter
import com.example.thefirstapp.model.TaskDatabase
import com.example.thefirstapp.repository.TaskRepository
import com.example.thefirstapp.viewmodel.TaskViewModel
import com.example.thefirstapp.viewmodel.TaskViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var taskAdapter: TaskAdapter
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(
            TaskRepository(TaskDatabase.getDatabase(applicationContext).taskDao())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        setupFab()
        observeTask()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onDeleteClick = { task -> viewModel.deleteTask(task) }
        )

        findViewById<RecyclerView>(R.id.recyclerView).apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.addTaskButton).setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun showAddTaskDialog() {
        val editText = EditText(this).apply {
            hint = "Enter task"
            setSingleLine()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Task")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val taskTitle = editText.text.toString().trim()
                if (taskTitle.isNotEmpty()) {
                    viewModel.insertTask(taskTitle)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeTask() {
        lifecycleScope.launch {
            viewModel.allTasks.collectLatest { tasks -> taskAdapter.submitList(tasks) }
        }
    }
}
