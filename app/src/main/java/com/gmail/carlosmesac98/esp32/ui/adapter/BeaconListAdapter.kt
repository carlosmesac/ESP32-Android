package com.gmail.carlosmesac98.esp32.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.gmail.carlosmesac98.esp32.R
import org.altbeacon.beacon.Beacon
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class BeaconListAdapter(private val context: Context, private val data: Array<Beacon>) :
    BaseAdapter() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): Any {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get view for row item
        val rowView = inflater.inflate(R.layout.list_beacon, parent, false)
// Get title element
        val titleTextView = rowView.findViewById(R.id.beacon_content) as TextView
        val beacon:Beacon = getItem(position) as Beacon
        val beaconDistance = String.format("%.3f", beacon.distance).toDouble()

        titleTextView.text = "MAC:${beacon.bluetoothAddress} - NAME:${beacon.bluetoothName} - RSSI:${beacon.rssi} - DIST:${beaconDistance}"
// Get thumbnail element
        val thumbnailImageView = rowView.findViewById(R.id.beacon_list_thumbnail) as ImageView
        return rowView

    }
}