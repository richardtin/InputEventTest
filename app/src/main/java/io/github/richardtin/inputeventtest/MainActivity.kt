package io.github.richardtin.inputeventtest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import io.github.richardtin.inputeventtest.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var mainBinding: ActivityMainBinding
    var logPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mainBinding.start.setOnClickListener {
            logPath = "${getSavedDir()}/${getCurrentDateString()}_${getPenGroupCheckedItem()}.csv"
            mainBinding.drawingView.logger = InputEventLogger(logPath!!)
        }
        mainBinding.stop.setOnClickListener {
            mainBinding.drawingView.clear()
            mainBinding.drawingView.logger = null
        }
        mainBinding.save.setOnClickListener {
            // TODO: Save log file to File Manager
        }
    }

    private fun getPenGroupCheckedItem(): String {
        return when (mainBinding.penTypeGroup.checkedRadioButtonId) {
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
}