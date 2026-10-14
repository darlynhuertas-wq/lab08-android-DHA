package com.example.lab08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Abrimos la misma base de datos para contar las tareas pendientes
        val db = Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java,
            "task_db"
        ).build()
        val pending = db.taskDao().countPending()
        db.close()

        // Solo avisamos si hay algo pendiente
        if (pending > 0) {
            showNotification(pending)
        }
        return Result.success()
    }

    private fun showNotification(pending: Int) {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Desde Android 8 las notificaciones necesitan un canal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de tareas",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        // Al tocar la notificación se abre la app
        val openApp = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Tareas pendientes")
            .setContentText("Tienes $pending tarea(s) por completar")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        manager.notify(1, notification)
    }

    companion object {
        const val CHANNEL_ID = "task_reminders"
    }
}