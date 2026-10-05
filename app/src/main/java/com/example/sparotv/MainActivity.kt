import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {
    private var server: SparoServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Android 12 All Files Access to read the USB Hard Drive
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } else {
            startServer()
        }
    }

    private fun startServer() {
        // Replace with your external hard drive's UUID mount path
        val externalDrivePath = File("/storage/YOUR_DRIVE_UUID/") 
        
        server = SparoServer(externalDrivePath, 8080)
        server?.start()
        
        // The app is now running. Connect any device on the Wi-Fi to: 
        // http://<Your-Redmi-9-Pro-IP>:8080
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
    }
}
