package com.serhatyurdakul.todo.app.ui.main.holder

import android.view.View
import android.widget.TextView
import com.kizitonwose.calendarview.ui.ViewContainer
import com.serhatyurdakul.todo.R

class DayViewContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.exFiveDayText)
    val taskOne = view.findViewById<TextView>(R.id.task_one)
    val taskTwo = view.findViewById<TextView>(R.id.task_two)
    val taskThree = view.findViewById<TextView>(R.id.task_three)

   // With ViewBinding
   //  val textView = CalendarDayLayoutBinding.bind(view).calendarDayText
}