package com.serhatyurdakul.todo.app.ui.main

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.serhatyurdakul.todo.R
import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity
import java.util.*


class ReminderBroadcast : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent) {


        val todoString = intent.action
        if(todoString!=null)
        {
            var todo = Gson().fromJson(todoString, TodoEntity::class.java)
            if (todo != null) {
            val notificationIntent = Intent(context, MainActivity::class.java)
            notificationIntent.putExtra("todo", todo)
            val contentIntent = PendingIntent.getActivity(
                context,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )


            val builder = NotificationCompat.Builder(context, "Reminder")
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("Görev Bildirimi - "+ todo.category)
                .setContentText(todo.todo)
                .setWhen(todo.dateEpoch)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            NotificationManagerCompat.from(context).notify(Random().nextInt(), builder.build())
        }}
        else
            Toast.makeText(context,"Todo is null",LENGTH_LONG).show()
    }


}