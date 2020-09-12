package com.serhatyurdakul.todo.app.ui.main.holder

import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity
import com.serhatyurdakul.todo.app.ui.main.callback.CategoryClickEvent
import com.serhatyurdakul.todo.databinding.ItemCategoryBinding


class CategoryHolder(private val binding: ItemCategoryBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(category: CategoryEntity, callback: CategoryClickEvent?) {
        binding.tvCategory.text = category.title

        // if the task is complete make the text gray, change the icon, and hide the edit button
        // else make it black, show different icon, and show the edit icon
        val r = Integer.parseInt(category.color.substring(0, 2), 16)
        val g = Integer.parseInt(category.color.substring(2, 4), 16)
        val b = Integer.parseInt(category.color.substring(4, 6), 16)
        val backgroundColor = Color.rgb(r, g, b)
        val textColor = Color.rgb(
            255 - 1 * r / 5,
            255 - 1 * g / 5,
            255 - 1 * b / 5
        )


        binding.container.setBackgroundColor(backgroundColor)
        binding.tvCategory.setTextColor(textColor)


        // toggle the task completion(complete/incomplete) with this action
        binding.container.setOnClickListener {
            callback?.onClickCategory(category, CategoryClickEvent.ACTION_EDIT, adapterPosition)
        }
        binding.btnDelete.setOnClickListener {
            callback?.onClickCategory(category, CategoryClickEvent.ACTION_DELETE, adapterPosition)
        }
    }
}