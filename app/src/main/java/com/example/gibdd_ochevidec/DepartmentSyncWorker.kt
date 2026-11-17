package com.example.gibdd_ochevidec

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.gibdd_ochevidec.network.EmployeeSessionStore
import com.example.gibdd_ochevidec.network.RetrofitClient
import java.util.concurrent.TimeUnit

/** Background fallback; Android may defer periodic work. The on-duty app uses WebSocket. */
class DepartmentSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val session = EmployeeSessionStore(applicationContext).load() ?: return Result.success()
        return try {
            val chats = RetrofitClient.apiService.chats(session.authorization).chats
            if (chats.any { it.unreadCount > 0 }) {
                val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(NotificationChannel("department_reports", "Новые сообщения", NotificationManager.IMPORTANCE_HIGH))
                if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    val intent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    manager.notify(4401, NotificationCompat.Builder(applicationContext, "department_reports")
                        .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Трезвый водитель 44")
                        .setContentText("Есть сообщения для уполномоченных сотрудников. Откройте приложение.")
                        .setContentIntent(intent).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
                }
            }
            Result.success()
        } catch (error: retrofit2.HttpException) {
            if (error.code() in setOf(401, 403)) Result.success() else Result.retry()
        } catch (_: Exception) { Result.retry() }
    }
    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DepartmentSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("department-sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
