package com.example.twilliodemo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import java.util.*


class ChooseLanguage : AppCompatActivity() {


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
        setContentView(R.layout.settings_activity)

        val actionBar: ActionBar? = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.chooseLanguage))
        }

        fun changeLanguage(locale:String) {
            SavedPreferences.setBusinessLanguage(this, locale)
            SavedPreferences.setBusinessLanguageUpdated(this, "true")
            finish()
        }

        findViewById<Button>(R.id.arButtonBusiness).setText(getString(R.string.arabic) + " // " + "اللغة العربية")
        findViewById<Button>(R.id.frButtonBusiness).setText(getString(R.string.french) + " // " + "français")
        findViewById<Button>(R.id.enButtonBusiness).setText(getString(R.string.english) + " // " + "English")
        findViewById<Button>(R.id.hiButtonBusiness).setText(getString(R.string.hindi) + " // " + "हिंदी")
        findViewById<Button>(R.id.itButtonBusiness).setText(getString(R.string.italian) + " // " + "italiano")
        findViewById<Button>(R.id.jaButtonBusiness).setText(getString(R.string.japanese) + " // " + "日本語")
        findViewById<Button>(R.id.koButtonBusiness).setText(getString(R.string.korean) + " // " + "한국어")
        findViewById<Button>(R.id.zhButtonBusiness).setText(getString(R.string.chinese) + " // " + "中文")
        findViewById<Button>(R.id.ptButtonBusiness).setText(getString(R.string.portuguese) + " // " + "português")
        findViewById<Button>(R.id.ruButtonBusiness).setText(getString(R.string.russian) + " // " + "русский")
        findViewById<Button>(R.id.esButtonBusiness).setText(getString(R.string.spanish) + " // " + "español")

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