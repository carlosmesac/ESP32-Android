package com.gmail.carlosmesac98.esp32

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.stevenmhernandez.esp32csiserial.CSIDataInterface
import com.stevenmhernandez.esp32csiserial.ESP32CSISerial

class MainActivity : AppCompatActivity(), CSIDataInterface {

    private lateinit var tvCSICounter: TextView
    private val csiSerial: ESP32CSISerial = ESP32CSISerial()
    private var csiCounter = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initLayout()
        csiSerial.setup(this, "project")
        csiSerial.onCreate(this)

    }

    private fun initLayout() {
        tvCSICounter = findViewById(R.id.tv_csiCounter)
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

    }
}