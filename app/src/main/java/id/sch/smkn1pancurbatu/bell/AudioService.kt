package id.sch.smkn1pancurbatu.bell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class AudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val CHANNEL_ID = "SMKN1_PANCUR_BATU_BELL_CHANNEL"
    private val NOTIFICATION_ID = 1001

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // WAJIB: Panggil startForeground seketika di onCreate (detik ke-0) agar tidak kena timeout Android
        val initialNotification = buildNotification("Layanan Audio Bel & Lagu Aktif")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SMKN1Bell::AudioWakeLock")
        wakeLock?.acquire(5 * 60 * 1000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: "ACTION_PLAY"

        if (action == "ACTION_STOP") {
            stopAudio()
            return START_NOT_STICKY
        }

        when (action) {
            "ACTION_PLAY", "ACTION_INDONESIA_RAYA" -> {
                updateNotification("Memutar Lagu Kebangsaan Indonesia Raya...")
                playAudioResource(R.raw.indonesia_raya)
            }
            "ACTION_BELL_ISTIRAHAT" -> {
                updateNotification("Bel Masuk Waktu Istirahat...")
                playChimeSound(3)
            }
            "ACTION_BELL_MASUK" -> {
                updateNotification("Bel Istirahat Selesai (Masuk Kelas)...")
                playChimeSound(2)
            }
            "ACTION_BELL_PULANG" -> {
                updateNotification("Bel Pembelajaran Selesai (Pulang)...")
                playChimeSound(4)
            }
            else -> {
                playChimeSound(1)
            }
        }

        return START_NOT_STICKY
    }

    private fun boostVolume() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVolume * 0.95).toInt(), 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playAudioResource(resId: Int) {
        try {
            boostVolume()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, resId).apply {
                setOnCompletionListener { stopAudio() }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            playChimeSound(2)
        }
    }

    private fun playChimeSound(beeps: Int) {
        Thread {
            try {
                boostVolume()
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                for (i in 1..beeps) {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 600)
                    Thread.sleep(800)
                }
                toneGen.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopAudio()
            }
        }.start()
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bel Otomatis SMKN 1 Pancur Batu",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan Bel dan Lagu Kebangsaan Sekolah"
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(contentText))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        stopAudio()
        super.onDestroy()
    }
}