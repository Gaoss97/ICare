package com.example.icare

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.botaoEditarPerfil).setOnClickListener {
            startActivity(Intent(this, EditarPerfilActivity::class.java))
        }

        findViewById<android.view.View>(R.id.botaoSairPerfil).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        carregarDadosPerfil()
    }

    private fun carregarDadosPerfil() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nome = document.getString("nome") ?: "Sem nome"
                    val email = document.getString("email") ?: "Sem e-mail"
                    val tipo = document.getString("tipo") ?: "rastreado"
                    val grupo = document.getString("grupo") ?: ""
                    val telefone = document.getString("telefone") ?: "Não informado"
                    val emailResponsavel = document.getString("emailResponsavel") ?: ""
                    val statusRastreamento = document.getBoolean("statusRastreamento") ?: true

                    findViewById<TextView>(R.id.textoNomePerfil).text = nome
                    findViewById<TextView>(R.id.textoEmailPerfil).text = email
                    findViewById<TextView>(R.id.textoTipoPerfil).text = if (tipo == "responsavel") "Responsável" else "Usuário Rastreado"
                    findViewById<TextView>(R.id.textoGrupoPerfil).text = if (grupo.isBlank()) "Nenhum grupo" else grupo
                    findViewById<TextView>(R.id.textoTelefonePerfil).text = telefone

                    if (tipo == "rastreado") {
                        findViewById<TextView>(R.id.labelResponsavelPerfil).visibility = android.view.View.VISIBLE
                        val txtResponsavel = findViewById<TextView>(R.id.textoResponsavelPerfil)
                        txtResponsavel.visibility = android.view.View.VISIBLE
                        txtResponsavel.text = if (emailResponsavel.isBlank()) "Não vinculado" else emailResponsavel
                    }

                    val textoStatus = findViewById<TextView>(R.id.textoStatusPerfil)
                    if (statusRastreamento) {
                        textoStatus.text = "Ativo"
                        textoStatus.setTextColor(0xFF15803D.toInt())
                    } else {
                        textoStatus.text = "Inativo"
                        textoStatus.setTextColor(0xFFDC2626.toInt())
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar dados do perfil", Toast.LENGTH_SHORT).show()
            }
    }
}
