package com.serhatyurdakul.todo.app.data.local.todo

data class TodoEntity(
    var id: String,
    var todo: String,
    var completed: Boolean,
    var date: String,
    val user: String,
    var createdAt: String,
    var category: String
)