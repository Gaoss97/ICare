package com.example.icare

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay

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

    private val solicitarPermissoesLocalizacao = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        val concedida = permissoes[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissoes[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedida) {
            ativarLocalizacaoNoMapa()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuração do Osmdroid (importante antes do setContentView)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar MapView (Osmdroid)
        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        
        val mapController = map.controller
        mapController.setZoom(15.0)
        val startPoint = GeoPoint(-23.5505, -46.6333) // São Paulo
        mapController.setCenter(startPoint)

        configurarLocalizacao()
        configurarInterface()
    }

    private fun configurarLocalizacao() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            ativarLocalizacaoNoMapa()
        } else {
            solicitarPermissoesLocalizacao.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun ativarLocalizacaoNoMapa() {
        locationOverlay.enableFollowLocation()
    }

    private fun configurarInterface() {
        val usuarios = listOf("Ana Clara", "Joao Pedro", "Marina Lima")
        val spinnerUsuario = findViewById<Spinner>(R.id.spinnerUsuarioRastreado)
        spinnerUsuario.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            usuarios
        )
        spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
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

        findViewById<FloatingActionButton>(R.id.botaoCentralizar).setOnClickListener {
            val myLocation = locationOverlay.myLocation
            if (myLocation != null) {
                map.controller.animateTo(myLocation)
                map.controller.setZoom(17.0)
            } else {
                Toast.makeText(this, "Localização ainda não disponível", Toast.LENGTH_SHORT).show()
            }
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

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
