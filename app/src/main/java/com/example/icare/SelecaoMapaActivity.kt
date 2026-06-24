package com.example.icare

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

class SelecaoMapaActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private var pontoSelecionado: GeoPoint? = null
    private var marker: Marker? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_selecao_mapa)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        map = findViewById(R.id.mapViewSelecao)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(17.0)
        
        // Ponto padrão (São Paulo) caso a localização falhe
        val defaultPoint = GeoPoint(-23.5505, -46.6333)
        map.controller.setCenter(defaultPoint)

        centralizarNoLocalAtual()

        // Adiciona um overlay para capturar o toque no mapa
        val overlay = object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val projection = mapView.projection
                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                selecionarPonto(geoPoint)
                return true
            }
        }
        map.overlays.add(overlay)

        findViewById<android.view.View>(R.id.botaoVoltarSelecaoMapa).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(R.id.botaoConfirmarPonto).setOnClickListener {
            if (pontoSelecionado != null) {
                val resultado = Intent()
                    .putExtra("lat", pontoSelecionado!!.latitude)
                    .putExtra("lon", pontoSelecionado!!.longitude)
                setResult(Activity.RESULT_OK, resultado)
                finish()
            } else {
                Toast.makeText(this, "Toque no mapa para selecionar um ponto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun centralizarNoLocalAtual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val ponto = GeoPoint(location.latitude, location.longitude)
                    map.controller.setCenter(ponto)
                    selecionarPonto(ponto) // Já seleciona o local atual por padrão
                }
            }
        }
    }

    private fun selecionarPonto(ponto: GeoPoint) {
        pontoSelecionado = ponto
        
        if (marker == null) {
            marker = Marker(map)
            marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(marker)
        }
        
        marker?.position = ponto
        marker?.title = "Ponto selecionado"
        map.invalidate()
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
