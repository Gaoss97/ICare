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

class CadastroActivity : AppCompatActivity() {
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

        radioTipoUsuario.setOnCheckedChangeListener { _, checkedId ->
            val usuarioRastreado = checkedId == R.id.radioRastreado
            editGrupo.hint = if (usuarioRastreado) {
                "Código do grupo"
            } else {
                "Nome do grupo inicial (opcional)"
            }
            editEmailResponsavel.visibility = if (usuarioRastreado) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        findViewById<View>(R.id.botaoFoto).setOnClickListener {
            Toast.makeText(this, "Seleção de foto será adicionada depois.", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.botaoSalvarCadastro).setOnClickListener {
            Toast.makeText(this, "Cadastro criado.", Toast.LENGTH_SHORT).show()
            finish()
        }

        textoEntrar.setOnClickListener {
            finish()
        }
    }
}
