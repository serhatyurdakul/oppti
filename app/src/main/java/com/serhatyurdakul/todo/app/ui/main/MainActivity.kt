package com.serhatyurdakul.todo.app.ui.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.appbar.AppBarLayout
import com.serhatyurdakul.todo.R
import com.serhatyurdakul.todo.app.data.local.Const
import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity
import com.serhatyurdakul.todo.app.data.local.user.UserEntity
import com.serhatyurdakul.todo.app.ui.helper.AuthHelper
import com.serhatyurdakul.todo.util.helper.FormatUtil
import com.serhatyurdakul.todo.util.helper.Toaster
import com.serhatyurdakul.todo.util.helper.load
import com.serhatyurdakul.todo.util.lib.firebase.FireStoreService
import com.serhatyurdakul.todo.util.lib.firebase.callback.CreateUserCallback
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_profile.view.*
import java.util.*


class MainActivity : AppCompatActivity(), AuthHelper {

    // firestore serveice class instance
    val remote: FireStoreService by lazy { FireStoreService() }

    var currentUserEntity: UserEntity? = null

    var isSignInPageVisible = false

    // menu for clearing all completed tasks at once
    private var completeAllMenu: MenuItem? = null
    private var clearCompletedMenu: MenuItem? = null
    var appBarLayout: AppBarLayout? = null
    var taskFragment: TaskFragment? = null
    var calendarFragment: CalendarFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appBarLayout = appbar


        initView()

        // check session
        if (currentUser() == null) {
            signIn(this)
        } else {
            currentUserEntity = remote.getUserEntity(currentUser()!!)
            updateStatus(0)
            //loadTodoList()
            ///  addTodoListListener()
            /// addCategoryListListener()
        }

        createNotificationChannel();

       var todoFromNotification = intent?.getSerializableExtra("todo") as TodoEntity?
        if (todoFromNotification != null) {
            showDetails(todoFromNotification)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        var todoFromNotification = intent?.getSerializableExtra("todo") as TodoEntity?
        if (todoFromNotification != null) {
            showDetails(todoFromNotification)
        }

    }

    private fun initView() {
        //init calendar view
        ///setUpCalendarView();
        // init recycler view
        container_profile.isClickable = true
        container_profile.setOnClickListener {
            if (currentUserEntity == null) signIn(this) else {
                AlertDialog.Builder(this)
                    .setTitle(R.string.text_are_you_sure)
                    .setMessage(R.string.switch_account_warning)
                    .setPositiveButton(R.string.label_switch_account) { _, _ -> switchAccount() }
                    .setNegativeButton(R.string.label_cancel) { _, _ -> signOutAccount() }
                    .create()
                    .show()
            }


        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Const.RequestCode.AUTH) {
            isSignInPageVisible = false
            // Firebase auth success
            if (resultCode == RESULT_OK) createUser() else {
                // Firebase auth failed
                val response = IdpResponse.fromResultIntent(data)
                when {
                    response == null ->
                        Toaster(this).showToast(getString(R.string.sign_in_required_exception))
                    response.error!!.errorCode == ErrorCodes.NO_NETWORK ->
                        Toaster(this).showToast(getString(R.string.no_internet_connection_exception))
                    else -> Toaster(this).showToast(response.error!!.message!!)
                }
            }
        }
    }


    // create new user based on the firebase auth data
    private fun createUser() {
        // swipe_refresh.isRefreshing = true
        remote.createUser(currentUser()!!, object : CreateUserCallback {
            override fun onResponse(user: UserEntity?, error: String?) {
                //swipe_refresh.isRefreshing = false
                if (error == null) {
                    currentUserEntity = user
                    Toaster(this@MainActivity).showToast("Hosgeldin ${currentUser()!!.displayName}")
                    updateStatus(0)
                    taskFragment?.onSignIn()
                    calendarFragment?.onSignIn()
                } else {
                    Toaster(this@MainActivity).showToast(error)
                }
            }
        })
    }


    // logout from current account and login to another/new account
    private fun signOutAccount() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    currentUserEntity = null

                    //show img_no_data
                    //adapter.clear()
                    //  rv_todo_list.visibility = View.INVISIBLE
                    // img_no_data.visibility = View.VISIBLE
                    updateStatus(0)
                    remote.removeTodoListListener()
                    remote.removeCategoryListListener()
                    remote.removeCategoryListListenerCalendar()
                    remote.removeTodoListListenerCalendar()
                } else {
                    Toaster(this).showToast(getString(R.string.unknown_exception))
                }
            }
    }


    // logout from current account and login to another/new account
    private fun switchAccount() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    currentUserEntity = null

                    //show img_no_data
                    //adapter.clear()
                    //  rv_todo_list.visibility = View.INVISIBLE
                    // img_no_data.visibility = View.VISIBLE
                    updateStatus(0)
                    remote.removeTodoListListener()
                    remote.removeCategoryListListener()
                    remote.removeCategoryListListenerCalendar()
                    remote.removeTodoListListenerCalendar()
                    signIn(this)
                } else {
                    Toaster(this).showToast(getString(R.string.unknown_exception))
                }
            }
    }

    // update the profile info and the empty data view
    fun updateStatus(count: Int) {
        completeAllMenu?.isVisible = currentUserEntity != null
        clearCompletedMenu?.isVisible = currentUserEntity != null

        if (currentUserEntity == null) {
            container_profile.visibility = View.VISIBLE
            container_profile.tv_name.text = "Devam etmek için giriş yapınız."
            container_profile.visibility = View.VISIBLE
            container_profile.tv_status.text = ""
        } else {
            container_profile.img_profile.load(currentUserEntity!!.image)
            container_profile.tv_name.text = currentUserEntity!!.name
            container_profile.visibility = View.VISIBLE


            var status = getString(R.string.label_no_todo_list_found)

            if (count > 0) {
                status = "${count} gorev bulundu"
            }

            container_profile.tv_status.text = status
        }
        val calender = Calendar.getInstance()
        val day = calender.get(Calendar.DAY_OF_MONTH)

        container_profile.tv_dd.text = day.toString()
        container_profile.tv_MMM.text = FormatUtil().toMonth(calender.time)
        container_profile.tv_day.text = FormatUtil().toDay(calender.time)
    }

    // remove listener of the todoList on activity destroy
    override fun onDestroy() {
        remote.removeTodoListListener()
        super.onDestroy()
    }

    fun createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "Reminder"
            val descriptionText = "Görev hatırlatıcı"
            val mChannel = NotificationChannel("Reminder", name, NotificationManager.IMPORTANCE_DEFAULT)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManagerCompat.from(this).createNotificationChannel(mChannel)

        }


    }

    //show todoItem's details
    private fun showDetails(todo: TodoEntity) {
        val details = "Title: ${todo.todo}\nDate: ${todo.date}"
        AlertDialog.Builder(this)
            .setTitle(R.string.label_details)
            .setMessage(details)
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
    }

}
