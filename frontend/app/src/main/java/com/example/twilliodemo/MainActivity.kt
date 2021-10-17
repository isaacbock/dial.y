package com.example.twilliodemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val REQUEST_MICROPHONE_CODE = 100

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkMicrophonePermission()

        var callButton = findViewById<Button>(R.id.callButton)
        var phoneNumber = findViewById<TextView>(R.id.phoneNumber)
        var questionTextEdit = findViewById<TextView>(R.id.questionTextEdit)

        callButton.setOnClickListener(){
            Log.e("Button", "CLicked!!")


            val phoneNumberString = phoneNumber.text.toString()
            Log.e("number", phoneNumberString)

            //TODO: Check if it is number only
            if (phoneNumberString == "" || phoneNumberString.length != 10){
                alertDialog("Phone number", "Please enter a valid phone number", "Okay")
            }else{
                onCallButton(phoneNumberString, questionTextEdit.text.toString())
            }
        }


    }

    private fun onCallButton(phoneNumber: String, question: String) {
        val intent = Intent(this, CallPage::class.java).apply{
            putExtra("PHONE_NUMBER", phoneNumber)
            putExtra("QUESTION_STRING", question)
        }
        startActivity(intent)
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

    fun alertDialog(title: String, message: String, positiveText: String){
        AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText) { _, _ ->
                }
                .show()
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