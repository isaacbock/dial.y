package com.example.twilliodemo

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.media.*
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import android.graphics.drawable.ColorDrawable
import android.widget.*
import com.example.twilliodemo.SavedPreferences.mGoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import io.socket.client.IO
import io.socket.client.Socket
import kotlin.concurrent.schedule


class CallPage : AppCompatActivity() {
    val constants = Constants()
//    lateinit var mGoogleSignInClient: GoogleSignInClient

    lateinit var phoneNumber:String
    lateinit var questionString: String
    lateinit var idToken: String

    lateinit var callId: String

    lateinit var callData: JSONObject

    lateinit var dialingStatus: ImageView
    lateinit var askingStatus: ImageView
    lateinit var recordingStatus: ImageView
    lateinit var dialingStatusText: TextView
    lateinit var askingStatusText: TextView
    lateinit var recordingStatusText: TextView

    lateinit var playButton: ImageButton

    lateinit var callResult: TextView
    lateinit var audioLink: String

    lateinit var mSocket: Socket;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_page)

        mGoogleSignInClient = SavedPreferences.mGoogleSignInClient

        dialingStatus = findViewById<ImageView>(R.id.dialingStatus)
        askingStatus = findViewById<ImageView>(R.id.askingStatus)
        recordingStatus = findViewById<ImageView>(R.id.recordingStatus)
        dialingStatusText = findViewById<TextView>(R.id.dialingStatusText)
        askingStatusText = findViewById<TextView>(R.id.askingStatusText)
        recordingStatusText = findViewById<TextView>(R.id.recordingStatusText)

        playButton = findViewById<ImageButton>(R.id.playButton)
        callResult = findViewById<TextView>(R.id.callResult)

        // if the call already occurred, display this previous data
        if (intent.getStringExtra("CALL")!=null) {
            callData = JSONObject(intent.getStringExtra("CALL"))
            updateUI()
        }

        //else if the call is new, initiate the call
        if (intent.getStringExtra("PHONE_NUMBER")!=null && intent.getStringExtra("QUESTION_STRING")!=null) {
            phoneNumber = intent.getStringExtra("PHONE_NUMBER")!!
            questionString = intent.getStringExtra("QUESTION_STRING")!!

            setTitle(phoneNumber);
            findViewById<TextView>(R.id.questionText).text = questionString

            idToken = SavedPreferences.getIDToken(this)
            if (idToken=="Expired") {
                mGoogleSignInClient.signOut().addOnCompleteListener {
                    val intent= Intent(this, LoginScreen::class.java)
                    Toast.makeText(this,"Session expired.", Toast.LENGTH_SHORT).show()
                    startActivity(intent)
                    finish()
                }
            }
            else {
                Log.v("Calling ", phoneNumber)
                makeCallRequest()
            }
        }

        // Socket.IO Connection
        try {
            mSocket = IO.socket(constants.herokuSocketUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("Socket.io", "Failed to connect.")
        }
        mSocket.connect()
        mSocket.on(Socket.EVENT_CONNECT)  {
            Log.d("Socket.io", "Connected!")
        }

        //Socket.IO Receiving
        mSocket.on("status") { args ->
            if (args[0] != null){
                val jsonObject = args[0] as JSONObject
                Log.e("status from socket", jsonObject.toString())
                callData = jsonObject
                runOnUiThread{
                    updateUI()
                }
            }
        }

        //Socket.IO Sending data
        var jsonArray = JSONArray()
        jsonArray.put(questionString)
        val jsonBody = JSONObject()
        jsonBody.put("phoneNumber", phoneNumber)
        jsonBody.put("questions", jsonArray)
        mSocket.emit("call", jsonBody)

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
            jsonBody.put("userToken", idToken)
            val requestBody = jsonBody.toString().toByteArray()

            val stringRequest = object : StringRequest(
                Request.Method.POST,
                URL,
                Response.Listener { response ->
                    Log.i("Call ID From Twilio", response.toString())
                    callId = response.toString()
                    val callIdJson = JSONObject()
                    callIdJson.put("phoneId", callId)
                    mSocket.emit("callId", callIdJson)
//                    makeStatusRequest()
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

                val stringRequest = object : StringRequest(
                    Request.Method.POST,
                    URL,
                    Response.Listener { response ->
                        Log.i("Response from GET", response.toString())
                        val jsonResponse = JSONObject(response.toString())
                        Log.e("CALL STATUS", jsonResponse["status"].toString())

                        callData = jsonResponse
                        updateUI()
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

            }catch (e: JSONException){
                Log.e("JSONException", e.toString())
            }
        }

    }

    fun updateUI() {
        val questionArray = JSONArray(callData["questions"].toString())
        val questionArrayObject = JSONObject(questionArray[0].toString())

        val answerTranscript = questionArrayObject["answerTranscript"].toString()
        val answerAudio = questionArrayObject["answerAudio"].toString()
        val answerStatus = questionArrayObject["status"].toString()
        Log.e("Answer transcript", answerTranscript)

        phoneNumber = callData["to"].toString()
        questionString = questionArrayObject["question"].toString()

        setTitle(phoneNumber);

        runOnUiThread {
            findViewById<TextView>(R.id.questionText).text = questionString
        }

        // If the call is still in progress, continue requesting future updates
//        if(callData["status"].toString()!="Completed" && callData["status"].toString()!="Hung Up" && callData["status"].toString()!="No Answer") {
//            Timer().schedule(2000) {
//                makeStatusRequest()
//            }
//        }

        // When the call is ongoing, update the progress visualization accordingly.
        if (callData["status"].toString() == "Dialing") {
            runOnUiThread {
                dialingStatus.setImageResource(R.drawable.dialing_complete)
                dialingStatusText.setTypeface(null, Typeface.BOLD);
                dialingStatusText.setTextColor(getResources().getColor(R.color.black))
            }
        }
        when(questionArrayObject["status"].toString()) {
            "Asking", "Prompting" -> {
                runOnUiThread {
                    dialingStatus.setImageResource(R.drawable.dialing_complete)
                    dialingStatusText.setTypeface(null, Typeface.NORMAL)
                    dialingStatusText.setTextColor(getResources().getColor(R.color.black))

                    askingStatus.setImageResource(R.drawable.asking_complete)
                    askingStatusText.setTypeface(null, Typeface.BOLD);
                    askingStatusText.setTextColor(getResources().getColor(R.color.black))
                }
            }
            "Recording" -> {
                runOnUiThread {
                    dialingStatus.setImageResource(R.drawable.dialing_complete)
                    dialingStatusText.setTypeface(null, Typeface.NORMAL)
                    dialingStatusText.setTextColor(getResources().getColor(R.color.black))

                    askingStatus.setImageResource(R.drawable.asking_complete)
                    askingStatusText.setTypeface(null, Typeface.NORMAL);
                    askingStatusText.setTextColor(getResources().getColor(R.color.black))

                    recordingStatus.setImageResource(R.drawable.recording_complete);
                    recordingStatusText.setTypeface(null, Typeface.BOLD);
                    recordingStatusText.setTextColor(getResources().getColor(R.color.black))
                }
            }
        }

        if (answerAudio != "null" && answerTranscript == "null"){
            runOnUiThread{
                dialingStatus.setImageResource(R.drawable.dialing_complete)
                dialingStatusText.setTypeface(null, Typeface.NORMAL)
                dialingStatusText.setTextColor(getResources().getColor(R.color.black))

                askingStatus.setImageResource(R.drawable.asking_complete)
                askingStatusText.setTypeface(null, Typeface.NORMAL);
                askingStatusText.setTextColor(getResources().getColor(R.color.black))

                recordingStatus.setImageResource(R.drawable.recording_complete);
                recordingStatusText.setTypeface(null, Typeface.NORMAL);
                recordingStatusText.setTextColor(getResources().getColor(R.color.black))

                audioLink = answerAudio
                playButton.setVisibility(View.VISIBLE)
                playButton.setOnClickListener() {
                    playAudio();
                }
                callResult.setVisibility(View.VISIBLE)
                callResult.text = "..."
                recordingStatusText.setTypeface(null, Typeface.NORMAL)
            }
        }
        else if (answerAudio != "null" && answerTranscript != "null"){
            runOnUiThread{
                dialingStatus.setImageResource(R.drawable.dialing_complete)
                dialingStatusText.setTypeface(null, Typeface.NORMAL)
                dialingStatusText.setTextColor(getResources().getColor(R.color.black))

                askingStatus.setImageResource(R.drawable.asking_complete)
                askingStatusText.setTypeface(null, Typeface.NORMAL);
                askingStatusText.setTextColor(getResources().getColor(R.color.black))

                recordingStatus.setImageResource(R.drawable.recording_complete);
                recordingStatusText.setTypeface(null, Typeface.NORMAL);
                recordingStatusText.setTextColor(getResources().getColor(R.color.black))

                audioLink = answerAudio
                playButton.setVisibility(View.VISIBLE)
                playButton.setOnClickListener() {
                    playAudio();
                }
                callResult.setVisibility(View.VISIBLE)
                if (answerTranscript == "") {
                    callResult.text = "Sorry, transcription is not available for this call."
                }
                else {
                    callResult.text = answerTranscript
                }
            }
        }

        // If the call was hung up or was not answered, tell the user.
        if (callData["status"].toString() == "No Answer") {
            callResult.setVisibility(View.VISIBLE)
            callResult.text = "Sorry, nobody answered the phone. Try again later!"

            dialingStatus.setImageResource(R.drawable.dialing_complete)
            dialingStatusText.setTypeface(null, Typeface.NORMAL);
            dialingStatusText.setTextColor(getResources().getColor(R.color.black))
        }
        else if (callData["status"].toString() == "Hung Up") {
            callResult.setVisibility(View.VISIBLE)
            callResult.text = "Sorry, nobody had an answer for your question and the call was ended."

            dialingStatus.setImageResource(R.drawable.dialing_complete)
            dialingStatusText.setTypeface(null, Typeface.NORMAL)
            dialingStatusText.setTextColor(getResources().getColor(R.color.black))

            askingStatus.setImageResource(R.drawable.asking_complete)
            askingStatusText.setTypeface(null, Typeface.NORMAL);
            askingStatusText.setTextColor(getResources().getColor(R.color.black))
        }
    }

    fun playAudio() {
        if (audioLink != null) {
            val uri = Uri.parse(audioLink)
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .build()
                )
                setDataSource(audioLink)
                prepare()
                start()
            }
        }
    }

    override fun onBackPressed() {
        mSocket.disconnect()
        finish()
    }
}