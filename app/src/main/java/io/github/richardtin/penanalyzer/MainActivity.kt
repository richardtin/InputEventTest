package io.github.richardtin.penanalyzer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import com.afollestad.materialdialogs.MaterialDialog
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.callback.FolderPickerCallback
import com.anggrayudi.storage.callback.StorageAccessCallback
import com.anggrayudi.storage.file.StorageType
import com.anggrayudi.storage.file.absolutePath
import com.anggrayudi.storage.file.copyTo
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener
import io.github.richardtin.penanalyzer.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var mainBinding: ActivityMainBinding
    var logName: String? = null
    var logPath: String? = null
    lateinit var storage: SimpleStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setupSimpleStorage()
        setupFolderPickerCallback()
        setupButtonActions()
    }

    override fun onDestroy() {
        if (mainBinding.recorder.isChecked) {
            stopInputEventLogger()
        }
        deleteAllInFolder(filesDir)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        storage.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        storage.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        storage.onRestoreInstanceState(savedInstanceState)
    }

    private fun setupButtonActions() {
        mainBinding.run {
            recorderState.addOnButtonCheckedListener { _, _, isChecked ->
                penTypeOptionThin.isEnabled = !isChecked
                penTypeOptionThick.isEnabled = !isChecked
                save.isEnabled = !isChecked
                share.isEnabled = !isChecked

                if (isChecked) {
                    // Logger
                    startInputEventLogger()

                    // UI
                    appHint.visibility = View.GONE
                    recorder.icon =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_stop_circle)
                    logfileName.text = logName
                } else {
                    stopInputEventLogger()

                    // UI
                    drawingView.clear()
                    appHint.visibility = View.VISIBLE
                    recorder.icon =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_play_circle)
                    saveLog()
                }
            }
            save.setOnClickListener {
                saveLog()
            }
            share.setOnClickListener {
                sendLog()
            }
        }
    }

    private fun setupSimpleStorage() {
        storage = SimpleStorage(this)
        storage.storageAccessCallback = object : StorageAccessCallback {
            override fun onRootPathNotSelected(
                requestCode: Int,
                rootPath: String,
                rootStorageType: StorageType,
                uri: Uri
            ) {
                MaterialDialog(this@MainActivity)
                    .message(text = "Please select $rootPath")
                    .negativeButton(android.R.string.cancel)
                    .positiveButton {
                        storage.requestStorageAccess(REQUEST_CODE_STORAGE_ACCESS, rootStorageType)
                    }.show()
            }

            override fun onCancelledByUser(requestCode: Int) {
                Toast.makeText(baseContext, "Cancelled by user", Toast.LENGTH_SHORT).show()
            }

            override fun onStoragePermissionDenied(requestCode: Int) {
                requestStoragePermission()
            }

            override fun onRootPathPermissionGranted(requestCode: Int, root: DocumentFile) {
                Toast.makeText(
                    baseContext,
                    "Storage access has been granted for ${root.absolutePath}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun requestStoragePermission() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .withListener(object : BaseMultiplePermissionsListener() {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        storage.requestStorageAccess(REQUEST_CODE_STORAGE_ACCESS)
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Please grant storage permissions",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }).check()
    }

    private fun setupFolderPickerCallback() {
        storage.folderPickerCallback = object : FolderPickerCallback {
            override fun onStoragePermissionDenied(requestCode: Int) {
                requestStoragePermission()
            }

            override fun onStorageAccessDenied(
                requestCode: Int,
                folder: DocumentFile?,
                storageType: StorageType?
            ) {
                if (storageType == null) {
                    requestStoragePermission()
                    return
                }
                MaterialDialog(this@MainActivity)
                    .message(
                        text = "You have no write access to this storage, thus selecting this folder is useless." +
                                "\nWould you like to grant access to this folder?"
                    )
                    .negativeButton(android.R.string.cancel)
                    .positiveButton {
                        storage.requestStorageAccess(REQUEST_CODE_STORAGE_ACCESS, storageType)
                    }.show()
            }

            override fun onFolderSelected(requestCode: Int, folder: DocumentFile) {
                when (requestCode) {
                    REQUEST_CODE_PICK_FOLDER_TARGET_FOR_COPY -> copyLogToSelectedFolder(folder)
                    else -> Toast.makeText(baseContext, folder.absolutePath, Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onCancelledByUser(requestCode: Int) {
                Toast.makeText(baseContext, "Folder picker cancelled by user", Toast.LENGTH_SHORT)
                    .show()
            }
        }
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

    private fun saveLog() {
        storage.openFolderPicker(REQUEST_CODE_PICK_FOLDER_TARGET_FOR_COPY)
    }

    private fun copyLogToSelectedFolder(folder: DocumentFile) {
        logPath?.let {
            val sourceFile = File(it)
            DocumentFile.fromFile(sourceFile).copyTo(this@MainActivity, folder)
        }
    }

    companion object {
        const val REQUEST_CODE_STORAGE_ACCESS = 1
        const val REQUEST_CODE_PICK_FOLDER_TARGET_FOR_COPY = 2
    }

}