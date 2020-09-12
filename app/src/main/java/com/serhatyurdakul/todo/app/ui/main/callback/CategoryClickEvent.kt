package com.serhatyurdakul.todo.app.ui.main.callback

import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity


interface CategoryClickEvent {

    companion object {
        // actions are used to make the callback method generic for all
        const val ACTION_EDIT = "edit"
        const val ACTION_DELETE = "delete"

    }

    fun onClickCategory(category: CategoryEntity, action: String, position: Int)
}