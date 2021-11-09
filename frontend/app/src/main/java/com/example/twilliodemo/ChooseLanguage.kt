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

class ChooseLanguage : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        var returnButton = findViewById<Button>(R.id.returnButton)
        var translateLanguage = "en"

        var arBox = findViewById<CheckBox>(R.id.arButton)
        var frBox = findViewById<CheckBox>(R.id.frButton)
        var deBox = findViewById<CheckBox>(R.id.deButton)
        var hiBox = findViewById<CheckBox>(R.id.hiButton)
        var itBox = findViewById<CheckBox>(R.id.itButton)
        var jaBox = findViewById<CheckBox>(R.id.jaButton)
        var koBox = findViewById<CheckBox>(R.id.koButton)
        var zhBox = findViewById<CheckBox>(R.id.zhButton)
        var ptBox = findViewById<CheckBox>(R.id.ptButton)
        var ruBox = findViewById<CheckBox>(R.id.ruButton)
        var esBox = findViewById<CheckBox>(R.id.esButton)
        var swBox = findViewById<CheckBox>(R.id.swButton)

        if(arBox.isChecked){translateLanguage = "ar"}
        if(frBox.isChecked){translateLanguage = "fr"}
        if(deBox.isChecked){translateLanguage = "de"}
        if(hiBox.isChecked){translateLanguage = "hi"}
        if(itBox.isChecked){translateLanguage = "it"}
        if(jaBox.isChecked){translateLanguage = "ja"}
        if(koBox.isChecked){translateLanguage = "ko"}
        if(zhBox.isChecked){translateLanguage = "zh"}
        if(ptBox.isChecked){translateLanguage = "pt"}
        if(ruBox.isChecked){translateLanguage = "ru"}
        if(esBox.isChecked){translateLanguage = "es"}
        if(swBox.isChecked){translateLanguage = "sw"}


        returnButton.setOnClickListener(){
            onReturnButton(translateLanguage)
        }

    }

    //Create new activity for language choice
    private fun onReturnButton(language: String) {
        val intent = Intent(this, StartCall::class.java).apply{
            putExtra("LANGUAGE_STRING", language)}
        startActivity(intent)

    }



}