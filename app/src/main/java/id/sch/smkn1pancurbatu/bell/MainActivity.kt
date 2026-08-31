package id.sch.smkn1pancurbatu.bell

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import id.sch.smkn1pancurbatu.bell.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var webServer: WebServerManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateDashboard()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startLocalWebServer()
        AlarmReceiver.scheduleNextAlarm(this)
        setupUI()
    }

    private fun startLocalWebServer() {
        try {
            webServer?.stop()
            webServer = WebServerManager(this, 8080).apply {
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onDestroy() {
        webServer?.stop()
        super.onDestroy()
    }

    private fun setupUI() {
        val prefs = getSharedPreferences("SMKN1_BELL_PREFS", Context.MODE_PRIVATE)
        binding.switchHoliday.isChecked = prefs.getBoolean("HOLIDAY_MODE", false)

        binding.switchHoliday.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("HOLIDAY_MODE", isChecked).apply()
            val msg = if (isChecked) "Mode Libur Aktif (Semua Bel & Lagu Dijeda)" else "Mode Sekolah Aktif"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            updateDashboard()
        }

        binding.btnPlayNow.setOnClickListener {
            val serviceIntent = Intent(this, AudioService::class.java).apply { action = "ACTION_PLAY" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Memutar lagu Indonesia Raya...", Toast.LENGTH_SHORT).show()
        }

        binding.btnStop.setOnClickListener {
            val serviceIntent = Intent(this, AudioService::class.java).apply { action = "ACTION_STOP" }
            startService(serviceIntent)
            Toast.makeText(this, "Audio dihentikan", Toast.LENGTH_SHORT).show()
        }

        binding.tvRunningText.isSelected = true
    }

    private fun updateDashboard() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))

        binding.tvClockTime.text = "${timeFormat.format(now)} WIB"
        binding.tvClockDate.text = dateFormat.format(now)

        val ipAddress = NetworkHelper.getLocalIpAddress(this)
        binding.tvIpAddress.text = "🌐 Web Remote Admin: http://$ipAddress:8080"

        val status = ScheduleManager.getCurrentStatus(this)
        binding.tvRunningText.text = "📢 PENGUMUMAN: ${status.announcement}"

        if (status.isHoliday) {
            binding.tvCurrentMapel.text = "🏖️ MODE LIBUR SEKOLAH"
            binding.tvCurrentGuru.text = "Semua jadwal dan bel otomatis dijeda."
            binding.tvRemainingTime.text = "Libur"
            binding.tvNextMapel.text = "Berikutnya: Tidak Ada"
        } else if (status.currentItem != null) {
            val item = status.currentItem
            if (item.type == "ISTIRAHAT") {
                binding.tvCurrentMapel.text = "☕ ${item.mapel.uppercase()}"
                binding.tvCurrentGuru.text = "Waktu Istirahat (${item.startTime} - ${item.endTime})"
                binding.tvCurrentMapel.setTextColor(Color.parseColor("#F59E0B"))
            } else {
                binding.tvCurrentMapel.text = "📖 ${item.mapel}"
                binding.tvCurrentGuru.text = "👨‍🏫 Guru: ${item.guru} • (${item.jamKe})"
                binding.tvCurrentMapel.setTextColor(Color.parseColor("#38BDF8"))
            }
            binding.tvRemainingTime.text = "⏳ Sisa: ${status.remainingMinutes} Menit"

            if (status.nextItem != null) {
                binding.tvNextMapel.text = "Berikutnya: ${status.nextItem.mapel} (${status.nextItem.startTime})"
            } else {
                binding.tvNextMapel.text = "Berikutnya: Bel Pulang Sekolah (15:30)"
            }
        } else {
            binding.tvCurrentMapel.text = "🏫 DI LUAR JAM PELAJARAN"
            binding.tvCurrentGuru.text = "Belajar Mengajar Dimulai Pukul 07:15 WIB"
            binding.tvRemainingTime.text = "-"
            binding.tvCurrentMapel.setTextColor(Color.parseColor("#94A3B8"))
            if (status.nextItem != null) {
                binding.tvNextMapel.text = "Mapel Pertama: ${status.nextItem.mapel} (${status.nextItem.startTime})"
            } else {
                binding.tvNextMapel.text = "Berikutnya: Selesai"
            }
        }

        val isBtConnected = BluetoothHelper.isBluetoothSpeakerConnected(this)
        if (isBtConnected) {
            binding.tvBluetoothStatus.text = "🟢 Speaker Bluetooth: Terhubung (Audio Siap)"
            binding.tvBluetoothStatus.setTextColor(Color.parseColor("#4ADE80"))
        } else {
            binding.tvBluetoothStatus.text = "🟠 Speaker Bluetooth: Tidak Terhubung (Speaker Internal IFP)"
            binding.tvBluetoothStatus.setTextColor(Color.parseColor("#FBBF24"))
        }
    }
}