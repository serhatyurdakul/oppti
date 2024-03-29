package com.serhatyurdakul.todo.app.ui.main.holder

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.serhatyurdakul.todo.R
import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity
import com.serhatyurdakul.todo.app.ui.main.callback.TodoClickEvent
import com.serhatyurdakul.todo.databinding.ItemTodoBinding
import java.util.*

class TodoHolder(private val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(todo: TodoEntity, callback: TodoClickEvent?) {
        var tempCalendar = Calendar.getInstance()
        tempCalendar.timeInMillis=todo.dateEpoch

        if(todo.dateEpoch>System.currentTimeMillis())
            binding.btnWarning.visibility= View.INVISIBLE
        else
            binding.btnWarning.visibility= View.VISIBLE
        binding.tvTodo.text = todo.todo
        binding.tvDate.text = todo.date +String.format(" %02d:%02d", tempCalendar.get(Calendar.HOUR_OF_DAY),tempCalendar.get(Calendar.MINUTE))
        // if the task is complete make the text gray, change the icon, and hide the edit button
        // else make it black, show different icon, and show the edit icon
        if (todo.completed) {
            binding.container.strokeColor = Color.GRAY
            binding.tvTodo.setTextColor(Color.GRAY)
            binding.btnTodoComplete.setImageResource(R.drawable.ic_done_all_grey)
            binding.btnEdit.visibility = View.GONE
        } else {
            binding.container.strokeColor =
                ContextCompat.getColor(binding.root.context, R.color.colorPrimary)
            binding.tvTodo.setTextColor(Color.BLACK)
            binding.btnTodoComplete.setImageResource(R.drawable.ic_check)
            binding.btnEdit.visibility = View.VISIBLE
        }

        // toggle the task completion(complete/incomplete) with this action
        binding.btnTodoComplete.setOnClickListener {
            callback?.onClickTodo(todo, TodoClickEvent.ACTION_COMPLETE, adapterPosition)
        }

        // show the task's details
        binding.tvTodo.setOnClickListener {
            callback?.onClickTodo(todo, TodoClickEvent.ACTION_DETAILS, adapterPosition)
        }

        // edit task
        binding.btnEdit.setOnClickListener {
            callback?.onClickTodo(todo, TodoClickEvent.ACTION_EDIT, adapterPosition)
        }

        // delete the task
        binding.btnDelete.setOnClickListener {
            callback?.onClickTodo(todo, TodoClickEvent.ACTION_DELETE, adapterPosition)
        }
    }
}