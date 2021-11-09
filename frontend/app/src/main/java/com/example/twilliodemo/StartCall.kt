package com.example.twilliodemo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.AuthFailureError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import android.telephony.PhoneNumberFormattingTextWatcher
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText



var translate = false

class StartCall : AppCompatActivity() {

    lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_call)

        setTitle("Hi " + SavedPreferences.getDisplayName(this) +"!");

        mGoogleSignInClient = SavedPreferences.mGoogleSignInClient

        var callButton = findViewById<Button>(R.id.callButton)
        var languageButton = findViewById<Button>(R.id.languageButton)
        var phoneNumber = findViewById<TextView>(R.id.phoneNumber)
        var questionTextEdit = findViewById<TextView>(R.id.questionTextEdit)
        //languageString = findViewById<TextView>(R.id.languageText)

        var languageString = "en"

        if(translate)
        {
            languageString = intent.getStringExtra("LANGUAGE_STRING")!!
        }

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
                if(translate)
                {
                    languageString = intent.getStringExtra("LANGUAGE_STRING")!!
                }

                onCallButton(phoneNumberString, questionTextString, languageString)
            }
        }

        languageButton.setOnClickListener(){
            val intent = Intent(this, ChooseLanguage::class.java)
            startActivity(intent)
            translate = true

        }

    }

    //Code to call alert dialog
    fun alertDialog(title: String, message: String, positiveText: String){
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->
            }
            .show()
    }

    //Create new activity by passing in phone number and question string
    private fun onCallButton(phoneNumber: String, question: String, language: String) {
        Log.e("will translate call to ", language)

        val intent = Intent(this, CallPage::class.java).apply{
            putExtra("PHONE_NUMBER", phoneNumber)
            putExtra("QUESTION_STRING", question)
            putExtra("LANGUAGE_STRING", language)

        }
        startActivity(intent)
    }

    //Remove focus from TextEdit field when blank space is touched
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event!!.action === MotionEvent.ACTION_DOWN) {
            val v: View? = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event!!.rawX.toInt(), event!!.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_start_call_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.log_out -> {
            mGoogleSignInClient.signOut().addOnCompleteListener {
                val intent= Intent(this, LoginScreen::class.java)
                Toast.makeText(this,"Logging Out",Toast.LENGTH_SHORT).show()
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