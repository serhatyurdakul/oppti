package com.serhatyurdakul.todo.app.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.serhatyurdakul.todo.R
import java.util.*

class ReminderBroadcast : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")
        val builder =  NotificationCompat.Builder(context,"Reminder")
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("Görev Bildirimi")
            .setContentText(title!!)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(Random().nextInt(),builder.build())
    }


}