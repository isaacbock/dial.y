package com.example.twilliodemo

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import android.widget.RadioGroup




class ChooseLanguage : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        var returnButton = findViewById<Button>(R.id.returnButton)
        var radioGroup = findViewById<View>(R.id.radioGroup)
        var translateLanguage = "en"


        var arBox = findViewById<RadioButton>(R.id.arButton)
        var frBox = findViewById<RadioButton>(R.id.frButton)
        var enBox = findViewById<RadioButton>(R.id.enButton)
        var hiBox = findViewById<RadioButton>(R.id.hiButton)
        var itBox = findViewById<RadioButton>(R.id.itButton)
        var jaBox = findViewById<RadioButton>(R.id.jaButton)
        var koBox = findViewById<RadioButton>(R.id.koButton)
        var zhBox = findViewById<RadioButton>(R.id.zhButton)
        var ptBox = findViewById<RadioButton>(R.id.ptButton)
        var ruBox = findViewById<RadioButton>(R.id.ruButton)
        var esBox = findViewById<RadioButton>(R.id.esButton)
        var swBox = findViewById<RadioButton>(R.id.swButton)



        returnButton.setOnClickListener(){
            if(arBox.isChecked){translateLanguage = "ar"}
            if(frBox.isChecked){translateLanguage = "fr"}
            if(enBox.isChecked){translateLanguage = "en"}
            if(hiBox.isChecked){translateLanguage = "hi"}
            if(itBox.isChecked){translateLanguage = "it"}
            if(jaBox.isChecked){translateLanguage = "ja"}
            if(koBox.isChecked){translateLanguage = "ko"}
            if(zhBox.isChecked){translateLanguage = "zh"}
            if(ptBox.isChecked){translateLanguage = "pt"}
            if(ruBox.isChecked){translateLanguage = "ru"}
            if(esBox.isChecked){translateLanguage = "es"}
            if(swBox.isChecked){translateLanguage = "sw"}
            onReturnButton(translateLanguage)
        }

    }

    //Create new activity for language choice
    private fun onReturnButton(language: String) {
        Log.e("language is: ", language)

        val intent = Intent(this, StartCall::class.java).apply{
            putExtra("LANGUAGE_STRING", language)}
        startActivity(intent)

    }



}