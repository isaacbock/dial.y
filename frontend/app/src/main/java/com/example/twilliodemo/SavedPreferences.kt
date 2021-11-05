package com.example.twilliodemo

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log

// Referenced from https://medium.com/swlh/google-login-and-logout-in-android-with-firebase-kotlin-implementation-73cf6a5a989e w/ permission

object SavedPreferences {

    const val EMAIL = "email"
    const val DISPLAYNAME ="displayName"

    private  fun getSharedPreference(ctx: Context?): SharedPreferences? {
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

}