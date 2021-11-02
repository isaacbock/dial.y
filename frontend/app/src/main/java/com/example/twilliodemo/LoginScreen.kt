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
import android.R.id
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.fragment.app.FragmentActivity


class LoginScreen : AppCompatActivity() {

    lateinit var mGoogleSignInClient: GoogleSignInClient;
    var RC_SIGN_IN = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_screen)

        // Hide title bar
        supportActionBar?.hide()

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        var signInButton = findViewById(R.id.sign_in_button) as CardView
        signInButton.setOnClickListener {
            signIn()
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

    fun updateUI(account: GoogleSignInAccount?) {
        if (account!=null) {
            SavedPreferences.setEmail(this,account.email.toString())
            SavedPreferences.setUsername(this,account.displayName.toString())
            val intent = Intent(this, StartCall::class.java)
            startActivity(intent)
            finish()
        }
        else {
            SavedPreferences.setEmail(this,account?.email.toString())
            SavedPreferences.setUsername(this,account?.displayName.toString())
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

//    lateinit var mGoogleSignInClient: GoogleSignInClient
//    val Req_Code:Int=123
//    private lateinit var firebaseAuth: FirebaseAuth
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_login_screen)
//
//        // Configure Google Sign In inside onCreate mentod
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(getString(R.string.default_web_client_id))
//            .requestEmail()
//            .build()
//        // Get the value of gso inside the GoogleSigninClient
//        mGoogleSignInClient=GoogleSignIn.getClient(this,gso)
//        // Initialize the firebaseAuth variable
//        firebaseAuth= FirebaseAuth.getInstance()
//
//        val signInButton = findViewById(R.id.sign_in_button) as CardView
//        signInButton.setOnClickListener{ view: View? ->
//            Toast.makeText(this,"Logging In",Toast.LENGTH_SHORT).show()
//            signInGoogle()
//        }
//    }
//
//    private  fun signInGoogle(){
//        val signInIntent: Intent = mGoogleSignInClient.signInIntent
//        startActivityForResult(signInIntent,Req_Code)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if(requestCode==Req_Code){
//            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
//            handleResult(task)
//        }
//    }
//
//    private fun handleResult(completedTask: Task<GoogleSignInAccount>){
//        try {
//            val account: GoogleSignInAccount? =completedTask.getResult(ApiException::class.java)
//            if (account != null) {
//                UpdateUI(account)
//            }
//        } catch (e: ApiException){
//            Toast.makeText(this,e.toString(), Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun UpdateUI(account: GoogleSignInAccount){
//        val credential= GoogleAuthProvider.getCredential(account.idToken,null)
//        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {task->
//            if(task.isSuccessful) {
//                SavedPreferences.setEmail(this,account.email.toString())
//                SavedPreferences.setUsername(this,account.displayName.toString())
//                val intent = Intent(this, StartCall::class.java)
//                startActivity(intent)
//                finish()
//            }
//        }
//    }
//
//    override fun onStart() {
//        super.onStart()
//        if(GoogleSignIn.getLastSignedInAccount(this)!=null){
//            startActivity(Intent(this, StartCall::class.java))
//            finish()
//        }
//    }