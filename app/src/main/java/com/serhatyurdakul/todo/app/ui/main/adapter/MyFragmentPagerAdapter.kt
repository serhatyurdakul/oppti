package com.serhatyurdakul.todo.app.ui.main.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.serhatyurdakul.todo.app.ui.main.CalendarFragment
import com.serhatyurdakul.todo.app.ui.main.TaskFragment

class MyFragmentPagerAdapter(manager:FragmentManager) : FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> CalendarFragment()
            else -> TaskFragment()
        }
    }

    override fun getCount(): Int {
       return 2
    }


}