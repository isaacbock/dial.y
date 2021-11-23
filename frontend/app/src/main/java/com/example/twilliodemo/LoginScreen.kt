package com.example.twilliodemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.fragment.app.FragmentActivity
import android.content.res.Configuration;
import android.os.Build
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale;


class LoginScreen : AppCompatActivity() {

    lateinit var mGoogleSignInClient: GoogleSignInClient;
    var RC_SIGN_IN = 123

    lateinit var currentLanguage: TextView
    lateinit var languageButton: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Localization via https://stackoverflow.com/a/9173571
        val config = resources.configuration
        var lang = SavedPreferences.getLocale(this)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            config.setLocale(locale)
        else
            config.locale = locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
        setContentView(R.layout.activity_login_screen)
        SavedPreferences.setLanguageUpdated(this, "false")

        // Hide title bar
        supportActionBar?.hide()

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        SavedPreferences.saveClient(mGoogleSignInClient)

        var signInButton = findViewById(R.id.sign_in_button) as CardView
        signInButton.setOnClickListener {
            signIn()
        }

        currentLanguage = findViewById(R.id.current_language)
        languageButton = findViewById(R.id.language)
        languageButton.setOnClickListener {
            val intent = Intent(this, ChooseAppLanguage::class.java)
            startActivity(intent)
        }
        if (lang=="") {
            SavedPreferences.setLocale(this, "en")
            lang = "en"
        }
        when (lang) {
            "ar" -> currentLanguage.text = "عربي"
            "fr" -> currentLanguage.text = "Français"
            "en" -> currentLanguage.text = "English"
            "hi" -> currentLanguage.text = "हिंदी"
            "it" -> currentLanguage.text = "Italiano"
            "ja" -> currentLanguage.text = "日本語"
            "ko" -> currentLanguage.text = "한국인"
            "zh" -> currentLanguage.text = "中文"
            "pt" -> currentLanguage.text = "Português"
            "ru" -> currentLanguage.text = "русский"
            "es" -> currentLanguage.text = "Español"
            "sw" -> currentLanguage.text = "Kiswahili"
        }
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.getSignInIntent()
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onStart() {
        super.onStart()
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(this)
        updateUI(account)
    }

    override fun onResume() {
        super.onResume()
        if (SavedPreferences.languageUpdated(this)=="true") {
            recreate()
        }
    }

    fun updateUI(account: GoogleSignInAccount?) {
        if (account!=null) {
            SavedPreferences.setEmail(this,account?.email)
            SavedPreferences.setDisplayName(this,account?.displayName)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        else {
            SavedPreferences.setEmail(this,"email")
            SavedPreferences.setDisplayName(this,"displayName")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully, show authenticated UI.
            updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Sign in", "signInResult:failed code=" + e.statusCode)
            updateUI(null)
        }
    }

}