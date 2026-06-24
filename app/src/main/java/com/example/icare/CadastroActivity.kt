package com.example.icare

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CadastroActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastro)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val radioTipoUsuario = findViewById<RadioGroup>(R.id.radioTipoUsuario)
        val editGrupo = findViewById<EditText>(R.id.editGrupo)
        val editEmailResponsavel = findViewById<EditText>(R.id.editEmailResponsavel)
        val textoEntrar = findViewById<TextView>(R.id.textoEntrar)

        val editTelefone = findViewById<EditText>(R.id.editTelefone)
        editTelefone.addTextChangedListener(TelefoneMaskWatcher(editTelefone))

        radioTipoUsuario.setOnCheckedChangeListener { _, checkedId ->
            val usuarioRastreado = checkedId == R.id.radioRastreado
            if (usuarioRastreado) {
                editGrupo.visibility = View.GONE
                editEmailResponsavel.visibility = View.VISIBLE
            } else {
                editGrupo.visibility = View.VISIBLE
                editGrupo.hint = "Nome do seu grupo (ex: Família Silva)"
                editEmailResponsavel.visibility = View.GONE
            }
        }

        findViewById<View>(R.id.botaoFoto).setOnClickListener {
            Toast.makeText(this, "Seleção de foto será adicionada depois.", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.botaoSalvarCadastro).setOnClickListener {
            criarCadastro()
        }

        textoEntrar.setOnClickListener {
            finish()
        }
    }

    private fun criarCadastro() {
        val nome = findViewById<EditText>(R.id.editNome).text.toString().trim()
        val email = findViewById<EditText>(R.id.editEmail).text.toString().trim()
        val senha = findViewById<EditText>(R.id.editSenha).text.toString()
        val confirmarSenha = findViewById<EditText>(R.id.editConfirmarSenha).text.toString()
        val telefone = findViewById<EditText>(R.id.editTelefone).text.toString().trim()
        val grupoDigitado = findViewById<EditText>(R.id.editGrupo).text.toString().trim()
        val emailResponsavel = findViewById<EditText>(R.id.editEmailResponsavel).text.toString().trim()
        val tipo = if (findViewById<RadioGroup>(R.id.radioTipoUsuario).checkedRadioButtonId == R.id.radioRastreado) {
            "rastreado"
        } else {
            "responsavel"
        }

        if (nome.isBlank() || email.isBlank() || senha.isBlank()) {
            Toast.makeText(this, "Preencha nome, e-mail e senha.", Toast.LENGTH_SHORT).show()
            return
        }

        if (senha != confirmarSenha) {
            Toast.makeText(this, "As senhas não conferem.", Toast.LENGTH_SHORT).show()
            return
        }

        findViewById<View>(R.id.botaoSalvarCadastro).isEnabled = false

        // Se for rastreado e informou email do responsável, vamos tentar buscar o grupo desse responsável primeiro
        if (tipo == "rastreado" && emailResponsavel.isNotBlank()) {
            db.collection("usuarios")
                .whereEqualTo("email", emailResponsavel)
                .whereEqualTo("tipo", "responsavel")
                .get()
                .addOnSuccessListener { documents ->
                    var grupoFinal = grupoDigitado
                    if (!documents.isEmpty) {
                        // Encontrou o responsável, pega o grupo dele
                        val docResponsavel = documents.documents[0]
                        grupoFinal = docResponsavel.getString("grupo") ?: grupoDigitado
                    }
                    prosseguirComCriacaoAuth(nome, email, senha, telefone, tipo, grupoFinal, emailResponsavel)
                }
                .addOnFailureListener {
                    prosseguirComCriacaoAuth(nome, email, senha, telefone, tipo, grupoDigitado, emailResponsavel)
                }
        } else {
            prosseguirComCriacaoAuth(nome, email, senha, telefone, tipo, grupoDigitado, emailResponsavel)
        }
    }

    private fun prosseguirComCriacaoAuth(
        nome: String,
        email: String,
        senha: String,
        telefone: String,
        tipo: String,
        grupo: String,
        emailResponsavel: String
    ) {
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid
                if (uid == null) {
                    findViewById<View>(R.id.botaoSalvarCadastro).isEnabled = true
                    Toast.makeText(this, "Não foi possível criar o usuário.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                salvarUsuario(uid, nome, email, telefone, tipo, grupo, emailResponsavel)
            }
            .addOnFailureListener { erro ->
                findViewById<View>(R.id.botaoSalvarCadastro).isEnabled = true
                Toast.makeText(this, "Erro no cadastro: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun salvarUsuario(
        uid: String,
        nome: String,
        email: String,
        telefone: String,
        tipo: String,
        grupo: String,
        emailResponsavel: String
    ) {
        val usuario = hashMapOf(
            "uid" to uid,
            "nome" to nome,
            "email" to email,
            "telefone" to telefone,
            "tipo" to tipo,
            "grupo" to grupo,
            "emailResponsavel" to emailResponsavel,
            "statusRastreamento" to true,
            "gpsAtivo" to false,
            "velocidade" to 0,
            "criadoEm" to System.currentTimeMillis()
        )

        db.collection("usuarios")
            .document(uid)
            .set(usuario)
            .addOnSuccessListener {
                if (tipo == "responsavel") {
                    atualizarRastreadosPendentes(email, grupo)
                } else {
                    Toast.makeText(this, "Cadastro criado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { erro ->
                findViewById<View>(R.id.botaoSalvarCadastro).isEnabled = true
                Toast.makeText(this, "Erro ao salvar dados: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarRastreadosPendentes(emailResp: String, grupo: String) {
        db.collection("usuarios")
            .whereEqualTo("emailResponsavel", emailResp)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Cadastro criado.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                for (doc in documents) {
                    batch.update(doc.reference, "grupo", grupo)
                }
                batch.commit().addOnCompleteListener {
                    Toast.makeText(this, "Cadastro concluído e dependentes vinculados!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                finish()
            }
    }
}
