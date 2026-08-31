package id.sch.smkn1pancurbatu.bell

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object AppUpdater {
    // GANTI baris di bawah ini dengan UsernameGitHub dan NamaRepo Anda jika berbeda
    // Contoh: "UsernameAnda/bel-smkn1-pancurbatu"
    private const val GITHUB_REPO = "psraw/SmkN1PancurBatuBell"

    fun checkForUpdate(context: Context, callback: (UpdateInfo) -> Unit) {
        Thread {
            try {
                val currentVersion = getCurrentVersionName(context)
                val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val tagName = json.optString("tag_name", "").replace("v", "")
                    val releaseNotes = json.optString("body", "Pembaruan versi terbaru.")
                    
                    var apkUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null && assets.length() > 0) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                apkUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    val isNewer = isVersionNewer(tagName, currentVersion)
                    Handler(Looper.getMainLooper()).post {
                        callback(UpdateInfo(isNewer && apkUrl.isNotEmpty(), tagName, apkUrl, releaseNotes))
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        callback(UpdateInfo(false, "", "", "Tidak dapat memeriksa rilis."))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    callback(UpdateInfo(false, "", "", e.message ?: "Koneksi gagal"))
                }
            }
        }.start()
    }

    fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val url = URL(downloadUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                }
                
                val fileLength = conn.contentLength
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val destinationFile = File(downloadDir, "update_smkn1.apk")

                if (destinationFile.exists()) destinationFile.delete()

                val input = conn.inputStream
                val output = FileOutputStream(destinationFile)
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = ((total * 100) / fileLength).toInt()
                        Handler(Looper.getMainLooper()).post { onProgress(progress) }
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                Handler(Looper.getMainLooper()).post {
                    onComplete()
                    installApk(context, destinationFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    onError(e.message ?: "Gagal mendownload pembaruan.")
                }
            }
        }.start()
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isVersionNewer(remote: String, local: String): Boolean {
        if (remote.isEmpty() || local.isEmpty()) return false
        // Bandingkan tag versi (misal: "2.1.0" > "2.0.0")
        val cleanRemote = remote.split("-")[0].replace("[^0-9.]".toRegex(), "")
        val cleanLocal = local.split("-")[0].replace("[^0-9.]".toRegex(), "")
        return cleanRemote != cleanLocal
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "2.0.0"
        } catch (e: Exception) {
            "2.0.0"
        }
    }
}