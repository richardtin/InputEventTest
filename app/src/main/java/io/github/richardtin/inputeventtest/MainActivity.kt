package io.github.richardtin.inputeventtest

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import io.github.richardtin.inputeventtest.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var mainBinding: ActivityMainBinding
    var logPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mainBinding.loggerStart.setOnClickListener {
            mainBinding.run {
                logfileName.text = generateLogfileName()
                logPath = "${getSavedDir()}/${logfileName.text}"
                drawingView.logger = InputEventLogger(logPath!!)
                appHint.visibility = View.GONE
                penTypeOptionThin.isEnabled = false
                penTypeOptionThick.isEnabled = false
                save.isEnabled = false
            }
        }
        mainBinding.loggerStop.setOnClickListener {
            mainBinding.run {
                drawingView.clear()
                drawingView.logger = null
                appHint.visibility = View.VISIBLE
                penTypeOptionThin.isEnabled = true
                penTypeOptionThick.isEnabled = true
                save.isEnabled = true
            }
        }
        mainBinding.save.setOnClickListener {
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

    private fun generateLogfileName(): String {
        return "${getCurrentDateString()}_${getPenGroupCheckedItem()}.csv"
    }
}