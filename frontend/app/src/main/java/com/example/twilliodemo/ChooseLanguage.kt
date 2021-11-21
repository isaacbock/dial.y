package com.example.twilliodemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity


class ChooseLanguage : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        var returnButton = findViewById<Button>(R.id.returnButton)
        var translateLanguage = "en"


        var arBox = findViewById<RadioButton>(R.id.arButtonRadio)
        var frBox = findViewById<RadioButton>(R.id.frButtonRadio)
        var enBox = findViewById<RadioButton>(R.id.enButtonRadio)
        var hiBox = findViewById<RadioButton>(R.id.hiButtonRadio)
        var itBox = findViewById<RadioButton>(R.id.itButtonRadio)
        var jaBox = findViewById<RadioButton>(R.id.jaButtonRadio)
        var koBox = findViewById<RadioButton>(R.id.koButtonRadio)
        var zhBox = findViewById<RadioButton>(R.id.zhButtonRadio)
        var ptBox = findViewById<RadioButton>(R.id.ptButtonRadio)
        var ruBox = findViewById<RadioButton>(R.id.ruButtonRadio)
        var esBox = findViewById<RadioButton>(R.id.esButtonRadio)
        var swBox = findViewById<RadioButton>(R.id.swButtonRadio)



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