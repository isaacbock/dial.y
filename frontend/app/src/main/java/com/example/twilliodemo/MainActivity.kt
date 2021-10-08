package com.example.twilliodemo

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val REQUEST_MICROPHONE_CODE = 100

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkMicrophonePermission()
    }

    fun checkMicrophonePermission(){
        val isChecked = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        Log.e("isChecked", isChecked.toString())
        if (isChecked != PackageManager.PERMISSION_GRANTED){
            Log.e("PERMISSION", "NO PERMISSION, ASKING FOR IT")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE_CODE)
        }else{
            Log.i("PERMISSION", "Microphone permission granted")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode){
            REQUEST_MICROPHONE_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    AlertDialog.Builder(this)
                            .setTitle("Microphone Permission")
                            .setMessage("Microphone permission is granted")
                            .setPositiveButton("dismiss") { _, _ ->
                            }
                            .show()
                }else{
                    AlertDialog.Builder(this)
                            .setTitle("Microphone Permission")
                            .setMessage("Microphone permission is not granted")
                            .setPositiveButton("dismiss") { _, _ ->
                            }
                            .show()
                }
            }
        }
    }
}