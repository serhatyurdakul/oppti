package com.serhatyurdakul.todo.app.ui.main

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.serhatyurdakul.todo.R
import com.serhatyurdakul.todo.app.data.local.Const
import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity
import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity
import com.serhatyurdakul.todo.app.data.local.user.UserEntity
import com.serhatyurdakul.todo.app.ui.helper.AuthHelper
import com.serhatyurdakul.todo.app.ui.helper.Validator
import com.serhatyurdakul.todo.app.ui.main.adapter.TodoAdapter
import com.serhatyurdakul.todo.app.ui.main.callback.CategoryClickEvent
import com.serhatyurdakul.todo.app.ui.main.callback.TodoClickEvent
import com.serhatyurdakul.todo.databinding.PromptCategoryBinding
import com.serhatyurdakul.todo.databinding.PromptTodoBinding
import com.serhatyurdakul.todo.util.helper.*
import com.serhatyurdakul.todo.util.lib.firebase.FireStoreService
import com.serhatyurdakul.todo.util.lib.firebase.callback.*
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_profile.view.*
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), AuthHelper {

    // firestore serveice class instance
    private val remote: FireStoreService by lazy { FireStoreService() }

    private var currentUserEntity: UserEntity? = null

    private val adapter = TodoAdapter()

    // menu for clearing all completed tasks at once
    private var completeAllMenu: MenuItem? = null
    private var clearCompletedMenu: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initView()

        // check session
        if (currentUser() == null) {
            signIn(this)
        } else {
            currentUserEntity = remote.getUserEntity(currentUser()!!)
            updateStatus()
            //loadTodoList()
            addTodoListListener()
            addCategoryListListener()
        }
    }

    private fun initView() {
        // init recycler view
        adapter.setListeners(object : TodoClickEvent {
            override fun onClickTodo(todo: TodoEntity, action: String, position: Int) {
                when (action) {
                    TodoClickEvent.ACTION_COMPLETE -> toggleMarkAsComplete(todo, position)
                    TodoClickEvent.ACTION_DETAILS -> showDetails(todo)
                    TodoClickEvent.ACTION_EDIT -> editTodo(todo, position)
                    TodoClickEvent.ACTION_DELETE -> deleteTodo(todo, position)
                }
            }
        }, object : CategoryClickEvent {
            override fun onClickCategory(category: CategoryEntity, action: String, position: Int) {
                when (action) {
                    CategoryClickEvent.ACTION_EDIT -> editCategory(category)
                    CategoryClickEvent.ACTION_DELETE -> deleteCategory(category, position)
                }
            }
        }

        )

        rv_todo_list.layoutManager = LinearLayoutManager(this)
        rv_todo_list.adapter = adapter
        rv_todo_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> if (!isAnimating) {
                        btn_add_todo.slideDown(150)
                        btn_add_category.slideDown(150)
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> if (!isAnimating) {
                        btn_add_todo.slideUp(150)
                        btn_add_category.slideUp(150)
                    }
                }
            }
        })

        //add button click and swipe refresh listener
        btn_add_todo.setOnClickListener { addTodo() }
        btn_add_category.setOnClickListener { addCategory() }
        swipe_refresh.setOnRefreshListener { loadTodoList() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Const.RequestCode.AUTH) {
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        completeAllMenu = menu?.getItem(1)
        clearCompletedMenu = menu?.getItem(2)
        completeAllMenu?.isVisible = currentUserEntity != null
        clearCompletedMenu?.isVisible = currentUserEntity != null
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_account -> {
                // switch account
                if (currentUserEntity == null) signIn(this) else {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.text_are_you_sure)
                        .setMessage(R.string.switch_account_warning)
                        .setPositiveButton(R.string.label_switch_account) { _, _ -> switchAccount() }
                        .setNegativeButton(R.string.label_cancel) { _, _ -> }
                        .create()
                        .show()
                }
            }
            R.id.action_complete_all_task -> {
                // mark all incomplete task as complete
                completeAllTasks()
            }
            R.id.action_show_categories -> {
                // mark all incomplete task as complete
                adapter.showCategories()
            }
            R.id.action_clear_completed_task -> {
                //clear complete tasks
                clearCompletedTasks()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    // create new user based on the firebase auth data
    private fun createUser() {
        swipe_refresh.isRefreshing = true
        remote.createUser(currentUser()!!, object : CreateUserCallback {
            override fun onResponse(user: UserEntity?, error: String?) {
                swipe_refresh.isRefreshing = false
                if (error == null) {
                    currentUserEntity = user
                    Toaster(this@MainActivity).showToast("Welcome ${currentUser()!!.displayName}")
                    updateStatus()
                    //loadTodoList()
                    addTodoListListener()
                    addCategoryListListener()
                } else {
                    Toaster(this@MainActivity).showToast(error)
                }
            }
        })
    }

    // load TodoList of current user without listener
    private fun loadTodoList() {
        if (currentUserEntity == null) {
            signIn(this); return
        }

        swipe_refresh.isRefreshing = true
        remote.getTodoList(currentUserEntity!!, object : TodoListCallback {
            override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {
                swipe_refresh.isRefreshing = false
                if (error != null) {
                    Toaster(this@MainActivity).showToast(error)
                } else {
                    if (todoList!!.size > 0) {
                        img_no_data.visibility = View.INVISIBLE
                        rv_todo_list.visibility = View.VISIBLE
                        adapter.setTodoList(todoList)
                        updateStatus()
                    } else {
                        adapter.setTodoList(todoList)
                        updateStatus()
                        if (adapter.getCategoryListArray().isEmpty()) {
                            img_no_data.visibility = View.VISIBLE
                            rv_todo_list.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        })
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

    // add new categoryItem with custom alert dialog
    private fun addCategory() {
        if (currentUserEntity == null) {
            signIn(this); return
        }

        val binding = DataBindingUtil.inflate<PromptCategoryBinding>(
            layoutInflater, R.layout.prompt_category, null, false
        )
        val colors = resources.getIntArray(R.array.colors)
        binding.tietCategoryColor.setOnClickListener {
            ColorSheet().colorPicker(
                colors = colors,
                listener = { color ->
                    binding.tilCategoryColor.setBackgroundColor(color)
                    binding.tietCategoryColor.setText(" ")
                })
                .show(supportFragmentManager)

        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.label_add_category)
            .setView(binding.root)
            .setPositiveButton(R.string.label_add_category) { _, _ ->
                swipe_refresh.isRefreshing = true

                val categoryTitle = binding.tietCategoryTitle.text.toString()
                var colorInt = (binding.tilCategoryColor.background as ColorDrawable).color
                val hexColor = String.format("%06X", 0xFFFFFF and colorInt)
                val category = CategoryEntity(
                    "", categoryTitle, currentUserEntity?.id!!, hexColor
                )

                remote.addCategory(category, object : CategoryCallback {
                    override fun onResponse(category: CategoryEntity?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error == null) {
                            Toaster(this@MainActivity).showToast(getString(R.string.add_category_success_message))
//                            img_no_data.visibility = View.INVISIBLE
//                            rv_todo_list.visibility = View.VISIBLE
//                            adapter.addTodo(todo!!)
                        } else {
                            Toaster(this@MainActivity).showToast(error)
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
        dialog.show()

        Validator.forceValidation(
            arrayOf(binding.tietCategoryTitle, binding.tietCategoryColor),
            dialog
        )
    }

    // add new todoItem with custom alert dialog
    private fun addTodo() {
        if (currentUserEntity == null) {
            signIn(this); return
        }


        if (adapter.getCategoryListArray().isEmpty()) {
            Toaster(this).showToast("Önce kategori eklemeniz gerekiyor.")
            addCategory()
        } else {
            val binding = DataBindingUtil.inflate<PromptTodoBinding>(
                layoutInflater, R.layout.prompt_todo, null, false
            )


            val adapterCategory = ArrayAdapter<String>(
                this,
                R.layout.dropdown_menu_popup_item,
                adapter.getCategoryListArray()
            )

            binding.filledExposedDropdown.setAdapter(adapterCategory)
            binding.filledExposedDropdown.setOnClickListener {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)

            }
            binding.tilTodoCategory.setOnClickListener {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)

            }
            val dateNow = FormatUtil().formatDate(Date(), FormatUtil.dd_MMM_yyyy)
            binding.tietTodoDate.text = SpannableStringBuilder(dateNow)

            val myCalendar = Calendar.getInstance()
            val dateListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                myCalendar.set(Calendar.YEAR, year)
                myCalendar.set(Calendar.MONTH, month)
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val dateSelected = FormatUtil().formatDate(myCalendar.time, FormatUtil.dd_MMM_yyyy)
                binding.tietTodoDate.text = SpannableStringBuilder(dateSelected)

            }
            binding.tietTodoDate.setOnClickListener {
                DatePickerDialog(
                    this, dateListener, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }


            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.label_add_todo)
                .setView(binding.root)
                .setPositiveButton(R.string.label_add_todo) { _, _ ->
                    swipe_refresh.isRefreshing = true

                    val todoTitle = binding.tietTodoTitle.text.toString()
                    val date = binding.tietTodoDate.text.toString()
                    val category = binding.filledExposedDropdown.text.toString()
                    val todo = TodoEntity(
                        "", todoTitle, false, date, currentUserEntity?.id!!, "", category
                    )

                    remote.addTodo(todo, object : TodoCallback {
                        override fun onResponse(todo: TodoEntity?, error: String?) {
                            swipe_refresh.isRefreshing = false
                            if (error == null) {
                                Toaster(this@MainActivity).showToast(getString(R.string.add_todo_success_message))
//                            img_no_data.visibility = View.INVISIBLE
//                            rv_todo_list.visibility = View.VISIBLE
//                            adapter.addTodo(todo!!)
                            } else {
                                Toaster(this@MainActivity).showToast(error)
                            }
                        }
                    })
                }
                .setNegativeButton(R.string.label_cancel) { _, _ -> }
                .create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            }
            dialog.show()

            Validator.forceValidation(arrayOf(binding.tietTodoTitle, binding.tietTodoDate), dialog)
        }

    }

    // edit an existing and incomplete todoItem
    private fun editTodo(todo: TodoEntity, position: Int) {

        val binding = DataBindingUtil.inflate<PromptTodoBinding>(
            layoutInflater, R.layout.prompt_todo, null, false
        )

        binding.tietTodoTitle.text = SpannableStringBuilder(todo.todo)
        binding.tietTodoDate.text = SpannableStringBuilder(todo.date)
        binding.tietTodoTitle.setSelection(todo.todo.length)
        val adapterCategory = ArrayAdapter<String>(
            this,
            R.layout.dropdown_menu_popup_item,
            adapter.getCategoryListArray()
        )

        binding.filledExposedDropdown.setAdapter(adapterCategory)
        binding.filledExposedDropdown.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)

        }
        binding.tilTodoCategory.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)

        }
        val myCalendar = Calendar.getInstance()
        val dateListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val dateSelected = FormatUtil().formatDate(myCalendar.time, FormatUtil.dd_MMM_yyyy)
            binding.tietTodoDate.text = SpannableStringBuilder(dateSelected)

        }
        binding.tietTodoDate.setOnClickListener {
            DatePickerDialog(
                this, dateListener, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.label_edit_todo)
            .setView(binding.root)
            .setPositiveButton(R.string.label_update_todo) { _, _ ->
                swipe_refresh.isRefreshing = true

                val todoTitle = binding.tietTodoTitle.text.toString()
                val date = binding.tietTodoDate.text.toString()
                val category = binding.filledExposedDropdown.text.toString()

                todo.todo = todoTitle
                todo.date = date
                todo.category = category
                remote.updateTodo(todo, object : TodoCallback {
                    override fun onResponse(todo: TodoEntity?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error == null) {
                            Toaster(this@MainActivity).showToast(getString(R.string.update_todo_success_message))
                            //adapter.notifyItemChanged(position)
                        } else {
                            Toaster(this@MainActivity).showToast(error)
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
        dialog.show()

        Validator.forceValidation(arrayOf(binding.tietTodoTitle, binding.tietTodoDate), dialog)
    }

    private fun editCategory(category: CategoryEntity) {
        if (currentUserEntity == null) {
            signIn(this); return
        }

        val binding = DataBindingUtil.inflate<PromptCategoryBinding>(
            layoutInflater, R.layout.prompt_category, null, false
        )
        val r = Integer.parseInt(category.color.substring(0, 2), 16)
        val g = Integer.parseInt(category.color.substring(2, 4), 16)
        val b = Integer.parseInt(category.color.substring(4, 6), 16)
        val backgroundColor = Color.rgb(r, g, b)


        binding.tietCategoryTitle.setText(category.title)
        binding.tilCategoryColor.setBackgroundColor(backgroundColor)
        binding.tietCategoryColor.setText(" ")
        val colors = resources.getIntArray(R.array.colors)
        binding.tietCategoryColor.setOnClickListener {
            ColorSheet().colorPicker(
                colors = colors,
                listener = { color ->
                    binding.tilCategoryColor.setBackgroundColor(color)
                    binding.tietCategoryColor.setText(" ")
                })
                .show(supportFragmentManager)

        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.label_edit_category)
            .setView(binding.root)
            .setPositiveButton(R.string.label_edit_category) { _, _ ->
                swipe_refresh.isRefreshing = true

                val categoryTitle = binding.tietCategoryTitle.text.toString()
                var colorInt = (binding.tilCategoryColor.background as ColorDrawable).color
                val hexColor =
                    String.format("%06X", 0xFFFFFF and colorInt)

                category.title = categoryTitle
                category.color = hexColor
                remote.updateCategory(category, object : CategoryCallback {
                    override fun onResponse(category: CategoryEntity?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error == null) {
                            Toaster(this@MainActivity).showToast(getString(R.string.update_category_success_message))
//                            img_no_data.visibility = View.INVISIBLE
//                            rv_todo_list.visibility = View.VISIBLE
//                            adapter.addTodo(todo!!)
                        } else {
                            Toaster(this@MainActivity).showToast(error)
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
        dialog.show()

        Validator.forceValidation(
            arrayOf(binding.tietCategoryTitle, binding.tietCategoryColor),
            dialog
        )

    }

    // mark a todoItem as complete/incomplete
    private fun toggleMarkAsComplete(todo: TodoEntity, position: Int) {
        if (currentUserEntity == null) {
            signIn(this); return
        }

        swipe_refresh.isRefreshing = true

        var successMessage = R.string.task_marked_as_completed_success_message
        if (todo.completed) successMessage = R.string.task_marked_as_incomplete_success_message
        todo.completed = !todo.completed
        remote.markTodoListAsComplete(arrayListOf(todo), object : TodoListCallback {
            override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {
                swipe_refresh.isRefreshing = false
                if (error == null) {
                    Toaster(this@MainActivity).showToast(getString(successMessage))
                } else {
                    Toaster(this@MainActivity).showToast(error)
                }
            }
        })
    }

    //delete a todoItem permanently
    private fun deleteTodo(todo: TodoEntity, position: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.text_are_you_sure)
            .setMessage(R.string.todo_delete_warning)
            .setPositiveButton(R.string.label_delete) { _, _ ->
                swipe_refresh.isRefreshing = true
                remote.deleteTodo(todo, object : TodoCallback {
                    override fun onResponse(todo: TodoEntity?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error == null) {
                            Toaster(this@MainActivity).showToast(getString(R.string.delete_todo_success_message))
                            //adapter.getTodoList().remove(todo)
                            //adapter.notifyDataSetChanged()
                        } else {
                            Toaster(this@MainActivity).showToast(error)
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
    }

    //delete a categoryItem permanently
    private fun deleteCategory(category: CategoryEntity, position: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.text_are_you_sure)
            .setMessage(R.string.todo_delete_warning)
            .setPositiveButton(R.string.label_delete) { _, _ ->
                swipe_refresh.isRefreshing = true
                remote.deleteCategory(category, object : CategoryCallback {
                    override fun onResponse(category: CategoryEntity?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error == null) {
                            Toaster(this@MainActivity).showToast(getString(R.string.delete_category_success_message))
                            //adapter.getTodoList().remove(todo)
                            //adapter.notifyDataSetChanged()
                        } else {
                            Toaster(this@MainActivity).showToast(error)
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
    }

    // bulk update all incomplete todoItems
    private fun completeAllTasks() {
        if (currentUserEntity == null) {
            signIn(this); return
        }

        if (adapter.getIncompleteTodoList().size == 0) {
            Toaster(this).showToast(getString(R.string.no_incomplete_task_found_exception))
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.text_are_you_sure)
            .setMessage(R.string.complete_all_tasks_warning)
            .setPositiveButton(R.string.label_mark_all_as_complete) { _, _ ->
                swipe_refresh.isRefreshing = true
                val incompleteList = ArrayList<TodoEntity>()
                incompleteList.addAll(adapter.getIncompleteTodoList())
                incompleteList.forEach { it.completed = true }
                remote.markTodoListAsComplete(incompleteList, object : TodoListCallback {
                    override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error != null) {
                            Toaster(this@MainActivity).showToast(error)
                        } else {
                            Toaster(this@MainActivity).showToast(
                                getString(R.string.update_incomplete_tasks_success_message)
                            )
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
    }

    // bulk delete all completed todoItems
    private fun clearCompletedTasks() {
        if (currentUserEntity == null) {
            signIn(this); return
        }

        if (adapter.getCompletedTodoList().size == 0) {
            Toaster(this).showToast(getString(R.string.no_completed_task_found_exception))
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.text_are_you_sure)
            .setMessage(R.string.clear_completed_tasks_warning)
            .setPositiveButton(R.string.label_clear_completed) { _, _ ->
                swipe_refresh.isRefreshing = true
                remote.deleteTodoList(adapter.getCompletedTodoList(), object : TodoListCallback {
                    override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error != null) {
                            Toaster(this@MainActivity).showToast(error)
                        } else {
                            Toaster(this@MainActivity).showToast(
                                getString(R.string.clear_completed_tasks_success_message)
                            )
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
    }

    // get todoItems of current user with firestore listener
    private fun addTodoListListener() {
        if (currentUserEntity == null) {
            signIn(this); return
        }

        remote.addTodoListListener(currentUserEntity!!, object : TodoListCallback {
            override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {
                if (error != null) {
                    Toaster(this@MainActivity).showToast(error)
                } else {
                    if (todoList!!.size > 0) {
                        img_no_data.visibility = View.INVISIBLE
                        rv_todo_list.visibility = View.VISIBLE
                        adapter.setTodoList(todoList)
                        updateStatus()
                    } else {
                        adapter.setTodoList(todoList)
                        updateStatus()
                        if (adapter.getCategoryListArray().isEmpty()) {
                            img_no_data.visibility = View.VISIBLE
                            rv_todo_list.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        })
    }

    // get todoItems of current user with firestore listener
    private fun addCategoryListListener() {
        if (currentUserEntity == null) {
            signIn(this); return
        }

        remote.addCategoryListListener(currentUserEntity!!, object : CategoryListCallback {
            override fun onResponse(categoryList: ArrayList<CategoryEntity>?, error: String?) {
                if (error != null) {
                    Toaster(this@MainActivity).showToast(error)
                } else {
                    if (categoryList!!.size > 0) {
                        img_no_data.visibility = View.INVISIBLE
                        rv_todo_list.visibility = View.VISIBLE
                        adapter.setCategoryList(categoryList)
                        updateStatus()
                    } else {
                        adapter.setCategoryList(categoryList)
                        updateStatus()
                        //  img_no_data.visibility = View.VISIBLE
                        //  rv_todo_list.visibility = View.INVISIBLE
                    }
                }
            }
        })
    }

    // logout from current account and login to another/new account
    private fun switchAccount() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    currentUserEntity = null
                    adapter.clear()
                    rv_todo_list.visibility = View.INVISIBLE
                    img_no_data.visibility = View.VISIBLE
                    updateStatus()
                    remote.removeTodoListListener()
                    signIn(this)
                } else {
                    Toaster(this).showToast(getString(R.string.unknown_exception))
                }
            }
    }

    // update the profile info and the empty data view
    private fun updateStatus() {
        completeAllMenu?.isVisible = currentUserEntity != null
        clearCompletedMenu?.isVisible = currentUserEntity != null

        if (currentUserEntity == null) {
            container_profile.visibility = View.GONE
        } else {
            container_profile.img_profile.load(currentUserEntity!!.image)
            container_profile.tv_name.text = currentUserEntity!!.name
            container_profile.visibility = View.VISIBLE
        }

        var status = getString(R.string.label_no_todo_list_found)
        var count = adapter.getToDoCount()
        if (count > 0) {
            status = "${count} to-do(s) found"
        }

        container_profile.tv_status.text = status

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
}
