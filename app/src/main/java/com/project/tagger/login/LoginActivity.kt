package com.project.tagger.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.tagger.R
import com.project.tagger.main.MainActivity
import com.project.tagger.util.tag
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_login.*
import org.koin.android.ext.android.inject


class LoginActivity : AppCompatActivity() {
    val updateUserUC: UpdateUserUC by inject()
    val getUserUC: GetUserUC by inject()

    companion object {
        const val RC_SIGN_IN = 1000
    }

    val auth = FirebaseAuth.getInstance()

    lateinit var gso: GoogleSignInOptions
    lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getUserUC.execute()
            .subscribeBy(onSuccess = {
                startMainActivity()
            }, onComplete = {
                handleOnCreate()
            }, onError = {
                handleOnCreate()
            })


    }

    private fun handleOnCreate() {
        setContentView(R.layout.activity_login)
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        mTvSignIn.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(tag(), "Google sign in failed", e)
                // ...
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(tag(), "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(tag(), "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(tag(), "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT)
                        .show()
                    updateUI(null)
                }

                // ...
            }
    }

    fun updateUI(user: FirebaseUser?) {
        if (user == null) return
        if (user.email == null) return

        updateUserUC.execute(
            UserEntity(
                name = user.displayName,
                email = user.email!!,
                profileUrl = user.photoUrl?.toString()
            )
        ).subscribeBy(onSuccess = {
            startMainActivity()
        }, onError = {
            Toast.makeText(this, "Login fail ${it.message}", Toast.LENGTH_LONG).show()
        })
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}
