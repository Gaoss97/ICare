package com.example.icare

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HistoricoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historico)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarFiltros()

        findViewById<View>(R.id.botaoVoltarHistorico).setOnClickListener {
            finish()
        }

        val mensagemMapa = "Visualizacao da rota no mapa sera adicionada depois."
        findViewById<View>(R.id.botaoVerMapaHoje).setOnClickListener {
            Toast.makeText(this, mensagemMapa, Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.botaoVerMapaOntem).setOnClickListener {
            Toast.makeText(this, mensagemMapa, Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.botaoVerMapaAnterior).setOnClickListener {
            Toast.makeText(this, mensagemMapa, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarFiltros() {
        findViewById<Spinner>(R.id.spinnerUsuarioHistorico).adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Ana Clara", "Joao Pedro", "Marina Lima")
        )

        val tipoHistorico = findViewById<Spinner>(R.id.spinnerTipoHistorico)
        tipoHistorico.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Rotas", "Alertas")
        )
        tipoHistorico.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                alternarTipoHistorico(position == 1)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        findViewById<Spinner>(R.id.spinnerFiltroHistorico).adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Ultimos registros", "Hoje", "Ultimos 7 dias", "Ultimos 30 dias")
        )
    }

    private fun alternarTipoHistorico(mostrarAlertas: Boolean) {
        findViewById<View>(R.id.containerRotas).visibility = if (mostrarAlertas) View.GONE else View.VISIBLE
        findViewById<View>(R.id.containerAlertas).visibility = if (mostrarAlertas) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.subtituloHistorico).text = if (mostrarAlertas) {
            "Alertas recebidos para o usuario selecionado."
        } else {
            "Rotas percorridas pelo usuario selecionado."
        }
    }
}
