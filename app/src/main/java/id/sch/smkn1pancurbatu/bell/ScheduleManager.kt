package id.sch.smkn1pancurbatu.bell

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ScheduleItem(
    val id: String,
    val hari: String,
    val jamKe: String,
    val startTime: String,
    val endTime: String,
    val mapel: String,
    val guru: String,
    val type: String
)

data class CurrentStatus(
    val currentItem: ScheduleItem?,
    val nextItem: ScheduleItem?,
    val remainingMinutes: Int,
    val isHoliday: Boolean,
    val announcement: String
)

object ScheduleManager {
    private const val PREFS_NAME = "SMKN1_SCHEDULE_PREFS"
    private const val KEY_SCHEDULE = "SCHEDULE_DATA_JSON"
    private const val KEY_ANNOUNCEMENT = "ANNOUNCEMENT_TEXT"
    private const val KEY_ADMIN_PIN = "ADMIN_PIN"

    fun getAdminPin(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ADMIN_PIN, "123456") ?: "123456"
    }

    fun getAnnouncement(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ANNOUNCEMENT, "Selamat Datang di SMK Negeri 1 Pancur Batu • Belajar Cerdas, Berkarakter & Unggul!") ?: ""
    }

    fun setAnnouncement(context: Context, text: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ANNOUNCEMENT, text).apply()
    }

    fun getScheduleJsonString(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var jsonStr = prefs.getString(KEY_SCHEDULE, null)
        if (jsonStr == null) {
            jsonStr = getDefaultScheduleJson()
            prefs.edit().putString(KEY_SCHEDULE, jsonStr).apply()
        }
        return jsonStr
    }

    fun saveScheduleJsonString(context: Context, jsonString: String): Boolean {
        return try {
            JSONArray(jsonString)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SCHEDULE, jsonString).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentStatus(context: Context): CurrentStatus {
        val prefs = context.getSharedPreferences("SMKN1_BELL_PREFS", Context.MODE_PRIVATE)
        val isHoliday = prefs.getBoolean("HOLIDAY_MODE", false)
        val announcement = getAnnouncement(context)

        if (isHoliday) {
            return CurrentStatus(null, null, 0, true, announcement)
        }

        val calendar = Calendar.getInstance()
        val dayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Minggu"
        }

        val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentMinutes = timeToMinutes(currentTimeStr)

        val allItems = parseSchedule(getScheduleJsonString(context)).filter { 
            it.hari.equals(dayName, ignoreCase = true) || it.hari.equals("Setiap Hari", ignoreCase = true)
        }.sortedBy { timeToMinutes(it.startTime) }

        var currentItem: ScheduleItem? = null
        var nextItem: ScheduleItem? = null
        var remainingMinutes = 0

        for (i in allItems.indices) {
            val item = allItems[i]
            val start = timeToMinutes(item.startTime)
            val end = timeToMinutes(item.endTime)

            if (currentMinutes in start until end) {
                currentItem = item
                remainingMinutes = end - currentMinutes
                if (i + 1 < allItems.size) {
                    nextItem = allItems[i + 1]
                }
                break
            } else if (currentMinutes < start && nextItem == null) {
                nextItem = item
            }
        }

        return CurrentStatus(currentItem, nextItem, remainingMinutes, false, announcement)
    }

    private fun parseSchedule(jsonStr: String): List<ScheduleItem> {
        val list = mutableListOf<ScheduleItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ScheduleItem(
                        id = obj.optString("id", i.toString()),
                        hari = obj.optString("hari", "Senin"),
                        jamKe = obj.optString("jamKe", "1"),
                        startTime = obj.optString("startTime", "07:30"),
                        endTime = obj.optString("endTime", "08:15"),
                        mapel = obj.optString("mapel", "-"),
                        guru = obj.optString("guru", "-"),
                        type = obj.optString("type", "BELAJAR")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun timeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            parts[0].trim().toInt() * 60 + parts[1].trim().toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun getDefaultScheduleJson(): String {
        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat")
        val array = JSONArray()

        for (day in days) {
            array.put(JSONObject().apply {
                put("id", "${day}_0")
                put("hari", day)
                put("jamKe", "Pembiasaan")
                put("startTime", "07:15")
                put("endTime", "07:30")
                put("mapel", if (day == "Senin") "Upacara Bendera" else "Literasi & Doa Pagi")
                put("guru", "Wali Kelas")
                put("type", "BELAJAR")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_1")
                put("hari", day)
                put("jamKe", "Jam 1")
                put("startTime", "07:30")
                put("endTime", "08:15")
                put("mapel", "Matematika / Produktif")
                put("guru", "Guru Pengampu")
                put("type", "BELAJAR")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_2")
                put("hari", day)
                put("jamKe", "Jam 2")
                put("startTime", "08:15")
                put("endTime", "09:00")
                put("mapel", "Bahasa Indonesia")
                put("guru", "Guru Pengampu")
                put("type", "BELAJAR")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_3")
                put("hari", day)
                put("jamKe", "Jam 3")
                put("startTime", "09:00")
                put("endTime", "09:45")
                put("mapel", "Bahasa Inggris")
                put("guru", "Guru Pengampu")
                put("type", "BELAJAR")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_4")
                put("hari", day)
                put("jamKe", "Jam 4 (10:00 ID Raya)")
                put("startTime", "09:45")
                put("endTime", "10:30")
                put("mapel", "Pendidikan Kejuruan")
                put("guru", "Guru Pengampu")
                put("type", "BELAJAR")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_ist1")
                put("hari", day)
                put("jamKe", "Istirahat 1")
                put("startTime", "10:30")
                put("endTime", "11:00")
                put("mapel", "Istirahat Pagi")
                put("guru", "-")
                put("type", "ISTIRAHAT")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_5")
                put("hari", day)
                put("jamKe", "Jam 5")
                put("startTime", "11:00")
                put("endTime", "11:45")
                put("mapel", "Konsentrasi Keahlian")
                put("guru", "Guru Pengampu")
                put("type", "BELAJAR")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_6")
                put("hari", day)
                put("jamKe", "Jam 6")
                put("startTime", "11:45")
                put("endTime", "12:30")
                put("mapel", "Konsentrasi Keahlian")
                put("guru", "Guru Pengampu")
                put("type", "BELAJAR")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_ist2")
                put("hari", day)
                put("jamKe", "Istirahat 2")
                put("startTime", "12:30")
                put("endTime", "13:15")
                put("mapel", "Ishoma (Sholat & Makan)")
                put("guru", "-")
                put("type", "ISTIRAHAT")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_7")
                put("hari", day)
                put("jamKe", "Jam 7")
                put("startTime", "13:15")
                put("endTime", "14:00")
                put("mapel", "Informatika / IPAS")
                put("guru", "Guru Pengampu")
                put("type", "BELAJAR")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_8")
                put("hari", day)
                put("jamKe", "Jam 8")
                put("startTime", "14:00")
                put("endTime", "14:45")
                put("mapel", "Pendidikan Agama")
                put("guru", "Guru Pengampu")
                put("type", "BELAJAR")
            })
            array.put(JSONObject().apply {
                put("id", "${day}_9")
                put("hari", day)
                put("jamKe", "Jam 9")
                put("startTime", "14:45")
                put("endTime", "15:30")
                put("mapel", "Bimbingan & Refleksi")
                put("guru", "Guru Pengampu")
                put("type", "BELAJAR")
            })
        }
        return array.toString()
    }
}