package id.sch.smkn1pancurbatu.bell

import android.content.Context
import android.content.Intent
import android.os.Build
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class WebServerManager(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        if (Method.POST == method) {
            val files = HashMap<String, String>()
            try {
                session.parseBody(files)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val postData = files["postData"] ?: ""

            when (uri) {
                "/api/login" -> {
                    return try {
                        val json = JSONObject(postData)
                        val pin = json.optString("pin")
                        val validPin = ScheduleManager.getAdminPin(context)
                        if (pin == validPin) {
                            newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                        } else {
                            newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", "{\"status\":\"error\",\"message\":\"PIN Salah\"}")
                        }
                    } catch (e: Exception) {
                        newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"status\":\"error\"}")
                    }
                }
                "/api/save-schedule" -> {
                    val saved = ScheduleManager.saveScheduleJsonString(context, postData)
                    return if (saved) {
                        newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                    } else {
                        newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"status\":\"error\",\"message\":\"Format JSON tidak valid\"}")
                    }
                }
                "/api/announcement" -> {
                    return try {
                        val json = JSONObject(postData)
                        val text = json.optString("text", "")
                        ScheduleManager.setAnnouncement(context, text)
                        newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                    } catch (e: Exception) {
                        newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"status\":\"error\"}")
                    }
                }
                "/api/play" -> {
                    return try {
                        val json = JSONObject(postData)
                        val actionType = json.optString("action", "ACTION_PLAY")
                        val serviceIntent = Intent(context, AudioService::class.java).apply {
                            action = actionType
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                        newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}")
                    } catch (e: Exception) {
                        newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"status\":\"error\"}")
                    }
                }
                "/api/toggle-holiday" -> {
                    val prefs = context.getSharedPreferences("SMKN1_BELL_PREFS", Context.MODE_PRIVATE)
                    val current = prefs.getBoolean("HOLIDAY_MODE", false)
                    prefs.edit().putBoolean("HOLIDAY_MODE", !current).apply()
                    return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\",\"isHoliday\":${!current}}")
                }
            }
        }

        when (uri) {
            "/api/status" -> {
                val status = ScheduleManager.getCurrentStatus(context)
                val currentVersion = AppUpdater.getCurrentVersionName(context)
                val json = JSONObject().apply {
                    put("version", currentVersion)
                    put("isHoliday", status.isHoliday)
                    put("announcement", status.announcement)
                    put("remainingMinutes", status.remainingMinutes)
                    put("currentMapel", status.currentItem?.mapel ?: "Tidak Ada Pembelajaran")
                    put("currentGuru", status.currentItem?.guru ?: "-")
                    put("currentJamKe", status.currentItem?.jamKe ?: "-")
                    put("currentType", status.currentItem?.type ?: "SELESAI")
                    put("nextMapel", status.nextItem?.mapel ?: "Selesai")
                    put("nextTime", status.nextItem?.startTime ?: "-")
                    put("isBluetoothConnected", BluetoothHelper.isBluetoothSpeakerConnected(context))
                }
                return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
            }
            "/api/get-schedule" -> {
                val scheduleJson = ScheduleManager.getScheduleJsonString(context)
                return newFixedLengthResponse(Response.Status.OK, "application/json", scheduleJson)
            }
            "/", "/index.html" -> {
                return try {
                    val html = context.assets.open("web/index.html").bufferedReader().use { it.readText() }
                    newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", html)
                } catch (e: Exception) {
                    newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Web dashboard belum terpasang.")
                }
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found")
    }
}