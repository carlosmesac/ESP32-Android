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

class MainActivity : AppCompatActivity(), CSIDataInterface {

    private lateinit var tvCSICounter: TextView
    private lateinit var etPosition: EditText
    private lateinit var btChangePosition: Button
    private lateinit var btClearPosition: Button

    private lateinit var tvCurrentPosition: TextView
    private var position = "Undefined"
    private val dataCollectorService: BaseDataCollectorService = FileDataCollectorService()

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

        btChangePosition.setOnClickListener {
            position = etPosition.text.toString()
            tvCurrentPosition.text = "La posicion actual es $position"
        }


        btClearPosition.setOnClickListener {
            position = "Undefined"
            tvCurrentPosition.text = "La posicion actual es $position"
            etPosition.setText("")
        }
    }

    private fun initLayout() {
        tvCSICounter = findViewById(R.id.tv_csiCounter)
        etPosition = findViewById(R.id.et_position)
        btChangePosition = findViewById(R.id.bt_changePosition)
        btClearPosition = findViewById(R.id.bt_clearPosition)
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