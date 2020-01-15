package com.example.game0109

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val input = Intent(context, RestartService::class.java)
            context.startForegroundService(input)
        } else {
            val input = Intent(context, MyService::class.java)
            context.startService(input)
        }
    }
}