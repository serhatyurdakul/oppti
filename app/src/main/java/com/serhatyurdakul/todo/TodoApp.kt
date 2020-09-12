package com.serhatyurdakul.todo

import android.content.Context
import android.os.SystemClock
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import timber.log.Timber

class TodoApp : MultiDexApplication() {
    init {
        sInstance = this
    }

    companion object {
        private lateinit var sInstance: TodoApp

        fun getApplicationContext(): Context {
            return sInstance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()

        SystemClock.sleep(300)

        if (applicationContext != null) {
            if (BuildConfig.DEBUG) {
                initiateOnlyInDebugMode()
            }
            initiate(applicationContext)
        }
    }

    private fun initiateOnlyInDebugMode() {
        Timber.plant(object : Timber.DebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String? {
                return super.createStackElementTag(element) +
                        " - Method:${element.methodName} - Line:${element.lineNumber}"
            }
        })
    }

    private fun initiate(context: Context) {
        //Prefs.init(context)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}