package io.github.richardtin.inputeventtest

import java.io.File
import java.io.FileOutputStream

class InputEventLogger(path: String) {

    private val file = File(path)
    private var fileOutputStream: FileOutputStream? = null

    init {
        initializeLogFile()
    }

    private fun initializeLogFile() {
        if (!file.exists()) file.createNewFile()
        fileOutputStream = FileOutputStream(file, true)
        log("x,y,touchMajor,touchMinor,size,pressure,eventTime,downTime,toolType")
    }

    fun log(record: String) {
        fileOutputStream?.write("$record\n".toByteArray())
    }
}