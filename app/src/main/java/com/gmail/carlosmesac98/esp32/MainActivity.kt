package com.gmail.carlosmesac98.esp32

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.gmail.carlosmesac98.esp32.services.BaseDataCollectorService
import com.gmail.carlosmesac98.esp32.services.FileDataCollectorService
import com.stevenmhernandez.esp32csiserial.CSIDataInterface
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial
import com.stevenmhernandez.esp32csiserial.UsbService.TAG
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), CSIDataInterface {

    private lateinit var tvCSICounter: TextView
    private lateinit var etPosition: EditText
    private lateinit var btChangePosition: Button
    private lateinit var btClearPosition: Button
    private lateinit var btOpenFolder: Button

    private lateinit var tvCurrentPosition: TextView
    private var position = "Undefined"
    private lateinit var dataCollectorService: BaseDataCollectorService

    private val csiSerial: ESP32CSISerial = ESP32CSISerial()
    private var csiCounter = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initLayout()

        dataCollectorService = FileDataCollectorService("ESP32_casa", ".csv")
        dataCollectorService.setup(this)
        dataCollectorService.handle("type,Mac,RSSI,CSI_Amplitude,CSI_Phase,Position\n")

        csiSerial.setup(this, "test")
        csiSerial.onCreate(this)

        btChangePosition.setOnClickListener {
            position = etPosition.text.toString()
            tvCurrentPosition.text = "La posicion actual es $position"
        }


        btClearPosition.setOnClickListener {
            position = "Undefined"
            tvCurrentPosition.text = "La posicion actual es $position"
            etPosition.setText("")
        }

        btOpenFolder.setOnClickListener {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));

        }
    }

    private fun initLayout() {
        tvCSICounter = findViewById(R.id.tv_csiCounter)
        tvCSICounter.text = csiCounter.toString()
        tvCurrentPosition = findViewById(R.id.tv_currentPosition)
        tvCurrentPosition.text = "La posicion actual es $position"
        etPosition = findViewById(R.id.et_position)
        btChangePosition = findViewById(R.id.bt_changePosition)
        btClearPosition = findViewById(R.id.bt_clearPosition)
        btOpenFolder = findViewById(R.id.bt_openFolder)

    }

    override fun onResume() {
        super.onResume()
        csiSerial.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        csiSerial.onPause(this)
    }

    override fun addCsi(csi_string: String?) {
        csiCounter++
        tvCSICounter.text = csiCounter.toString()
        val arr = csi_string?.split(",")
        if (arr != null) {


            dataCollectorService.handle(
                updateCsiString(
                    arr[0],
                    arr[2],
                    arr[3],
                    arr.last(),
                    position
                )
            )
        }


    }

    private fun updateCsiString(
        type: String,
        MAC: String,
        RSSI: String,
        CSI: String,
        position: String

    ): String {
        val amplitudes = parseCSI(CSI, false)
        val phases = parseCSI(CSI, true)

        Log.d(
            TAG,
            "$type,$MAC,$RSSI,$amplitudes,$phases,$position\n"
        )
        return "$type,$MAC,$RSSI,$amplitudes,$phases,$position\n"
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


}