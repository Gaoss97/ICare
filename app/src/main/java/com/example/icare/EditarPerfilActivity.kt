package com.example.icare

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var tipoUsuario: String = "rastreado"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_editar_perfil)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        carregarDadosAtuais()

        val editTelefone = findViewById<EditText>(R.id.editTelefonePerfil)
        editTelefone.addTextChangedListener(TelefoneMaskWatcher(editTelefone))

        findViewById<View>(R.id.botaoVoltarEditarPerfil).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.botaoSalvarEditarPerfil).setOnClickListener {
            salvarAlteracoes()
        }

        findViewById<View>(R.id.botaoAlterarFoto).setOnClickListener {
            Toast.makeText(this, "Alteração de foto em breve.", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.botaoGerarCodigoGrupo).setOnClickListener {
            Toast.makeText(this, "Funcionalidade de convite em breve.", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.botaoSairGrupo).setOnClickListener {
            Toast.makeText(this, "Saída do grupo em breve.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun carregarDadosAtuais() {
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    tipoUsuario = document.getString("tipo") ?: "rastreado"
                    val emailResponsavel = document.getString("emailResponsavel") ?: ""

                    findViewById<EditText>(R.id.editNomePerfil).setText(document.getString("nome"))
                    findViewById<EditText>(R.id.editEmailPerfil).setText(document.getString("email"))
                    findViewById<EditText>(R.id.editEmailPerfil).isEnabled = false
                    findViewById<EditText>(R.id.editTelefonePerfil).setText(document.getString("telefone"))
                    
                    val editGrupo = findViewById<EditText>(R.id.editNomeGrupoPerfil)
                    editGrupo.setText(document.getString("grupo"))
                    
                    if (tipoUsuario == "rastreado") {
                        editGrupo.isEnabled = false
                        editGrupo.hint = "Grupo definido pelo responsável"
                        val editEmailResp = findViewById<EditText>(R.id.editEmailResponsavelPerfil)
                        editEmailResp.visibility = View.VISIBLE
                        editEmailResp.setText(emailResponsavel)
                    } else {
                        editGrupo.isEnabled = true
                        editGrupo.hint = "Nome do seu grupo"
                    }

                    findViewById<EditText>(R.id.editCodigoGrupoPerfil).setText(uid.take(8).uppercase())
                    findViewById<EditText>(R.id.editCodigoGrupoPerfil).isEnabled = false
                }
            }
    }

    private fun salvarAlteracoes() {
        val uid = auth.currentUser?.uid ?: return
        val novoNome = findViewById<EditText>(R.id.editNomePerfil).text.toString().trim()
        val novoTelefone = findViewById<EditText>(R.id.editTelefonePerfil).text.toString().trim()
        val novoGrupo = findViewById<EditText>(R.id.editNomeGrupoPerfil).text.toString().trim()
        val novoEmailResp = findViewById<EditText>(R.id.editEmailResponsavelPerfil).text.toString().trim()

        if (novoNome.isBlank()) {
            Toast.makeText(this, "O nome não pode estar vazio.", Toast.LENGTH_SHORT).show()
            return
        }

        findViewById<View>(R.id.botaoSalvarEditarPerfil).isEnabled = false

        if (tipoUsuario == "rastreado" && novoEmailResp.isNotBlank()) {
            // Tenta buscar o grupo do responsável para manter sincronizado
            db.collection("usuarios")
                .whereEqualTo("email", novoEmailResp)
                .whereEqualTo("tipo", "responsavel")
                .get()
                .addOnSuccessListener { documents ->
                    var grupoFinal = novoGrupo
                    if (!documents.isEmpty) {
                        val docResponsavel = documents.documents[0]
                        grupoFinal = docResponsavel.getString("grupo") ?: novoGrupo
                    }
                    executarUpdate(uid, novoNome, novoTelefone, grupoFinal, novoEmailResp)
                }
                .addOnFailureListener {
                    executarUpdate(uid, novoNome, novoTelefone, novoGrupo, novoEmailResp)
                }
        } else {
            executarUpdate(uid, novoNome, novoTelefone, novoGrupo, novoEmailResp)
        }
    }

    private fun executarUpdate(uid: String, nome: String, telefone: String, grupo: String, emailResp: String) {
        val atualizacoes = mutableMapOf(
            "nome" to nome,
            "telefone" to telefone,
            "grupo" to grupo
        )
        
        if (tipoUsuario == "rastreado") {
            atualizacoes["emailResponsavel"] = emailResp
        }

        db.collection("usuarios").document(uid).update(atualizacoes as Map<String, Any>)
            .addOnSuccessListener {
                if (tipoUsuario == "responsavel") {
                    // Se o responsável mudou o nome do grupo, atualiza todos os seus rastreados
                    atualizarGrupoDosRastreados(auth.currentUser?.email ?: "", grupo)
                } else {
                    Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                findViewById<View>(R.id.botaoSalvarEditarPerfil).isEnabled = true
                Toast.makeText(this, "Erro ao atualizar perfil.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun atualizarGrupoDosRastreados(emailResp: String, novoGrupo: String) {
        if (emailResp.isBlank()) {
            finish()
            return
        }

        db.collection("usuarios")
            .whereEqualTo("emailResponsavel", emailResp)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Perfil atualizado!", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                for (doc in documents) {
                    batch.update(doc.reference, "grupo", novoGrupo)
                }
                batch.commit().addOnCompleteListener {
                    Toast.makeText(this, "Perfil e grupo dos dependentes atualizados!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                finish()
            }
    }
}
