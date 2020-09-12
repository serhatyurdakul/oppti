package com.serhatyurdakul.todo.util.lib.firebase.callback

import com.serhatyurdakul.todo.app.data.local.user.UserEntity

interface CreateUserCallback {
    fun onResponse(user: UserEntity?, error: String?)
}