package com.serhatyurdakul.todo.util.lib.firebase.callback

import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity

interface CategoryCallback {
    fun onResponse(category: CategoryEntity?, error: String?)
}