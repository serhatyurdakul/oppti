package com.serhatyurdakul.todo.app.ui.main.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.serhatyurdakul.todo.app.ui.main.CalendarFragment
import com.serhatyurdakul.todo.app.ui.main.TaskFragment

class MyFragmentPagerAdapter(manager:FragmentManager) : FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    var calendarFragment: CalendarFragment? = null
    var taskFragment: TaskFragment? = null

    override fun getItem(position: Int): Fragment {
         when (position) {
            0 -> calendarFragment=CalendarFragment()
            else -> taskFragment=TaskFragment()
        }
        return when (position) {
            0 -> calendarFragment!!
            else -> taskFragment!!
        }
    }

    override fun getCount(): Int {
       return 2
    }


}