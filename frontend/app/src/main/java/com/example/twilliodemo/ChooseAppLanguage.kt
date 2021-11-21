package com.example.twilliodemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity


class ChooseAppLanguage : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_app_language)

        fun changeLanguage(locale:String) {
            SavedPreferences.setLocale(this, locale)
            SavedPreferences.setLanguageUpdated(this, "true")
            finish()
        }

        findViewById<Button>(R.id.arButton).setOnClickListener(){
            changeLanguage("ar")
        }
        findViewById<Button>(R.id.frButton).setOnClickListener(){
            changeLanguage("fr")
        }
        findViewById<Button>(R.id.enButton).setOnClickListener(){
            changeLanguage("en")
        }
        findViewById<Button>(R.id.hiButton).setOnClickListener(){
            changeLanguage("hi")
        }
        findViewById<Button>(R.id.itButton).setOnClickListener(){
            changeLanguage("it")
        }
        findViewById<Button>(R.id.jaButton).setOnClickListener(){
            changeLanguage("ja")
        }
        findViewById<Button>(R.id.koButton).setOnClickListener(){
            changeLanguage("ko")
        }
        findViewById<Button>(R.id.zhButton).setOnClickListener(){
            changeLanguage("zh")
        }
        findViewById<Button>(R.id.ptButton).setOnClickListener(){
            changeLanguage("pt")
        }
        findViewById<Button>(R.id.ruButton).setOnClickListener(){
            changeLanguage("ru")
        }
        findViewById<Button>(R.id.esButton).setOnClickListener(){
            changeLanguage("es")
        }
        findViewById<Button>(R.id.swButton).setOnClickListener(){
            changeLanguage("sw")
        }

    }

}