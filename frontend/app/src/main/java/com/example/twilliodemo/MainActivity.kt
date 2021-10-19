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
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.HashMap
import android.telephony.PhoneNumberFormattingTextWatcher




private const val REQUEST_MICROPHONE_CODE = 100

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkMicrophonePermission()

        var callButton = findViewById<Button>(R.id.callButton)
        var phoneNumber = findViewById<TextView>(R.id.phoneNumber)
        var questionTextEdit = findViewById<TextView>(R.id.questionTextEdit)

        if (phoneNumber != null) {
            phoneNumber.addTextChangedListener(PhoneNumberFormattingTextWatcher())
        }
        if (questionTextEdit != null) {
            questionTextEdit.setHorizontallyScrolling(false);
            questionTextEdit.setMaxLines(4);
        }

        callButton.setOnClickListener(){
            Log.e("Button", "Clicked!!")


            val phoneNumberString = phoneNumber.text.toString()
            val questionTextString = questionTextEdit.text.toString()
            Log.e("number", phoneNumberString)

            //TODO: Check if it is number only
            if (phoneNumberString == "" || phoneNumberString.length != 14){
                alertDialog("Sorry...", "Please enter a valid phone number.", "Okay")
            } else if (questionTextString == "") {
                alertDialog("Sorry...", "Please ask a question.", "Okay")
            } else {
                onCallButton(phoneNumberString, questionTextString)
            }
        }

        //TODO: DELETE!!
        test()


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

    fun test(){
        try{
            val constants = Constants()
            val requestQueue: RequestQueue = Volley.newRequestQueue(this)
            val URL = constants.herokuappStatusUrl
            val jsonObject = JSONObject()
            val id = "id"
            val callId = "CA2245eabf5c7f79fda673db01b3fda20f"
            jsonObject.put(id, callId)
//            val requestBody = jsonObject.toString().toByteArray()
            val requestBody = jsonObject.toString().toByteArray()


            val stringRequest = object : StringRequest(
                Method.POST,
                URL,
                Response.Listener { response ->
                    Log.i("Response from GET", response.toString())
                    val jsonResponse = JSONObject(response.toString())
                    Log.e("CALL STATUS", jsonResponse["status"].toString())

                    val questionArray = JSONArray(jsonResponse["questions"].toString())
                    val questionArrayObject = JSONObject(questionArray[0].toString())
                    val answerTranscript = questionArrayObject["answerTranscript"].toString()
                    Log.e("Answer transcript", answerTranscript)
                },
                Response.ErrorListener { error ->
                    Log.e("GET ERROR", "Error :" + error.toString())
                }){
                override fun getBodyContentType(): String {
                    return "application/json"
                }

//                override fun getPostParams(): MutableMap<String, String> {
//                    var map = HashMap<String,String>()
//                    map["id"] = callId
//                    return map
//                }

                @Throws(AuthFailureError::class)
                override fun getBody(): ByteArray {
                    return requestBody
                }

            }

            requestQueue!!.add(stringRequest!!)

        }catch (e: JSONException){
            Log.e("JSONExeption", e.toString())
        }
    }
}