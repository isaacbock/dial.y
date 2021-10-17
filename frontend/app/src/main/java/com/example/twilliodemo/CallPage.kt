package com.example.twilliodemo

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.okhttp.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.Text


class CallPage : AppCompatActivity() {
    val constants = Constants()
    lateinit var phoneNumber:String
    lateinit var questionString: String

    lateinit var callId: String
    lateinit var callStatus: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_page)

        phoneNumber = intent.getStringExtra("PHONE_NUMBER")!!
        questionString = intent.getStringExtra("QUESTION_STRING")!!
        Log.e("Number got from home", phoneNumber)

        findViewById<TextView>(R.id.inProgressNumber).text = phoneNumber
        findViewById<TextView>(R.id.questionText).text = "Question:\n$questionString"

        callStatus = findViewById<TextView>(R.id.callStatus)

        makeFirstRequest()

    }


    fun makeFirstRequest(){
        var jsonArray = JSONArray()
        jsonArray.put(questionString)

        try{
            val requestQueue: RequestQueue = Volley.newRequestQueue(this)
            val URL = constants.herokuappUrl
            val jsonBody = JSONObject()
            jsonBody.put("phoneNumber", phoneNumber)
            jsonBody.put("questions", jsonArray)
            val requestBody = jsonBody.toString().toByteArray()

            val stringRequest = object : StringRequest(Request.Method.POST, URL, Response.Listener { response ->
                runOnUiThread {
                    var callStatus = findViewById<TextView>(R.id.callStatus)
                    callStatus.text = "Call in progress"
                }
                Log.i("Call ID From Twilio", response.toString())
                callId = response.toString()
            }, Response.ErrorListener { error ->
                Log.i("POST ERROR", "Error :" + error.toString())
            }){
                override fun getBodyContentType(): String {
                    return "application/json"
                }

                @Throws(AuthFailureError::class)
                override fun getBody(): ByteArray {
                    return requestBody
                }
            }

            requestQueue!!.add(stringRequest!!)
        }catch (e: JSONException){

        }
//        val body = MultipartBuilder()
//                .type(MultipartBuilder.FORM)
//                .addFormDataPart("phoneNumber", phoneNumber)
//                .addFormDataPart("questions", "[$questionString]")
//                .build()
//
//        val client = OkHttpClient()
//        val request = Request.Builder()
//                .header("Content-Type", "application/json")
//                .url(constants.herokuappUrl)
//                .post(body)
//                .build()
//
//        client.newCall(request).enqueue(object:Callback{
//            override fun onFailure(request: Request?, e: IOException?) {
//                Log.e("POST", "FAILED!!!!")
//                Log.e("FAIL MESSAGE", e.toString())
//            }
//
//            override fun onResponse(response: Response?) {
//                Log.e("POST", "SUCCESS!!!")
//                Log.e("response", response.toString())
//            }
//        })
    }
}