package com.example.icare

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConfiguracoesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var latSelecionada: Double = 0.0
    private var lonSelecionada: Double = 0.0
    private var targetUserId: String? = null

    private val selecionarPontoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            latSelecionada = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            lonSelecionada = result.data?.getDoubleExtra("lon", 0.0) ?: 0.0
            Toast.makeText(this, "Ponto selecionado: $latSelecionada, $lonSelecionada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_configuracoes)

        // Recebe o UID do usuário que será configurado (ou usa o próprio se não for passado)
        targetUserId = intent.getStringExtra("targetUserId") ?: auth.currentUser?.uid

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.botaoVoltarConfiguracoes).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.botaoSelecionarPonto).setOnClickListener {
            val intent = Intent(this, SelecaoMapaActivity::class.java)
            selecionarPontoLauncher.launch(intent)
        }

        findViewById<android.view.View>(R.id.botaoSalvarConfiguracoes).setOnClickListener {
            salvarConfiguracoes()
        }

        carregarConfiguracoesAtuais()
    }

    private fun carregarConfiguracoesAtuais() {
        val userId = targetUserId ?: return
        db.collection("usuarios").document(userId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val cerca = doc.get("cercaEletronica") as? Map<String, Any>
                if (cerca != null) {
                    findViewById<EditText>(R.id.editNomeArea).setText(cerca["nome"] as? String)
                    findViewById<EditText>(R.id.editRaioArea).setText(cerca["raio"]?.toString())
                    findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchCercaAtiva).isChecked = cerca["ativa"] as? Boolean ?: false
                    latSelecionada = (cerca["latitude"] as? Number)?.toDouble() ?: 0.0
                    lonSelecionada = (cerca["longitude"] as? Number)?.toDouble() ?: 0.0
                }
            }
        }
    }

    private fun salvarConfiguracoes() {
        val userId = targetUserId ?: return
        val nome = findViewById<EditText>(R.id.editNomeArea).text.toString()
        val raioText = findViewById<EditText>(R.id.editRaioArea).text.toString()
        val raio = raioText.toDoubleOrNull() ?: 0.0
        val ativa = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchCercaAtiva).isChecked

        if (nome.isEmpty() || raio <= 0 || latSelecionada == 0.0) {
            Toast.makeText(this, "Preencha o nome, raio e selecione um ponto no mapa.", Toast.LENGTH_SHORT).show()
            return
        }

        val cercaData = hashMapOf(
            "nome" to nome,
            "raio" to raio,
            "latitude" to latSelecionada,
            "longitude" to lonSelecionada,
            "ativa" to ativa
        )

        db.collection("usuarios").document(userId).update("cercaEletronica", cercaData)
            .addOnSuccessListener {
                Toast.makeText(this, "Configurações de cerca salvas!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar configurações", Toast.LENGTH_SHORT).show()
            }
    }
}
