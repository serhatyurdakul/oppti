package com.serhatyurdakul.todo.util.lib.firebase.callback

import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity

interface TodoListCallback {
    fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?)
}