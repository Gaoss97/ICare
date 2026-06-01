package com.example.icare

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EditarPerfilActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_editar_perfil)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.botaoVoltarEditarPerfil).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.botaoAlterarFoto).setOnClickListener {
            Toast.makeText(this, "Alteração de foto será adicionada depois.", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.botaoGerarCodigoGrupo).setOnClickListener {
            findViewById<EditText>(R.id.editCodigoGrupoPerfil).setText("ICARE-5821")
            Toast.makeText(this, "Código de convite gerado.", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.botaoSairGrupo).setOnClickListener {
            Toast.makeText(this, "Saída do grupo será adicionada depois.", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.botaoSalvarEditarPerfil).setOnClickListener {
            Toast.makeText(this, "Perfil atualizado.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
