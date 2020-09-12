package com.serhatyurdakul.todo.util.lib.firebase.callback

import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity


interface CategoryListCallback {
    fun onResponse(categoryList: ArrayList<CategoryEntity>?, error: String?)
}