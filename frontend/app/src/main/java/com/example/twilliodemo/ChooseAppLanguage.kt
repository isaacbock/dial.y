package com.example.twilliodemo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import java.util.*


class ChooseAppLanguage : AppCompatActivity() {


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
        setContentView(R.layout.activity_choose_app_language)

        val actionBar: ActionBar? = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.chooseLanguage))
        }

        fun changeLanguage(locale:String) {
            SavedPreferences.setLocale(this, locale)
            SavedPreferences.setLanguageUpdated(this, "true")
            finish()
        }

        findViewById<Button>(R.id.arButton).setText(getString(R.string.arabic) + " // " + "اللغة العربية")
        findViewById<Button>(R.id.frButton).setText(getString(R.string.french) + " // " + "français")
        findViewById<Button>(R.id.enButton).setText(getString(R.string.english) + " // " + "English")
        findViewById<Button>(R.id.hiButton).setText(getString(R.string.hindi) + " // " + "हिंदी")
        findViewById<Button>(R.id.itButton).setText(getString(R.string.italian) + " // " + "italiano")
        findViewById<Button>(R.id.jaButton).setText(getString(R.string.japanese) + " // " + "日本語")
        findViewById<Button>(R.id.koButton).setText(getString(R.string.korean) + " // " + "한국인")
        findViewById<Button>(R.id.zhButton).setText(getString(R.string.chinese) + " // " + "中文")
        findViewById<Button>(R.id.ptButton).setText(getString(R.string.portuguese) + " // " + "português")
        findViewById<Button>(R.id.ruButton).setText(getString(R.string.russian) + " // " + "русский")
        findViewById<Button>(R.id.esButton).setText(getString(R.string.spanish) + " // " + "español")
        findViewById<Button>(R.id.swButton).setText(getString(R.string.swahili) + " // " + "kiswahili")

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