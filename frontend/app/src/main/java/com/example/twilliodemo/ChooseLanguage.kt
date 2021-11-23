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

        fun changeLanguage(locale:String) {
            SavedPreferences.setBusinessLanguage(this, locale)
            SavedPreferences.setBusinessLanguageUpdated(this, "true")
            finish()
        }

        findViewById<Button>(R.id.arButtonBusiness).setOnClickListener(){
            changeLanguage("ar")
        }
        findViewById<Button>(R.id.frButtonBusiness).setOnClickListener(){
            changeLanguage("fr")
        }
        findViewById<Button>(R.id.enButtonBusiness).setOnClickListener(){
            changeLanguage("en")
        }
        findViewById<Button>(R.id.hiButtonBusiness).setOnClickListener(){
            changeLanguage("hi")
        }
        findViewById<Button>(R.id.itButtonBusiness).setOnClickListener(){
            changeLanguage("it")
        }
        findViewById<Button>(R.id.jaButtonBusiness).setOnClickListener(){
            changeLanguage("ja")
        }
        findViewById<Button>(R.id.koButtonBusiness).setOnClickListener(){
            changeLanguage("ko")
        }
        findViewById<Button>(R.id.zhButtonBusiness).setOnClickListener(){
            changeLanguage("zh")
        }
        findViewById<Button>(R.id.ptButtonBusiness).setOnClickListener(){
            changeLanguage("pt")
        }
        findViewById<Button>(R.id.ruButtonBusiness).setOnClickListener(){
            changeLanguage("ru")
        }
        findViewById<Button>(R.id.esButtonBusiness).setOnClickListener(){
            changeLanguage("es")
        }

    }

}