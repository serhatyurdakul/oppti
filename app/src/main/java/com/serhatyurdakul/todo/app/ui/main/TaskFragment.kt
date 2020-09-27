package com.serhatyurdakul.todo.app.ui.main

import android.app.*
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.serhatyurdakul.todo.R
import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity
import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity
import com.serhatyurdakul.todo.app.ui.helper.Validator
import com.serhatyurdakul.todo.app.ui.main.adapter.TodoAdapter
import com.serhatyurdakul.todo.app.ui.main.callback.CategoryClickEvent
import com.serhatyurdakul.todo.app.ui.main.callback.TodoClickEvent
import com.serhatyurdakul.todo.databinding.PromptCategoryBinding
import com.serhatyurdakul.todo.databinding.PromptTodoBinding
import com.serhatyurdakul.todo.util.helper.*
import com.serhatyurdakul.todo.util.lib.firebase.callback.CategoryCallback
import com.serhatyurdakul.todo.util.lib.firebase.callback.CategoryListCallback
import com.serhatyurdakul.todo.util.lib.firebase.callback.TodoCallback
import com.serhatyurdakul.todo.util.lib.firebase.callback.TodoListCallback
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.android.synthetic.main.fragment_task.*
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TaskFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TaskFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var mContext: Context? = null
    val adapter = TodoAdapter()
    private var mainActivity: MainActivity? = null
    var dialogAddTodo: AlertDialog?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context;
        mainActivity = activity as MainActivity
    }

    override fun onAttach(activity: Activity) {
        mContext = activity;
        mainActivity = activity as MainActivity
        super.onAttach(activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity?.taskFragment = this

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




        rv_todo_list.layoutManager = LinearLayoutManager(mContext!!)
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


        addCategoryListListener()
        addTodoListListener()

    }

    fun onSignIn() {
        loadTodoList();
        addTodoListListener()
        addCategoryListListener()

    }

    // load TodoList of current user without listener
    private fun loadTodoList() {
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
        }

        swipe_refresh.isRefreshing = true
        mainActivity!!.remote.getTodoList(
            mainActivity!!.currentUserEntity!!,
            object : TodoListCallback {
                override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {
                    swipe_refresh.isRefreshing = false
                    if (error != null) {
                        Toaster(mContext!!).showToast(error)
                    } else {
                        if (todoList!!.size > 0) {
                            img_no_data.visibility = View.INVISIBLE
                            rv_todo_list.visibility = View.VISIBLE
                            adapter.setTodoList(todoList)
                            mainActivity!!.updateStatus(adapter.getToDoCount())
                        } else {
                            adapter.setTodoList(todoList)
                            mainActivity!!.updateStatus(adapter.getToDoCount())
                            if (adapter.getCategoryListArray().isEmpty()) {
                                img_no_data.visibility = View.VISIBLE
                                rv_todo_list.visibility = View.INVISIBLE
                            }
                        }
                    }
                }
            })
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
            mContext!!,
            R.layout.dropdown_menu_popup_item,
            adapter.getCategoryListArray()
        )

        binding.filledExposedDropdown.setAdapter(adapterCategory)
        binding.filledExposedDropdown.setOnClickListener {
            val imm =
                mContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)

        }




        binding.tilTodoCategory.setOnClickListener {
            val imm =
                mContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
                mContext!!, dateListener, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        var tempCalendar = Calendar.getInstance()
        tempCalendar.timeInMillis=todo.dateEpoch
        binding.tietTodoTime.text = SpannableStringBuilder(String.format("%02d:%02d", tempCalendar.get(Calendar.HOUR_OF_DAY),tempCalendar.get(Calendar.MINUTE)))


        val timeListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            myCalendar.set(Calendar.MINUTE, minute)
            myCalendar.set(Calendar.SECOND, 0)
            val timeSelected = "$hourOfDay:$minute"
            binding.tietTodoTime.text = SpannableStringBuilder(timeSelected)

        }
        binding.tietTodoTime.setOnClickListener {
            TimePickerDialog(mContext!!,timeListener,myCalendar.get(Calendar.HOUR_OF_DAY),myCalendar.get(Calendar.MINUTE),true).show()
        }

         dialogAddTodo = AlertDialog.Builder(mContext!!)
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
                todo.dateEpoch = myCalendar.timeInMillis
                mainActivity!!.remote.updateTodo(todo, object : TodoCallback {
                    override fun onResponse(todoCurrent: TodoEntity?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error == null) {
                            Toaster(mContext!!).showToast(getString(R.string.update_todo_success_message))
                            val intent = Intent(mainActivity!!,ReminderBroadcast::class.java)
                            intent.action = Gson().toJson(todo)
                            val pendingIntent = PendingIntent.getBroadcast(mainActivity!!,Random().nextInt(),intent,FLAG_CANCEL_CURRENT)
                            val alarmManager = mainActivity!!.getSystemService(ALARM_SERVICE) as AlarmManager
                            alarmManager.set(AlarmManager.RTC_WAKEUP,todo.dateEpoch,pendingIntent)
                        } else {
                            Toaster(mContext!!).showToast(error)
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()

        dialogAddTodo?.setOnShowListener {
            dialogAddTodo?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        }

        binding.filledExposedDropdown.setOnItemClickListener { _, _, position, _ ->
            if(adapter.getCategoryListArray()[position]=="Kategori Ekle ")
            {
                addCategory(true,false)
               // dialog.dismiss()
            }

        }




        dialogAddTodo?.show()

        Validator.forceValidation(arrayOf(binding.tietTodoTitle, binding.tietTodoDate, binding.tietTodoTime), dialogAddTodo!!)
    }


    // add new todoItem with custom alert dialog
    private fun addTodo(date: Date = Date()) {
        val dateNow = FormatUtil().formatDate(date, FormatUtil.dd_MMM_yyyy)
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
        }


        if (adapter.getCategoryListArray().isEmpty()) {
            Toaster(mContext!!).showToast("Önce kategori eklemeniz gerekiyor.")
            addCategory(true,true)
        } else {
            val binding = DataBindingUtil.inflate<PromptTodoBinding>(
                layoutInflater, R.layout.prompt_todo, null, false
            )


            val adapterCategory = ArrayAdapter<String>(
                mContext,
                R.layout.dropdown_menu_popup_item,
                adapter.getCategoryListArray()
            )

            binding.filledExposedDropdown.setAdapter(adapterCategory)
            binding.filledExposedDropdown.setOnClickListener {
                val imm =
                    mContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)

            }
            binding.tilTodoCategory.setOnClickListener {
                val imm =
                    mContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(it.windowToken, 0)

            }

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
                    mContext!!, dateListener, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            binding.tietTodoTime.text = SpannableStringBuilder("12:00")


            val timeListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                myCalendar.set(Calendar.MINUTE, minute)
                myCalendar.set(Calendar.SECOND, 0)
                val timeSelected = "$hourOfDay:$minute"
                binding.tietTodoTime.text = SpannableStringBuilder(timeSelected)

            }
            binding.tietTodoTime.setOnClickListener {
               TimePickerDialog(mContext!!,timeListener,myCalendar.get(Calendar.HOUR_OF_DAY),myCalendar.get(Calendar.MINUTE),true).show()
            }




             dialogAddTodo = AlertDialog.Builder(mContext!!)
                .setTitle(R.string.label_add_todo)
                .setView(binding.root)
                .setPositiveButton(R.string.label_add_todo) { _, _ ->
                    swipe_refresh.isRefreshing = true

                    val todoTitle = binding.tietTodoTitle.text.toString()
                    val date = binding.tietTodoDate.text.toString()
                    val dateEpoch = myCalendar.timeInMillis
                    val category = binding.filledExposedDropdown.text.toString()
                    val todo = TodoEntity(
                        "",
                        todoTitle,
                        false,
                        date,
                        dateEpoch,
                        mainActivity!!.currentUserEntity?.id!!,
                        "",
                        category
                    )

                    mainActivity!!.remote.addTodo(todo, object : TodoCallback {
                        override fun onResponse(todoCurrent: TodoEntity?, error: String?) {
                            swipe_refresh.isRefreshing = false
                            if (error == null) {
                                Toaster(mContext!!).showToast(getString(R.string.add_todo_success_message))

                                val intent = Intent(mainActivity!!,ReminderBroadcast::class.java)
                                intent.action = Gson().toJson(todo)
                                val pendingIntent = PendingIntent.getBroadcast(mainActivity,Random().nextInt(),intent,FLAG_CANCEL_CURRENT)
                                val alarmManager = mainActivity!!.getSystemService(ALARM_SERVICE) as AlarmManager
                                alarmManager.set(AlarmManager.RTC_WAKEUP,todo.dateEpoch,pendingIntent)

                            } else {
                                Toaster(mContext!!).showToast(error)
                            }
                        }
                    })
                }
                .setNegativeButton(R.string.label_cancel) { _, _ -> }
                .create()

            dialogAddTodo?.setOnShowListener {
                dialogAddTodo?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            }

            binding.filledExposedDropdown.setOnItemClickListener { _, _, position, _ ->
                if(adapter.getCategoryListArray()[position]=="Kategori Ekle ")
                {
                    addCategory(true,false)
                  //  dialog.dismiss()
                }

            }


            dialogAddTodo?.show()

            Validator.forceValidation(arrayOf(binding.tietTodoTitle, binding.tietTodoDate, binding.tietTodoTime), dialogAddTodo!!)
        }

    }


    private fun editCategory(category: CategoryEntity) {
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
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
                .show(fragmentManager!!)

        }
        val dialog = AlertDialog.Builder(mContext!!)
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
                mainActivity!!.remote.updateCategory(category, object : CategoryCallback {
                    override fun onResponse(category: CategoryEntity?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error == null) {
                            Toaster(mContext!!).showToast(getString(R.string.update_category_success_message))

                        } else {
                            Toaster(mContext!!).showToast(error)
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

    // bulk delete all completed todoItems
    fun clearCompletedTasks() {
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
        }

        if (adapter.getCompletedTodoList().size == 0) {
            Toaster(mContext!!).showToast(getString(R.string.no_completed_task_found_exception))
            return
        }

        AlertDialog.Builder(mContext!!)
            .setTitle(R.string.text_are_you_sure)
            .setMessage(R.string.clear_completed_tasks_warning)
            .setPositiveButton(R.string.label_clear_completed) { _, _ ->
                swipe_refresh.isRefreshing = true
                mainActivity!!.remote.deleteTodoList(
                    adapter.getCompletedTodoList(),
                    object : TodoListCallback {
                        override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {
                            swipe_refresh.isRefreshing = false
                            if (error != null) {
                                Toaster(mContext!!).showToast(error)
                            } else {
                                Toaster(mContext!!).showToast(
                                    mContext!!.getString(R.string.clear_completed_tasks_success_message)
                                )
                            }
                        }
                    })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
    }

    // add new categoryItem with custom alert dialog
    private fun addCategory(shouldAddTodo: Boolean = false,createAddTodoDialog: Boolean = false) {
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
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
                .show(fragmentManager!!)

        }

          val dialog = AlertDialog.Builder(mContext!!)
            .setTitle(R.string.label_add_category)
            .setView(binding.root)
            .setPositiveButton(R.string.label_add_category) { _, _ ->
                swipe_refresh.isRefreshing = true

                val categoryTitle = binding.tietCategoryTitle.text.toString()
                var colorInt = (binding.tilCategoryColor.background as ColorDrawable).color
                val hexColor = String.format("%06X", 0xFFFFFF and colorInt)
                val category = CategoryEntity(
                    "", categoryTitle, mainActivity!!.currentUserEntity?.id!!, hexColor
                )

                mainActivity!!.remote.addCategory(category, object : CategoryCallback {
                    override fun onResponse(category: CategoryEntity?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error == null) {
                            Toaster(mContext!!).showToast(getString(R.string.add_category_success_message))
                            if(shouldAddTodo) {
                                if(!adapter.listOfCategories.contains(category!!))
                                adapter.listOfCategories.add(category!!)
                                if(createAddTodoDialog)
                                addTodo()
                                else
                                {

                                    val adapterCategory = ArrayAdapter<String>(
                                        mContext,
                                        R.layout.dropdown_menu_popup_item,
                                        adapter.getCategoryListArray()
                                    )
                                    dialogAddTodo?.findViewById<AutoCompleteTextView>(R.id.filled_exposed_dropdown)?.setAdapter(adapterCategory)
                                    dialogAddTodo?.findViewById<AutoCompleteTextView>(R.id.filled_exposed_dropdown)?.setText(category.title,false)
                                }

                            }
                        } else {
                            Toaster(mContext!!).showToast(error)
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()

        dialog?.setOnShowListener {
            dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        }
        dialog?.show()

        Validator.forceValidation(
            arrayOf(binding.tietCategoryTitle, binding.tietCategoryColor),
            dialog!!
        )
    }

    //show todoItem's details
    private fun showDetails(todo: TodoEntity) {
        val details = "Title: ${todo.todo}\nDate: ${todo.date}"
        AlertDialog.Builder(mContext!!)
            .setTitle(R.string.label_details)
            .setMessage(details)
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
    }

    // bulk update all incomplete todoItems
    fun completeAllTasks() {
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
        }

        if (adapter.getIncompleteTodoList().size == 0) {
            Toaster(mContext!!).showToast(getString(R.string.no_incomplete_task_found_exception))
            return
        }

        AlertDialog.Builder(mContext!!)
            .setTitle(R.string.text_are_you_sure)
            .setMessage(R.string.complete_all_tasks_warning)
            .setPositiveButton(R.string.label_mark_all_as_complete) { _, _ ->
                swipe_refresh.isRefreshing = true
                val incompleteList = ArrayList<TodoEntity>()
                incompleteList.addAll(adapter.getIncompleteTodoList())
                incompleteList.forEach { it.completed = true }
                mainActivity!!.remote.markTodoListAsComplete(
                    incompleteList,
                    object : TodoListCallback {
                        override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {
                            swipe_refresh.isRefreshing = false
                            if (error != null) {
                                Toaster(mContext!!).showToast(error)
                            } else {
                                Toaster(mContext!!).showToast(
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


    // get todoItems of current user with firestore listener
    private fun addTodoListListener() {
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
        }

        mainActivity!!.remote.addTodoListListener(
            mainActivity!!.currentUserEntity!!,
            object : TodoListCallback {
                override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {
                    if (error != null) {
                        Toaster(mContext!!).showToast(error)
                    } else {
                        if (todoList!!.size > 0) {
                            img_no_data.visibility = View.INVISIBLE
                            rv_todo_list.visibility = View.VISIBLE
                            adapter.setTodoList(todoList)
                            mainActivity!!.updateStatus(adapter.getToDoCount())
                        } else {
                            adapter.setTodoList(todoList)
                            mainActivity!!.updateStatus(adapter.getToDoCount())
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
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
        }

        mainActivity!!.remote.addCategoryListListener(
            mainActivity!!.currentUserEntity!!,
            object : CategoryListCallback {
                override fun onResponse(categoryList: ArrayList<CategoryEntity>?, error: String?) {
                    if (error != null) {
                        Toaster(mContext!!).showToast(error)
                    } else {
                        if (categoryList!!.size > 0) {
                            img_no_data.visibility = View.INVISIBLE
                            rv_todo_list.visibility = View.VISIBLE
                            adapter.setCategoryList(categoryList)
                            mainActivity!!.updateStatus(adapter.getToDoCount())
                        } else {
                            adapter.setCategoryList(categoryList)
                            mainActivity!!.updateStatus(adapter.getToDoCount())

                        }
                    }
                }
            })
    }


    // mark a todoItem as complete/incomplete
    private fun toggleMarkAsComplete(todo: TodoEntity, position: Int) {
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
        }

        swipe_refresh.isRefreshing = true

        var successMessage = R.string.task_marked_as_completed_success_message
        if (todo.completed) successMessage = R.string.task_marked_as_incomplete_success_message
        todo.completed = !todo.completed
        mainActivity!!.remote.markTodoListAsComplete(arrayListOf(todo), object : TodoListCallback {
            override fun onResponse(todoList: ArrayList<TodoEntity>?, error: String?) {
                swipe_refresh.isRefreshing = false
                if (error == null) {
                    Toaster(mContext!!).showToast(getString(successMessage))
                } else {
                    Toaster(mContext!!).showToast(error)
                }
            }
        })
    }

    //delete a todoItem permanently
    private fun deleteTodo(todo: TodoEntity, position: Int) {
        AlertDialog.Builder(mContext!!)
            .setTitle(R.string.text_are_you_sure)
            .setMessage(R.string.todo_delete_warning)
            .setPositiveButton(R.string.label_delete) { _, _ ->
                swipe_refresh.isRefreshing = true
                mainActivity!!.remote.deleteTodo(todo, object : TodoCallback {
                    override fun onResponse(todo: TodoEntity?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error == null) {
                            Toaster(mContext!!).showToast(getString(R.string.delete_todo_success_message))
                            //adapter.getTodoList().remove(todo)
                            //adapter.notifyDataSetChanged()
                        } else {
                            Toaster(mContext!!).showToast(error)
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
        AlertDialog.Builder(mContext!!)
            .setTitle(R.string.text_are_you_sure)
            .setMessage(R.string.todo_delete_warning)
            .setPositiveButton(R.string.label_delete) { _, _ ->
                swipe_refresh.isRefreshing = true
                mainActivity!!.remote.deleteCategory(category, object : CategoryCallback {
                    override fun onResponse(category: CategoryEntity?, error: String?) {
                        swipe_refresh.isRefreshing = false
                        if (error == null) {
                            Toaster(mContext!!).showToast(getString(R.string.delete_category_success_message))
                            //adapter.getTodoList().remove(todo)
                            //adapter.notifyDataSetChanged()
                        } else {
                            Toaster(mContext!!).showToast(error)
                        }
                    }
                })
            }
            .setNegativeButton(R.string.label_cancel) { _, _ -> }
            .create()
            .show()
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TaskFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TaskFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}