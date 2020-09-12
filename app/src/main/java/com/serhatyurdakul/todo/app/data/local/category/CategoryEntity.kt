package com.serhatyurdakul.todo.app.data.local.category

data class CategoryEntity(
    var id: String,
    var title: String,
    val user: String,
    var color: String
)