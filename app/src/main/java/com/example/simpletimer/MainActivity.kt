package com.example.simpletimer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.simpletimer.ui.theme.SimpleTimerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()

        setContent {
            SimpleTimerTheme {
                TimerApp()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                sendNotification()
            } else {
                showPermissionDeniedMessage()
            }
        }
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(this, "Notification permission is required to receive timer alerts.", Toast.LENGTH_LONG).show()
    }

    private fun sendNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            showPermissionDeniedMessage()
            return
        }

        try {
            val channelId = "timer_channel"
            val channelName = "Timer Notifications"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(notificationChannel)
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Timer Finished")
                .setContentText("Your timer is up!")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(this).notify(1, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
            showPermissionDeniedMessage()
        }
    }

    @Composable
    fun TimerApp() {
        var inputMinutes by remember { mutableStateOf("") }
        var timeLeft by remember { mutableStateOf(0L) }
        var isRunning by remember { mutableStateOf(false) }
        var countDownTimer: CountDownTimer? by remember { mutableStateOf(null) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = inputMinutes,
                    onValueChange = { inputMinutes = it },
                    label = { Text("Enter minutes") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (inputMinutes.isNotEmpty()) {
                                val minutes = inputMinutes.toLongOrNull() ?: 0
                                timeLeft = minutes * 60000
                                isRunning = true
                                countDownTimer?.cancel()
                                countDownTimer = object : CountDownTimer(timeLeft, 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        timeLeft = millisUntilFinished
                                    }

                                    override fun onFinish() {
                                        timeLeft = 0
                                        isRunning = false
                                        sendNotification()
                                    }
                                }.start()
                            }
                        },
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start")
                    }

                    Button(
                        onClick = {
                            countDownTimer?.cancel()
                            timeLeft = 0
                            isRunning = false
                        },
                        enabled = isRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = formatTime(timeLeft),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }

    @Composable
    fun formatTime(timeInMillis: Long): String {
        val seconds = (timeInMillis / 1000) % 60
        val minutes = (timeInMillis / 1000) / 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    @Preview(showBackground = true)
    @Composable
    fun TimerAppPreview() {
        SimpleTimerTheme {
            TimerApp()
        }
    }
}
