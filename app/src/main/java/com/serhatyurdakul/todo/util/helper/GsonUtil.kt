package com.serhatyurdakul.todo.util.helper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/*
* Gson implementation for serializing and de-serializing data
*/
class GsonUtil {
    fun toJson(obj: Any): String {
        return Gson().toJson(obj)
    }

    inline fun <reified T> fromJson(jsonStr: String?): T? {
        return Gson().fromJson<T>(
            jsonStr, object : TypeToken<T>() {}.type
        )
    }

    fun <T> fromJson(jsonStr: String?, anyClass: Class<T>): T {
        return Gson().fromJson(jsonStr, anyClass)
    }
}