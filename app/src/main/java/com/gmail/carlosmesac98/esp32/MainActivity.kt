package com.gmail.carlosmesac98.esp32

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.gmail.carlosmesac98.esp32.services.FileDataCollectorService
import com.gmail.carlosmesac98.esp32.services.BaseDataCollectorService
import com.stevenmhernandez.esp32csiserial.CSIDataInterface
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial
import com.stevenmhernandez.esp32csiserial.UsbService.TAG
import kotlin.math.log

class MainActivity : AppCompatActivity(), CSIDataInterface {

    private lateinit var tvCSICounter: TextView
    private lateinit var tvCSIString: TextView
    private lateinit var etPosition: EditText
    private lateinit var btChangePositon: Button
    private lateinit var tvCurrentPosition: TextView
    private var position = "Undefined"
    val dataCollectorService: BaseDataCollectorService = FileDataCollectorService()

    private val csiSerial: ESP32CSISerial = ESP32CSISerial()
    private var csiCounter = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initLayout()

        dataCollectorService.setup(this)
        dataCollectorService.handle("type,Mac,RSSI,CSI,Position\n")

        csiSerial.setup(this, "test")
        csiSerial.onCreate(this)

        btChangePositon.setOnClickListener {
            position = etPosition.text.toString()
            tvCurrentPosition.text = "La posicion actual es $position"
        }


    }

    private fun initLayout() {
        tvCSICounter = findViewById(R.id.tv_csiCounter)
        tvCSIString = findViewById(R.id.tv_csiString)
        etPosition = findViewById(R.id.et_position)
        btChangePositon = findViewById(R.id.bt_changePosition)
        tvCurrentPosition = findViewById(R.id.tv_currentPosition)

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
        Log.d(
            TAG,
            "updateCsiString: type: $type MAC: $MAC RSSI: $RSSI CSI: $CSI Position: $position"
        )
        return "$type,$MAC,$RSSI,$CSI,$position\n"
    }
}