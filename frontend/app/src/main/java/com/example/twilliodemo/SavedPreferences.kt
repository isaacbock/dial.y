package com.example.twilliodemo

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient

// Referenced from https://medium.com/swlh/google-login-and-logout-in-android-with-firebase-kotlin-implementation-73cf6a5a989e w/ permission

object SavedPreferences {

    const val EMAIL = "email"
    const val DISPLAYNAME = "displayName"
    const val LOCALE = "en"
    const val LANGUAGEUPDATED = "false"
    // Note: BUSINESSLANG must be initialized to an empty string to prevent localization bug
    const val BUSINESSLANG = ""
    const val BUSINESSLANGUPDATED = "false"

    lateinit var mGoogleSignInClient: GoogleSignInClient

    private fun getSharedPreference(ctx: Context?): SharedPreferences? {
        return PreferenceManager.getDefaultSharedPreferences(ctx)
    }

    private fun  editor(context: Context, const:String, string: String){
        getSharedPreference(
            context
        )?.edit()?.putString(const,string)?.apply()
    }

    fun getEmail(context: Context)= getSharedPreference(
        context
    )?.getString(EMAIL,"")
    fun setEmail(context: Context, email: String){
        Log.v("User email", email)
        editor(
            context,
            EMAIL,
            email
        )
    }

    fun getDisplayName(context: Context) = getSharedPreference(
        context
    )?.getString(DISPLAYNAME,"")
    fun setDisplayName(context: Context, displayName:String){
        Log.v("User", displayName)
        editor(
            context,
            DISPLAYNAME,
            displayName
        )
    }

    fun getLocale(context: Context) = getSharedPreference(
        context
    )?.getString(LOCALE,"")
    fun setLocale(context: Context, locale:String){
        Log.v("Locale", locale)
        editor(
            context,
            LOCALE,
            locale
        )
    }
    fun languageUpdated(context: Context) = getSharedPreference(
        context
    )?.getString(LANGUAGEUPDATED,"")
    fun setLanguageUpdated(context: Context, updated:String){
        editor(
            context,
            LANGUAGEUPDATED,
            updated
        )
    }

    fun getBusinessLanguage(context: Context) = getSharedPreference(
        context
    )?.getString(BUSINESSLANG,"")
    fun setBusinessLanguage(context: Context, language:String){
        Log.v("Business language", language)
        editor(
            context,
            BUSINESSLANG,
            language
        )
    }
    fun businessLanguageUpdated(context: Context) = getSharedPreference(
        context
    )?.getString(BUSINESSLANGUPDATED,"")
    fun setBusinessLanguageUpdated(context: Context, updated:String){
        editor(
            context,
            BUSINESSLANGUPDATED,
            updated
        )
    }

    fun saveClient(client: GoogleSignInClient) {
        mGoogleSignInClient = client
    }
    fun getIDToken(context: Context): String {
        //Sometimes app crashes because "GoogleSignIn.getLastSignedInAccount(context)" is null, so I added a if statement to check when it is null.
        if (GoogleSignIn.getLastSignedInAccount(context) == null){
            return "Expired"
        }
        else{
            if (GoogleSignIn.getLastSignedInAccount(context).isExpired) {
                Log.e("Token expired", "Logging out.")
                return "Expired"
            }
            else {
                Log.v("Token", GoogleSignIn.getLastSignedInAccount(context).idToken)
                return GoogleSignIn.getLastSignedInAccount(context).idToken
            }
        }
    }
}