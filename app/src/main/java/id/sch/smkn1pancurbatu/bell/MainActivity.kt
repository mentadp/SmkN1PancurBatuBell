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
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 1000) // Update jam setiap detik
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AlarmReceiver.scheduleNextAlarm(this)
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private fun setupUI() {
        val prefs = getSharedPreferences("SMKN1_BELL_PREFS", Context.MODE_PRIVATE)
        binding.switchHoliday.isChecked = prefs.getBoolean("HOLIDAY_MODE", false)

        binding.switchHoliday.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("HOLIDAY_MODE", isChecked).apply()
            val msg = if (isChecked) "Mode Libur Aktif (Lagu Tidak Diputar)" else "Mode Sekolah Aktif (Lagu Diputar 10:00 WIB)"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            updateStatus()
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
    }

    private fun updateStatus() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))

        binding.tvClockTime.text = "${timeFormat.format(now)} WIB"
        binding.tvClockDate.text = dateFormat.format(now)

        val isBtConnected = BluetoothHelper.isBluetoothSpeakerConnected(this)
        if (isBtConnected) {
            binding.tvBluetoothStatus.text = "🟢 Speaker Bluetooth: Terhubung (Output Siap)"
            binding.tvBluetoothStatus.setTextColor(Color.parseColor("#4ADE80"))
        } else {
            binding.tvBluetoothStatus.text = "🟠 Speaker Bluetooth: Tidak Terhubung (Speaker Internal IFP)"
            binding.tvBluetoothStatus.setTextColor(Color.parseColor("#FBBF24"))
        }
    }
}