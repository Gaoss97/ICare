package com.example.icare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.button).setOnClickListener {
            fazerLogin()
        }

        findViewById<View>(R.id.buttonCriarCadastro).setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }

    private fun fazerLogin() {
        val email = findViewById<EditText>(R.id.editTextText).text.toString().trim()
        val senha = findViewById<EditText>(R.id.editTextTextPassword).text.toString()

        if (email.isBlank() || senha.isBlank()) {
            Toast.makeText(this, "Preencha e-mail e senha.", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro no login: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }
}
