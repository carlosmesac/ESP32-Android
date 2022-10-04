package com.gmail.carlosmesac98.esp32.services

import android.content.Context
import android.net.Uri
import java.io.IOException


/**
 * This abstract base class allows us to very easily create new methods to store our data.
 * For example, if you wish to simple send an HTTP PUT request to a server, create a
 * subclass of this abstract class and implement the following methods.
 */
abstract class BaseDataCollectorService {
    /**
     * Do any required setup here (i.e. initiate connection to a server or creating a file)
     *
     * @param context application context if required.
     */
    abstract fun setup(context: Context?)

    /**
     * Handle new data (i.e. HTTP PUT request or append to a local file)
     *
     * @param csi string data direct from the ESP32 serial monitor
     */
    abstract fun handle(csi: String?)

    /**
     * Get File URI
     *
     * @return
     * @throws IOException
     */
    @get:Throws(IOException::class)
    abstract val fileUri: Uri?
}