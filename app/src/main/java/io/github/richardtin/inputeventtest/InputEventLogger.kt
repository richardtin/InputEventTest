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
        log("pathId,x,y,touchMajor,touchMinor,touchSize,eventTime,downTime")
    }

    fun log(message: String) {
        fileOutputStream?.write("$message\n".toByteArray())
    }
}