package io.github.richardtin.inputeventtest

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import io.github.richardtin.inputeventtest.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var mainBinding: ActivityMainBinding
    var logName: String? = null
    var logPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mainBinding.run {
            recorderState.addOnButtonCheckedListener { _, _, isChecked ->
                penTypeOptionThin.isEnabled = !isChecked
                penTypeOptionThick.isEnabled = !isChecked
                save.isEnabled = !isChecked

                if (isChecked) {
                    // Logger
                    startInputEventLogger()

                    // UI
                    appHint.visibility = View.GONE
                    recorder.icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_stop_circle)
                    logfileName.text = logName
                } else {
                    stopInputEventLogger()

                    // UI
                    drawingView.clear()
                    appHint.visibility = View.VISIBLE
                    recorder.icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_play_circle)
                    sendLog()
                }
            }
            save.setOnClickListener {
                sendLog()
            }
        }
    }

    override fun onDestroy() {
        if (mainBinding.recorder.isChecked) {
            stopInputEventLogger()
        }
        deleteAllInFolder(filesDir)
        super.onDestroy()
    }

    private fun startInputEventLogger() {
        logName = generateLogName()
        logPath = "${getSavedDir()}/${logName}"
        mainBinding.drawingView.logger = InputEventLogger(logPath!!)
    }

    private fun stopInputEventLogger() {
        mainBinding.drawingView.logger?.close()
        mainBinding.drawingView.logger = null
    }

    private fun deleteAllInFolder(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { child ->
                deleteAllInFolder(child)
            }
        }
        fileOrDirectory.delete()
    }

    private fun getPenGroupCheckedItem(): String {
        return when (mainBinding.penTypeToggleGroup.checkedButtonId) {
            R.id.pen_type_option_thin -> "thin"
            R.id.pen_type_option_thick -> "thick"
            else -> "none"
        }
    }

    private fun getSavedDir(): String {
        return "$filesDir"
    }

    private fun getCurrentDate(): Date {
        return Date()
    }

    private fun getCurrentDateString(): String {
        val dateFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return dateFormatter.format(getCurrentDate())
    }

    private fun generateLogName(): String {
        return "${getCurrentDateString()}_${getPenGroupCheckedItem()}.csv"
    }

    private fun sendLog() {
        logPath?.let {
            val fileUri =
                FileProvider.getUriForFile(this, "$packageName.fileprovider", File(it))
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }
    }

}