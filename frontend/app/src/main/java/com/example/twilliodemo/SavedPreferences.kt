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
    const val DISPLAYNAME ="displayName"

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

    fun saveClient(client: GoogleSignInClient) {
        mGoogleSignInClient = client
    }
    fun getIDToken(context: Context): String {
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