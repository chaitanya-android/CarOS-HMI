package com.carosmini.applauncher

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.DeadObjectException
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.carosmini.core.PairedDevice
import com.carosmini.core.PhoneConnectionServiceConnection
import com.carosmini.core.Profile
import com.carosmini.core.ProfileServiceConnection

class ProfileSettingsActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvPermissionWarning: TextView
    private lateinit var spinnerDevices: Spinner
    private lateinit var spinnerProfiles: Spinner
    private lateinit var btnLink: Button

    private var pairedDevices = listOf<PairedDevice>()
    private var profiles = listOf<Profile>()

    private var isProfileServiceReady = false
    private var isPhoneServiceReady = false

    private val profileConn = ProfileServiceConnection { _ ->
        isProfileServiceReady = true
        runOnUiThread { checkAndLoad() }
    }

    private val phoneConn = PhoneConnectionServiceConnection { _ ->
        isPhoneServiceReady = true
        runOnUiThread { checkAndLoad() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        tvStatus = findViewById(R.id.tv_status)
        tvPermissionWarning = findViewById(R.id.tv_permission_warning)
        spinnerDevices = findViewById(R.id.spinner_devices)
        spinnerProfiles = findViewById(R.id.spinner_profiles)
        btnLink = findViewById(R.id.btn_link)

        profileConn.bind(this)
        phoneConn.bind(this)

        btnLink.setOnClickListener { linkSelection() }
    }

    override fun onResume() {
        super.onResume()
        if (!hasBluetoothPermission()) {
            tvPermissionWarning.visibility = View.VISIBLE
            tvPermissionWarning.text =
                "Bluetooth permission not granted.\nGrant it in Settings → Apps → CarOSMini → Permissions, then return here."
            btnLink.isEnabled = false
            return
        }
        tvPermissionWarning.visibility = View.GONE
        checkAndLoad()
    }

    override fun onDestroy() {
        super.onDestroy()
        profileConn.unbind(this)
        phoneConn.unbind(this)
    }

    // Called from both service callbacks and onResume. Only proceeds when both
    // services are ready and BT permission is granted.
    private fun checkAndLoad() {
        if (!isProfileServiceReady || !isPhoneServiceReady) return
        if (!hasBluetoothPermission()) return
        window.decorView.postDelayed({
            try {
                loadFromServices()
            } catch (e: DeadObjectException) {
                Log.e("SERVICE_DEBUG", "Binder died, rebinding", e)
                isProfileServiceReady = false
                isPhoneServiceReady = false
                profileConn.bind(this)
                phoneConn.bind(this)
            }
        }, 200)
    }

    private fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun loadFromServices() {
        profiles = profileConn.service?.profiles ?: emptyList()
        refreshProfileSpinner()

        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            showDeviceError("Bluetooth is off. Enable it and return here.")
            return
        }

        val bonded = adapter.bondedDevices ?: emptySet()
        val links = phoneConn.service?.pairedDevices?.associateBy { it.macAddress } ?: emptyMap()

        pairedDevices = bonded.map { bt ->
            links[bt.address] ?: PairedDevice(
                macAddress = bt.address,
                displayName = bt.name ?: bt.address,
            )
        }
        refreshDeviceSpinner()
    }

    private fun showDeviceError(message: String) {
        pairedDevices = emptyList()
        spinnerDevices.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, listOf(message)
        )
        btnLink.isEnabled = false
    }

    private fun refreshDeviceSpinner() {
        val labels = pairedDevices.map { d ->
            val linked = if (d.linkedProfileId != -1) {
                profiles.find { it.id == d.linkedProfileId }?.name ?: "unknown profile"
            } else "not linked"
            "${d.displayName} (${d.macAddress.takeLast(5)}) → $linked"
        }.ifEmpty { listOf("No paired Bluetooth devices found") }

        spinnerDevices.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        btnLink.isEnabled = pairedDevices.isNotEmpty() && profiles.isNotEmpty()
    }

    private fun refreshProfileSpinner() {
        val labels = profiles.map { it.name }.ifEmpty { listOf("No profiles") }
        spinnerProfiles.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        btnLink.isEnabled = pairedDevices.isNotEmpty() && profiles.isNotEmpty()
    }

    private fun linkSelection() {
        val deviceIdx = spinnerDevices.selectedItemPosition
        val profileIdx = spinnerProfiles.selectedItemPosition
        if (deviceIdx < 0 || deviceIdx >= pairedDevices.size) return
        if (profileIdx < 0 || profileIdx >= profiles.size) return

        val device = pairedDevices[deviceIdx]
        val profile = profiles[profileIdx]

        phoneConn.service?.linkDeviceToProfile(device.macAddress, device.displayName, profile.id)
        pairedDevices = pairedDevices.map {
            if (it.macAddress == device.macAddress) it.copy(linkedProfileId = profile.id) else it
        }
        refreshDeviceSpinner()

        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "Linked ${device.displayName} → ${profile.name}"
    }
}
