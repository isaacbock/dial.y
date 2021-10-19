package com.example.twilliodemo

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import android.media.*
import android.net.Uri
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*


class CallPage : AppCompatActivity() {
    val constants = Constants()
    lateinit var phoneNumber:String
    lateinit var questionString: String

    lateinit var callId: String
    lateinit var callStatus: TextView

    lateinit var dialingStatus: FrameLayout
    lateinit var askingStatus: FrameLayout
    lateinit var recordingStatus: FrameLayout

    lateinit var playButton: ImageButton

    lateinit var callResult: TextView
    lateinit var audioLink: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_page)

        phoneNumber = intent.getStringExtra("PHONE_NUMBER")!!
        questionString = intent.getStringExtra("QUESTION_STRING")!!
        Log.e("Number got from home", phoneNumber)

        findViewById<TextView>(R.id.inProgressNumber).text = phoneNumber
        findViewById<TextView>(R.id.questionText).text = questionString

        dialingStatus = findViewById<FrameLayout>(R.id.dialingStatus)
        askingStatus = findViewById<FrameLayout>(R.id.askingStatus)
        recordingStatus = findViewById<FrameLayout>(R.id.recordingStatus)

        playButton = findViewById<ImageButton>(R.id.playButton)

        callStatus = findViewById<TextView>(R.id.callStatus)
        callResult = findViewById<TextView>(R.id.callResult)


        makeCallRequest()

        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                makeStatusRequest()
            }
        }, 0, 2000)

    }


    fun makeCallRequest(){
        var jsonArray = JSONArray()
        jsonArray.put(questionString)

        try{
            val requestQueue: RequestQueue = Volley.newRequestQueue(this)
            val URL = constants.herokuappCallUrl
            val jsonBody = JSONObject()
            jsonBody.put("phoneNumber", phoneNumber)
            jsonBody.put("questions", jsonArray)
            val requestBody = jsonBody.toString().toByteArray()

            val stringRequest = object : StringRequest(
                Request.Method.POST,
                URL,
                Response.Listener { response ->
                    runOnUiThread {
                        callStatus.text = "In Progress"
                    }
                    Log.i("Call ID From Twilio", response.toString())
                    callId = response.toString()
                },
                Response.ErrorListener { error ->
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
            Log.e("JSONException", e.toString())
        }
    }

    fun makeStatusRequest(){
        if (::callId.isInitialized){
            try{
                val requestQueue: RequestQueue = Volley.newRequestQueue(this)
                val URL = constants.herokuappStatusUrl
                val jsonObject = JSONObject()
                jsonObject.put("id", callId)
                val requestBody = jsonObject.toString().toByteArray()
                Log.e("jsonObject", jsonObject.toString())

//                val id = "id"
//                val requestBody = "{$id:$callId}".toByteArray()
//                Log.e("RequestBody", "{$id:$callId}")


                val stringRequest = object : StringRequest(
                    Request.Method.POST,
                    URL,
                    Response.Listener { response ->
                        Log.i("Response from GET", response.toString())
                        val jsonResponse = JSONObject(response.toString())
                        Log.e("CALL STATUS", jsonResponse["status"].toString())

                        val questionArray = JSONArray(jsonResponse["questions"].toString())
                        val questionArrayObject = JSONObject(questionArray[0].toString())

                        val answerTranscript = questionArrayObject["answerTranscript"].toString()
                        val answerAudio = questionArrayObject["answerAudio"].toString()
                        val answerStatus = questionArrayObject["status"].toString()
                        Log.e("Answer transcript", answerTranscript)

                        runOnUiThread {
                            callStatus.text = jsonResponse["status"].toString();
                        }

                        when(questionArrayObject["status"].toString()){
                            "Asking" -> {
                                runOnUiThread {
                                    askingStatus.setBackgroundResource(R.drawable.circle_filled);
                                }
                            }
                            "Recording" -> {
                                runOnUiThread {
                                    recordingStatus.setBackgroundResource(R.drawable.circle_filled);
                                }
                            }
                        }

                        if (answerAudio != "null" && answerTranscript == "null"){
                            runOnUiThread{
                                audioLink = answerAudio
                                playButton.setVisibility(View.VISIBLE)
                                playButton.setOnClickListener() {
                                    playAudio();
                                }
                                callResult.text = "Transcribing..."
                            }
                        }
                        else if (answerAudio != "null" && answerTranscript != "null"){
                            runOnUiThread{
                                callResult.text = answerTranscript
                            }
                        }
                    },
                    Response.ErrorListener { error ->
                        Log.i("GET ERROR", "Error :" + error.toString())
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

//                val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, URL, jsonObject,
//                    { response ->
//                        try {
//                            //TODO: Handle your response here
//                            Log.i("Response from GET", response.toString())
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//
//                        }
//                        print(response)
//                    }) { error -> // TODO: Handle error
//                    Log.i("GET ERROR", "Error :" + error.toString())
//                    error.printStackTrace()
//                }
//
//                requestQueue!!.add(jsonObjectRequest!!)
            }catch (e: JSONException){
                Log.e("JSONException", e.toString())
            }
        }

    }

    fun playAudio() {
        if (audioLink != null) {
            val uri = Uri.parse(audioLink)
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(applicationContext, uri)
                prepare()
                start()
            }
        }
    }
}