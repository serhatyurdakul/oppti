package com.serhatyurdakul.todo.util.lib.firebase.callback

import com.serhatyurdakul.todo.app.data.local.user.UserEntity

interface GetUsersCallback {
    fun onResponse(users: ArrayList<UserEntity>?, error: String?)
}