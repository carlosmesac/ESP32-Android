package com.gmail.carlosmesac98.esp32

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.gmail.carlosmesac98.esp32.services.BaseDataCollectorService
import com.gmail.carlosmesac98.esp32.services.FileDataCollectorService
import com.gmail.carlosmesac98.esp32.ui.adapter.BeaconListAdapter
import com.gmail.carlosmesac98.esp32.utils.Constants
import com.google.android.material.button.MaterialButtonToggleGroup
import com.stevenmhernandez.esp32csiserial.CSIDataInterface
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial
import com.stevenmhernandez.esp32csiserial.UsbService.TAG
import org.altbeacon.beacon.*
import java.io.IOException
import java.net.InetAddress
import kotlin.math.atan2
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), CSIDataInterface {

    private var neverAskAgainPermissions = ArrayList<String>()
    private var firstScan = true

    private lateinit var tvCSICounter: TextView
    private lateinit var tvMAC: TextView
    private lateinit var tvBeaconCounter: TextView


    private lateinit var etPosition: EditText
    private lateinit var btChangePosition: Button
    private lateinit var btClearPosition: Button
    private lateinit var btOpenFolder: Button
    private lateinit var toggleButton: MaterialButtonToggleGroup

    private lateinit var tvCurrentPosition: TextView
    private lateinit var tvPing: TextView

    private var position = "Undefined"
    private var orientation = "North"
    private lateinit var dataCollectorService: BaseDataCollectorService

    private val csiSerial: ESP32CSISerial = ESP32CSISerial()
    private var csiCounter = 0
    private var first = true
    lateinit var beaconList: ListView
    private val apMAC = "58:EF:68:0E:16:EF"
    //private val apMAC = "A8:5E:45:F6:81:90"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initLayout()


        // Iniciamos el controlador para detectar balizas
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        )
        beaconManager.backgroundScanPeriod = 5000
        beaconManager.foregroundScanPeriod = 2000

        val region = Region("all-beacons-region", null, null, null)

        // Set up a Live Data observer for beacon data
        beaconManager.getRegionViewModel(region).rangedBeacons.observeForever(rangingObserver)

        // observer will be called each time a new list of beacons is ranged (typically ~1 second in the foreground)
        beaconManager.startRangingBeacons(region)



        dataCollectorService = FileDataCollectorService("ESP32_casa", ".csv")
        dataCollectorService.setup(this)
        dataCollectorService.handle("type,Mac,RSSI,TimeStamp,CSI_Amplitude,CSI_Phase,Position,Orientation\n")

        csiSerial.setup(this, "test")
        csiSerial.onCreate(this)


        btChangePosition.setOnClickListener {
            if (etPosition.text.isNotEmpty()) {
                position = etPosition.text.toString()
                tvCurrentPosition.text = "La posicion actual es $position"
            }
            }


        btClearPosition.setOnClickListener {
            position = "Undefined"
            tvCurrentPosition.text = "La posicion actual es $position"
            etPosition.setText("")
            toggleButton.check(R.id.bt_north)

        }

        btOpenFolder.setOnClickListener {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));

        }
        toggleButton.check(R.id.bt_north)
        toggleButton.addOnButtonCheckedListener { toggleButton, checkedId, _ ->
            orientation = when (checkedId) {
                R.id.bt_north -> "North"
                R.id.bt_east -> "East"
                R.id.bt_west -> "West"
                R.id.bt_south -> "South"

                else -> { // Note the block
                    toggleButton.check(R.id.bt_north)
                    "North"
                }
            }
        }
    }



    private fun initLayout() {
        tvCSICounter = findViewById(R.id.tv_csiCounter)
        tvMAC = findViewById(R.id.tv_MAC)
        tvBeaconCounter = findViewById(R.id.tv_beaconCounter)
        tvCSICounter.text = csiCounter.toString()
        tvCurrentPosition = findViewById(R.id.tv_currentPosition)
        tvCurrentPosition.text = "La posicion actual es $position"
        tvPing = findViewById(R.id.tv_pingReach)
        etPosition = findViewById(R.id.et_position)
        btChangePosition = findViewById(R.id.bt_changePosition)
        btClearPosition = findViewById(R.id.bt_clearPosition)
        btOpenFolder = findViewById(R.id.bt_openFolder)
        toggleButton = findViewById(R.id.toggleButton)
        beaconList = findViewById(R.id.beacon_list)

    }

    override fun onResume() {
        super.onResume()
        csiSerial.onResume(this)
        checkPermissions()

    }

    override fun onPause() {
        super.onPause()
        csiSerial.onPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    private val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        if (BeaconManager.getInstanceForApplication(this).rangedRegions.isNotEmpty()) {
            tvBeaconCounter.text = "Ranging enabled: ${beacons.count()} beacon(s) detected"
            Log.d(
                TAG,
                "rangingObserver: $beacons"
            )
        }

        beaconList.adapter = BeaconListAdapter(this, beacons.toTypedArray())
        for(beacon in beacons) {
            if (position != "Undefined") {
                dataCollectorService.handle(
                    updateCsiString(
                        "beacon",
                        beacon.bluetoothAddress,
                        beacon.rssi.toString(),
                        "",
                        position,
                        orientation
                    )
                )
            }
        }

        pingRouter("192.168.1.1")
    }

    private fun pingRouter(routerIp: String) {
        try {
            val process = Runtime.getRuntime().exec("ping -c 1 $routerIp")
            val inputStream = process.inputStream
            val input = inputStream.bufferedReader().use { it.readText() }
            Log.d("PING", input)
        } catch (e: IOException) {
            Log.e("PING", "Failed to ping router", e)
        }
    }

    override fun addCsi(csi_string: String?) {
        Log.d(TAG, "addCsi: $csi_string")
        tvCSICounter.text = csiCounter.toString()
        val arr = csi_string?.split(",")
        if (arr != null) {
            if (arr[2] == apMAC) {
                if (position != "Undefined") {
                    csiCounter++
                    val mac = arr[2]
                    tvMAC.text = mac
                    val type = arr[0]
                    val rssi = arr[3]
                    val csi = arr.last()
                    dataCollectorService.handle(
                        updateCsiString(
                            type,
                            mac,
                            rssi,
                            csi,
                            position,
                            orientation
                        )
                    )
                }
            }
        }
    }

    private fun updateCsiString(
        type: String,
        MAC: String,
        RSSI: String,
        CSI: String,
        position: String,
        orientation:String

    ): String {

        return if(CSI.isNotEmpty()) {
            Log.d(TAG, "updateCsiString: $CSI")
            val amplitudes = parseCSI(CSI, false)
            val amp = amplitudes.toString().replace("," ,"")
            val phases = parseCSI(CSI, true)
            val pha = phases.toString().replace("," ,"")

            Log.d(TAG, "updateCsiStringAmplitude: $amplitudes")
            "$type,$MAC,$RSSI,${System.currentTimeMillis()},$amp,$pha,$position,$orientation\n"

        }else {
            Log.d(
                TAG,
                "$type,$MAC,$RSSI,-,-,$position\n"
            )
            "$type,$MAC,$RSSI,${System.currentTimeMillis()},-,-,$position,$orientation\n"
        }
    }

    private fun parseCSI(CSI: String, returnPhases: Boolean): ArrayList<Float> {
        val csiInt = arrayListOf<Int>()
        val imaginary = arrayListOf<Float>()
        val real = arrayListOf<Float>()
        val amplitudes = arrayListOf<Float>()
        val phases = arrayListOf<Float>()

        val csiRaw =
            CSI.slice(1 until CSI.length - 2).split(" ")
        for (i in csiRaw) {
            csiInt.add(Integer.parseInt(i))
        }

        for (i in csiInt.indices) {
            if (i % 2 == 0) {
                imaginary.add(csiInt[i].toFloat())
            } else {
                real.add(csiInt[i].toFloat())
            }
        }

        for (i in 0 until csiInt.size / 2) {
            val amp = imaginary[i].pow(2) + real[i].pow(2)
            amplitudes.add(sqrt(amp))
            phases.add(atan2(imaginary[i], real[i]))
        }

        return if (returnPhases) {
            phases
        } else {
            amplitudes
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i in 1 until permissions.size) {
            Log.d(TAG, "onRequestPermissionResult for " + permissions[i] + ":" + grantResults[i])
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                //check if user select "never ask again" when denying any permission
                if (!shouldShowRequestPermissionRationale(permissions[i])) {
                    neverAskAgainPermissions.add(permissions[i])
                }
            }
        }
    }

    private fun checkPermissions() {
        // basepermissions are for M and higher
        var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        var permissionRationale =
            "This app needs fine location permission to detect beacons.  Please grant this now."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN
            )
            permissionRationale =
                "This app needs fine location permission, and bluetooth scan permission to detect beacons.  Please grant all of these now."
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                permissionRationale =
                    "This app needs fine location permission to detect beacons.  Please grant this now."
            } else {
                permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                permissionRationale =
                    "This app needs background location permission to detect beacons in the background.  Please grant this now."
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            permissionRationale =
                "This app needs both fine location permission and background location permission to detect beacons in the background.  Please grant both now."
        }
        var allGranted = true
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) allGranted =
                false
        }
        if (!allGranted) {
            if (neverAskAgainPermissions.isEmpty()) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("This app needs permissions to detect beacons")
                builder.setMessage(permissionRationale)
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener {
                    requestPermissions(
                        permissions, Constants.PERMISSION_REQUEST_FINE_LOCATION
                    )
                }
                builder.show()
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Functionality limited")
                builder.setMessage("Since location and device permissions have not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location and device discovery permissions to this app.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener { }
                builder.show()
            }
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("This app needs background location access")
                        builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.setOnDismissListener {
                            requestPermissions(
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                Constants.PERMISSION_REQUEST_BACKGROUND_LOCATION
                            )
                        }
                        builder.show()
                    } else {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Functionality limited")
                        builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.setOnDismissListener { }
                        builder.show()
                    }
                }
            } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S && (checkSelfPermission(
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("This app needs bluetooth scan permission")
                    builder.setMessage("Please grant scan permission so this app can detect beacons.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener {
                        requestPermissions(
                            arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                            Constants.PERMISSION_REQUEST_BLUETOOTH_SCAN
                        )
                    }
                    builder.show()
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Functionality limited")
                    builder.setMessage("Since bluetooth scan permission has not been granted, this app will not be able to discover beacons  Please go to Settings -> Applications -> Permissions and grant bluetooth scan permission to this app.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener { }
                    builder.show()
                }
            } else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("This app needs background location access")
                            builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                            builder.setPositiveButton(android.R.string.ok, null)
                            builder.setOnDismissListener {
                                requestPermissions(
                                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                    Constants.PERMISSION_REQUEST_BACKGROUND_LOCATION
                                )
                            }
                            builder.show()
                        } else {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Functionality limited")
                            builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                            builder.setPositiveButton(android.R.string.ok, null)
                            builder.setOnDismissListener { }
                            builder.show()
                        }
                    }
                }
            }
        }
    }
}