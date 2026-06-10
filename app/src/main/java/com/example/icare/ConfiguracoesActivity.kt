package com.example.icare

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ConfiguracoesActivity : AppCompatActivity() {

    private val selecionarPontoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lon = result.data?.getDoubleExtra("lon", 0.0) ?: 0.0
            Toast.makeText(this, "Ponto selecionado: $lat, $lon", Toast.LENGTH_LONG).show()
        }
    }

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
            val intent = Intent(this, SelecaoMapaActivity::class.java)
            selecionarPontoLauncher.launch(intent)
        }

        findViewById<android.view.View>(R.id.botaoSalvarConfiguracoes).setOnClickListener {
            Toast.makeText(this, "Configurações salvas.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
