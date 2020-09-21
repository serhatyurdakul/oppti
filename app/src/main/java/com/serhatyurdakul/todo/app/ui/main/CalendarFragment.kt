package com.serhatyurdakul.todo.app.ui.main

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.MonthScrollListener
import com.serhatyurdakul.todo.R
import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity
import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity
import com.serhatyurdakul.todo.app.ui.helper.Validator
import com.serhatyurdakul.todo.app.ui.main.adapter.TodoAdapter
import com.serhatyurdakul.todo.app.ui.main.holder.DayViewContainer
import com.serhatyurdakul.todo.app.ui.main.holder.MonthViewContainer
import com.serhatyurdakul.todo.databinding.PromptTodoBinding
import com.serhatyurdakul.todo.util.helper.FormatUtil
import com.serhatyurdakul.todo.util.helper.Toaster
import com.serhatyurdakul.todo.util.helper.daysOfWeekFromLocale
import com.serhatyurdakul.todo.util.lib.firebase.callback.CategoryListCallback
import com.serhatyurdakul.todo.util.lib.firebase.callback.TodoCallback
import com.serhatyurdakul.todo.util.lib.firebase.callback.TodoListCallback
import kotlinx.android.synthetic.main.fragment_calendar.*
import kotlinx.android.synthetic.main.fragment_task.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*
import kotlin.collections.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CalendarFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CalendarFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var mContext: Context? = null
    private var mainActivity: MainActivity? = null
    var todoList: ArrayList<TodoEntity>? = null
    var categoryList: ArrayList<CategoryEntity>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        addCategoryListListenerCalendar()
        addTodoListListenerCalendar()
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CalendarFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CalendarFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity?.calendarFragment = this
        setUpCalendarView();
    }

    //TODO call on signin
    fun onSignIn() {

        addTodoListListenerCalendar()
        addCategoryListListenerCalendar()

    }

    fun setUpCalendarView() {

        calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            // Called only when a new container is needed.
            override fun create(view: View) = DayViewContainer(view)

            // Called every time we need to reuse a container.
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.textView.text = day.date.dayOfMonth.toString()
                container.view.isClickable = true
                val tasks = getTasksOfDay(day.date)
                container.view.setOnClickListener {
                    val myCalendar = Calendar.getInstance()
                    myCalendar.set(Calendar.YEAR, day.date.year)
                    myCalendar.set(Calendar.MONTH, day.date.monthValue-1)
                    myCalendar.set(Calendar.DAY_OF_MONTH, day.date.dayOfMonth)
                    val dateOfTask = FormatUtil().formatDate(myCalendar.time, FormatUtil.dd_MMM_yyyy)
                    var adapter =TodoAdapter()
                    categoryList?.let {
                        adapter.setCategoryList(categoryList!!)
                    }
                        adapter.setTodoList(tasks)

                    fragmentManager?.let { it1 -> TodoDialogFragment(adapter,dateOfTask,mainActivity!!).show(it1,"") }
                }
                var counter =0
                container.taskOne.visibility=View.INVISIBLE
                container.taskTwo.visibility=View.INVISIBLE
                container.taskThree.visibility=View.INVISIBLE
                for(todo in tasks)
                {
                    var currentCategory:CategoryEntity = getCategory(todo)
                    val r = Integer.parseInt(currentCategory.color.substring(0, 2), 16)
                    val g = Integer.parseInt(currentCategory.color.substring(2, 4), 16)
                    val b = Integer.parseInt(currentCategory.color.substring(4, 6), 16)
                    val backgroundColor = Color.rgb(r, g, b)
                    val textColor = Color.rgb(
                        255 - 1 * r / 5,
                        255 - 1 * g / 5,
                        255 - 1 * b / 5
                    )



                    when(counter)
                    {
                        0->
                        {
                            container.taskOne.visibility=View.VISIBLE
                            container.taskOne.text = currentCategory.title.substring(0,1).toUpperCase()
                            container.taskOne.setTextColor(textColor)
                            container.taskOne.setBackgroundColor(backgroundColor)
                        }
                        1->
                        {
                            container.taskTwo.visibility=View.VISIBLE
                            container.taskTwo.text = currentCategory.title.substring(0,1).toUpperCase()
                            container.taskTwo.setTextColor(textColor)
                            container.taskTwo.setBackgroundColor(backgroundColor)
                        }
                        2->
                        {
                            container.taskThree.visibility=View.VISIBLE
                            container.taskThree.text = currentCategory.title.substring(0,1).toUpperCase()
                            container.taskThree.setTextColor(textColor)
                            container.taskThree.setBackgroundColor(backgroundColor)
                        }
                    }
                    counter++
                }

            }
        }

        val currentMonth = YearMonth.now()
        val firstMonth = currentMonth.minusMonths(10)
        val lastMonth = currentMonth.plusMonths(60)
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

        calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                // Setup each header day text if we have not done that already.
                val daysOfWeek = daysOfWeekFromLocale()
                if (container.legendLayout.tag == null) {
                    container.legendLayout.tag = month.yearMonth
                    for (c in 0..6) {
                        var tv = container.legendLayout.getChildAt(c) as TextView;
                        tv.text = daysOfWeek[c].getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                            .toUpperCase(Locale.ENGLISH)
                        ///tv.setTextColorRes(R.color.example_5_text_grey)
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)

                    }

                    month.yearMonth
                }
            }
        }
        calendarView.monthScrollListener = {
            val cYear = Calendar.getInstance().get(Calendar.YEAR);
            val monthText = it.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .toUpperCase(Locale.ENGLISH)
            if (cYear == it.year)
                header_tv.text = monthText
            else
                header_tv.text = monthText + " - " + it.year

        }
        calendarView.setup(firstMonth, lastMonth, firstDayOfWeek)

        calendarView.scrollToMonth(currentMonth)


    }


    private fun getTasksOfDay(date: LocalDate): ArrayList<TodoEntity> {
        val myCalendar = Calendar.getInstance()
        myCalendar.set(Calendar.YEAR, date.year)
        myCalendar.set(Calendar.MONTH, date.monthValue-1)
        myCalendar.set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
        val dateOfTask = FormatUtil().formatDate(myCalendar.time, FormatUtil.dd_MMM_yyyy)

        val tasks = ArrayList<TodoEntity>()

        todoList?.let {

            for (todo in todoList!!) {
                if (todo.date == dateOfTask)
                    tasks.add(todo)
            }

        }




        return tasks
    }

    private fun getCategory(todo: TodoEntity): CategoryEntity
    {

        categoryList?.let {

            for(category in categoryList!!)
            {
                if(todo.category==category.title)
                    return category

            }

        }

        return CategoryEntity("-1","Diğer","","455A64")
    }


    // get todoItems of current user with firestore listener
    private fun addTodoListListenerCalendar() {
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
        }

        mainActivity!!.remote.addTodoListListenerCalendar(
            mainActivity!!.currentUserEntity!!,
            object :
                TodoListCallback {
                override fun onResponse(todoListC: ArrayList<TodoEntity>?, error: String?) {
                    if (error != null) {
                        Toaster(mContext!!).showToast(error)
                    } else {
                        todoList = todoListC
                        calendarView.notifyCalendarChanged()
                    }
                }
            })
    }

    // get todoItems of current user with firestore listener
    private fun addCategoryListListenerCalendar() {
        if (mainActivity!!.currentUserEntity == null) {
            mainActivity!!.signIn(mainActivity!!); return
        }

        mainActivity!!.remote.addCategoryListListenerCalendar(
            mainActivity!!.currentUserEntity!!,
            object :
                CategoryListCallback {
                override fun onResponse(categoryListC: ArrayList<CategoryEntity>?, error: String?) {
                    if (error != null) {
                        Toaster(mContext!!).showToast(error)
                    } else {
                        categoryList = categoryListC
                        calendarView.notifyCalendarChanged()
                    }
                }
            })
    }


}