package com.example.icare

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SelecaoMapaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_selecao_mapa)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.botaoVoltarSelecaoMapa).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.botaoConfirmarPonto).setOnClickListener {
            val resultado = Intent()
                .putExtra("lat", -23.5505)
                .putExtra("lon", -46.6333)
            setResult(Activity.RESULT_OK, resultado)
            finish()
        }
    }
}
