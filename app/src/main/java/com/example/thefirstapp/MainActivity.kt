package com.example.thefirstapp

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thefirstapp.model.Task
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
            onDeleteClick = {  viewModel.deleteTask(it) },
            onEditClick = { showEditTaskDialog(it) }
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val task = taskAdapter.currentList[position]
                viewModel.deleteTask(task)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable(Color.RED)

                if (dX > 0) {
                    background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                } else {
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                }

                background.draw(c)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
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

    private fun showEditTaskDialog(task: Task) {
        val editText = EditText(this).apply {
            setText(task.title)
            setSingleLine()
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_task)
            .setView(editText)
            .setPositiveButton(R.string.save_edit) { _, _ ->
                val updatedTitle = editText.text.toString().trim()
               if (updatedTitle.isNotEmpty()) {
                   viewModel.updateTask(task.copy(title = updatedTitle))
               }
            }
            .setNegativeButton(R.string.cancel_edit, null)
            .show()
    }


    private fun observeTask() {
        lifecycleScope.launch {
            viewModel.allTasks.collectLatest { taskAdapter.submitList(it) }
        }
    }
}
