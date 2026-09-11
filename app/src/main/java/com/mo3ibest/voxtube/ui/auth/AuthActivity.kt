package com.mo3ibest.voxtube.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.mo3ibest.voxtube.R
import com.mo3ibest.voxtube.databinding.ActivityAuthBinding
import com.mo3ibest.voxtube.ui.home.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            goToMain(account)
            return
        }

        setupGoogleSignIn()
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/youtube.readonly"))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSignIn.isEnabled = false
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            binding.progressBar.visibility = View.GONE
            binding.btnSignIn.isEnabled = true
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                goToMain(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "خطا در ورود: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToMain(account: GoogleSignInAccount) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_name", account.displayName)
        intent.putExtra("user_email", account.email)
        intent.putExtra("user_photo", account.photoUrl?.toString())
        startActivity(intent)
        finish()
    }
}
