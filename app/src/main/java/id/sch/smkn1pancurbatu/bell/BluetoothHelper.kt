package id.sch.smkn1pancurbatu.bell

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

object BluetoothHelper {
    fun isBluetoothSpeakerConnected(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    return true
                }
            }
        } else {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            if (btAdapter != null && btAdapter.isEnabled) {
                val a2dpState = btAdapter.getProfileConnectionState(BluetoothProfile.A2DP)
                return a2dpState == BluetoothProfile.STATE_CONNECTED
            }
        }
        return false
    }
}
