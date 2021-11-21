package com.example.twilliodemo

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.cardview.widget.CardView
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import org.json.JSONArray
import org.json.JSONObject
import java.time.*

import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.google.android.flexbox.FlexboxLayout
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    val constants = Constants()
    lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Localization via https://stackoverflow.com/a/9173571
        val config = resources.configuration
        val lang = SavedPreferences.getLocale(this)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            config.setLocale(locale)
        else
            config.locale = locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
        setContentView(R.layout.activity_main)

        mGoogleSignInClient = SavedPreferences.mGoogleSignInClient

        var hi = getResources().getString(R.string.hi)
        var name = SavedPreferences.getDisplayName(this)?.substringBefore(" ")
        if (lang=="ar") {
            setTitle("!" + name + " " + hi)
        }
        else {
            setTitle(hi + " " + name +"!");
        }

        var callButton = findViewById<Button>(R.id.createNewCall)
        callButton.setOnClickListener(){
            val intent = Intent(this, StartCall::class.java)
            startActivity(intent)
        }

        getCallHistory();
    }

    override fun onResume() {
        super.onResume()
        getCallHistory();
    }


    fun getCallHistory() {
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        val URL = constants.herokuappCallHistoryUrl
        val jsonObject = JSONObject()

        val idToken = SavedPreferences.getIDToken(this)
        if (idToken=="Expired") {
            mGoogleSignInClient.signOut().addOnCompleteListener {
                val intent= Intent(this, LoginScreen::class.java)
                Toast.makeText(this,getResources().getString(R.string.session_expired), Toast.LENGTH_SHORT).show()
                startActivity(intent)
                finish()
            }
        }
        else {
            jsonObject.put("userToken", idToken)
            val requestBody = jsonObject.toString().toByteArray()

            val stringRequest = object : StringRequest(
                Request.Method.POST,
                URL,
                Response.Listener { response ->
                    Log.i("Response from POST", response.toString())
                    val callHistory = JSONArray(response.toString())

                    if (callHistory.length()!=0) {

                        // clear outdated calls
                        val callHistoryLayout = findViewById<View>(R.id.callHistory) as FlexboxLayout
                        runOnUiThread {
                            callHistoryLayout.removeAllViews()
                        }

                        // create card for each new call
                        for (i in 0 until callHistory.length()) {
                            val callObject = callHistory.getJSONObject(i)
                            val callTo = callObject["to"].toString()
                            val questionObject = JSONArray(callObject["questions"].toString())
                            val question = JSONObject(questionObject[0].toString())["question"].toString()
                            val dateTimeJsonArray = JSONObject(callObject["date"].toString())
                            val seconds = dateTimeJsonArray["_seconds"].toString()
                            val dateTime = getDateTime(seconds)

                            Log.v("Call History", question)

                            runOnUiThread {
                                val inflater = LayoutInflater.from(this)
                                val previousCallCard = inflater.inflate(
                                    R.layout.previous_call,
                                    callHistoryLayout,
                                    false
                                ) as CardView
                                previousCallCard.setOnClickListener {
                                    val intent = Intent(this, CallPage::class.java).apply{
                                        putExtra("CALL", callObject.toString())
                                    }
                                    startActivity(intent)
                                }
                                previousCallCard.findViewById<TextView>(R.id.previousCallNumber).text = callTo
                                previousCallCard.findViewById<TextView>(R.id.previousCallQuestion).text = question
                                previousCallCard.findViewById<TextView>(R.id.previousCallDateTime).text = dateTime
                                callHistoryLayout.addView(previousCallCard)

                            }
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
        }
    }

    private fun getDateTime(s: String): String? {
        try {
            val sdf = SimpleDateFormat("MM/dd/yyyy")
            val netDate = Date(s.toLong() * 1000)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_start_call_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.log_out -> {
            mGoogleSignInClient.signOut().addOnCompleteListener {
                val intent= Intent(this, LoginScreen::class.java)
                Toast.makeText(this,getResources().getString(R.string.logging_out), Toast.LENGTH_SHORT).show()
                startActivity(intent)
                finish()
            }
            true
        }
        else -> {
            // Else the user's action was not recognized.
            super.onOptionsItemSelected(item)
        }
    }
}