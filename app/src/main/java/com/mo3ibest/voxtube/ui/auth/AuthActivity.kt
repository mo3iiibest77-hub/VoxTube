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
import com.mo3ibest.voxtube.databinding.ActivityAuthBinding
import com.mo3ibest.voxtube.ui.home.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var googleSignInClient: GoogleSignInClient? = null

    companion object {
        private const val RC_SIGN_IN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            goToMain(
                name = account.displayName,
                email = account.email,
                photo = account.photoUrl?.toString()
            )
            return
        }

        setupGoogleSignIn()
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestScopes(Scope("https://www.googleapis.com/auth/youtube.readonly"))
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            // Google Play Services may be missing; skip still works
            googleSignInClient = null
        }
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            val client = googleSignInClient
            if (client == null) {
                Toast.makeText(this, "گوگل ساین‌این در دسترس نیست — از ورود آزمایشی استفاده کن", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSignIn.isEnabled = false
            startActivityForResult(client.signInIntent, RC_SIGN_IN)
        }

        binding.btnSkip.setOnClickListener {
            goToMain(name = "کاربر تست", email = null, photo = null)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            binding.progressBar.visibility = View.GONE
            binding.btnSignIn.isEnabled = true
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                goToMain(
                    name = account.displayName,
                    email = account.email,
                    photo = account.photoUrl?.toString()
                )
            } catch (e: ApiException) {
                Toast.makeText(this, "خطا در ورود: ${e.statusCode} — از ورود آزمایشی استفاده کن", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goToMain(name: String?, email: String?, photo: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_name", name ?: "کاربر")
        intent.putExtra("user_email", email)
        intent.putExtra("user_photo", photo)
        startActivity(intent)
        finish()
    }
}
