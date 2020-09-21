package com.serhatyurdakul.todo.util.helper

import android.text.format.DateFormat

import java.text.SimpleDateFormat
import java.util.*


/*
* All type of formatter(i.e. date formatter) in one place
*/
class FormatUtil {
    companion object {
        const val dd_MMM_yyyy = "dd MMM yyyy"
        const val MMM = "MMM"
        const val EEEE = "EEEE"
    }

    fun formatDate(date: Date, format: String): String {
        return try {
            DateFormat.format(format, date).toString()
        } catch (ex: Exception) {
            date.toString()
        }
    }

    fun getTimeStampFromDate(dateString:String):Long
    {
        val formatter: SimpleDateFormat = SimpleDateFormat(dd_MMM_yyyy,Locale.ENGLISH)
        val date = formatter.parse(dateString) as Date
        return date.time
    }

    fun toMonth(date: Date): String {
        return try {
            DateFormat.format(MMM, date).toString()
        } catch (ex: Exception) {
            ""
        }
    }

    fun toDay(date: Date): String {
        return try {
            DateFormat.format(EEEE, date).toString()
        } catch (ex: Exception) {
            ""
        }
    }
}