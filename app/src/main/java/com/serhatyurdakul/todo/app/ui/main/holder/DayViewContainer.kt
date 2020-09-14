package com.serhatyurdakul.todo.app.ui.main.holder

import android.view.View
import android.widget.TextView
import com.kizitonwose.calendarview.ui.ViewContainer
import com.serhatyurdakul.todo.R

class DayViewContainer(view: View) : ViewContainer(view) {
    val textView = view.findViewById<TextView>(R.id.exFiveDayText)
    
   // With ViewBinding
   //  val textView = CalendarDayLayoutBinding.bind(view).calendarDayText
}