package id.sch.smkn1pancurbatu.bell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class AudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val CHANNEL_ID = "SMKN1_PANCUR_BATU_BELL_CHANNEL"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SMKN1Bell::AudioWakeLock")
        wakeLock?.acquire(5 * 60 * 1000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "ACTION_STOP") {
            stopAudio()
            return START_NOT_STICKY
        }
        startForeground(1001, buildNotification("Memutar Lagu Kebangsaan Indonesia Raya..."))
        playIndonesiaRaya()
        return START_NOT_STICKY
    }

    private fun playIndonesiaRaya() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVolume * 0.95).toInt(), 0)

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.indonesia_raya).apply {
                setOnCompletionListener { stopAudio() }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopAudio()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bel Otomatis SMKN 1 Pancur Batu",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Layanan Pemutaran Lagu Indonesia Raya Pukul 10:00 WIB"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMK NEGERI 1 PANCUR BATU")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopAudio()
        super.onDestroy()
    }
}
