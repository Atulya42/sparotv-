package com.example.sparotv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {
    private var server: SparoServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Android 12 All Files Access
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } else {
            startServer()
        }
    }

    private fun startServer() {
        var usbPath: File? = null
        
        // Auto-detect attached USB / External Drives
        val externalDirs = getExternalFilesDirs(null)
        for (dir in externalDirs) {
            if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                // Android returns a deep app-specific path. We strip it to get the root drive path.
                val pathStr = dir.absolutePath
                val rootPath = pathStr.substringBefore("/Android/data/")
                usbPath = File(rootPath)
                break
            }
        }

        if (usbPath != null && usbPath.exists()) {
            server = SparoServer(usbPath, 8080)
            server?.start()
            Toast.makeText(this, "SPARO TV+ running from: $usbPath", Toast.LENGTH_LONG).show()
        } else {
            // Fallback if the drive isn't detected immediately
            Toast.makeText(this, "No external drive found! Plug it in and restart app.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
    }
}
