package com.serhatyurdakul.todo.util.lib.firebase.callback

import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity

interface TodoCallback {
    fun onResponse(todo: TodoEntity?, error: String?)
}