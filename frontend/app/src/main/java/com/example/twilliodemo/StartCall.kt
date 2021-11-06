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





class StartCall : AppCompatActivity() {

    lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_call)

        setTitle("Hi " + SavedPreferences.getDisplayName(this) +"!");

        mGoogleSignInClient = SavedPreferences.mGoogleSignInClient

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

    }

    fun alertDialog(title: String, message: String, positiveText: String){
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->
            }
            .show()
    }

    private fun onCallButton(phoneNumber: String, question: String) {
        val intent = Intent(this, CallPage::class.java).apply{
            putExtra("PHONE_NUMBER", phoneNumber)
            putExtra("QUESTION_STRING", question)
        }
        startActivity(intent)
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