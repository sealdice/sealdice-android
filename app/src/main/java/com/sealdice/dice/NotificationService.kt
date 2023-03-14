package com.sealdice.dice

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder


class NotificationService : Service(){
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ns: String = Context.NOTIFICATION_SERVICE
        val mNotificationManager = getSystemService(ns) as NotificationManager
        val notificationChannel = NotificationChannel("sealdice","SealDice", NotificationManager.IMPORTANCE_HIGH)
        mNotificationManager.createNotificationChannel(notificationChannel)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), 0)
        val notification: Notification = Notification.Builder(this,"sealdice")
            .setContentTitle("SealDice is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}