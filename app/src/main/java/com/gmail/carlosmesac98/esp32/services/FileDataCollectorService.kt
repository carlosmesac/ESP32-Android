package com.gmail.carlosmesac98.esp32.services

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class FileDataCollectorService(private val fileName: String, private val extension: String) :
    BaseDataCollectorService() {

    private val LOG_TAG = "FileDataCollectorService"
    private var outputFile: File? = null
    private var localBackup: FileOutputStream? = null
    override fun setup(context: Context?) {
        // Setup a local file just in case the phone never returns to the internet!
        // (or at least just in case the data never actually gets sent)
        try {
            outputFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName + System.currentTimeMillis() + extension
            )
            localBackup = FileOutputStream(outputFile, true)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Log.w(LOG_TAG, "FileOutputStream exception: - $e")
        }
    }

    override fun handle(csi: String?) {
        try {
            if (localBackup != null) {
                localBackup!!.write(csi!!.toByteArray())
                localBackup!!.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @get:Throws(IOException::class)
    override val fileUri: Uri
        get() = Uri.fromFile(outputFile)
}