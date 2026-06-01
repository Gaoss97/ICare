package com.example.icare

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ConfiguracoesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_configuracoes)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.botaoVoltarConfiguracoes).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.botaoSelecionarPonto).setOnClickListener {
            Toast.makeText(this, "Seleção no mapa será adicionada depois.", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.botaoSalvarConfiguracoes).setOnClickListener {
            Toast.makeText(this, "Configurações salvas.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
