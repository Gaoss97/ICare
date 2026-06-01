package com.example.icare

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private val capturarFotoSos = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { foto ->
        if (foto != null) {
            Toast.makeText(this, "Alerta SOS enviado com foto.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Foto cancelada. Alerta SOS nao enviado.", Toast.LENGTH_SHORT).show()
        }
    }

    private val solicitarPermissaoCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissaoConcedida ->
        if (permissaoConcedida) {
            capturarFotoSos.launch(null)
        } else {
            Toast.makeText(this, "Permissao de camera necessaria para enviar foto no SOS.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val usuarios = listOf("Ana Clara", "Joao Pedro", "Marina Lima")
        val spinnerUsuario = findViewById<Spinner>(R.id.spinnerUsuarioRastreado)
        spinnerUsuario.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            usuarios
        )
        spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                atualizarDetalhesUsuario(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        findViewById<android.view.View>(R.id.botaoPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        findViewById<android.view.View>(R.id.botaoConfiguracoes).setOnClickListener {
            startActivity(Intent(this, ConfiguracoesActivity::class.java))
        }

        findViewById<android.view.View>(R.id.botaoHistorico).setOnClickListener {
            startActivity(Intent(this, HistoricoActivity::class.java))
        }

        findViewById<android.view.View>(R.id.botaoPanico).setOnClickListener {
            val opcoesSos = arrayOf("Enviar com foto", "Enviar sem foto", "Cancelar")
            AlertDialog.Builder(this)
                .setTitle("Enviar alerta SOS?")
                .setItems(opcoesSos) { dialog, opcao ->
                    when (opcao) {
                        0 -> iniciarCapturaSos()
                        1 -> Toast.makeText(this, "Alerta SOS enviado sem foto.", Toast.LENGTH_SHORT).show()
                        else -> dialog.dismiss()
                    }
                }
                .show()
        }
    }

    private fun iniciarCapturaSos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            capturarFotoSos.launch(null)
        } else {
            solicitarPermissaoCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun atualizarDetalhesUsuario(position: Int) {
        val status = findViewById<TextView>(R.id.textoStatusUsuario)
        val gps = findViewById<TextView>(R.id.textoGpsUsuario)
        val velocidade = findViewById<TextView>(R.id.textoVelocidadeUsuario)
        val atualizacao = findViewById<TextView>(R.id.textoAtualizacaoUsuario)

        when (position) {
            1 -> {
                status.text = "Atencao"
                status.setTextColor(0xFFB45309.toInt())
                gps.text = "Instavel"
                velocidade.text = "7 km/h"
                atualizacao.text = "14:29"
            }
            2 -> {
                status.text = "Seguro"
                status.setTextColor(0xFF15803D.toInt())
                gps.text = "Sem sinal"
                velocidade.text = "0 km/h"
                atualizacao.text = "14:18"
            }
            else -> {
                status.text = "Seguro"
                status.setTextColor(0xFF15803D.toInt())
                gps.text = "Ativo"
                velocidade.text = "3 km/h"
                atualizacao.text = "14:32"
            }
        }
    }
}
