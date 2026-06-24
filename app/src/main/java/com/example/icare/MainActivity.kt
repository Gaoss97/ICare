package com.example.icare

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream
import java.util.UUID
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var monitoramentoUsuario: ListenerRegistration? = null
    private var monitoramentoHistorico: ListenerRegistration? = null
    private var listaUsuariosRastreados = mutableListOf<Map<String, Any>>()

    private val capturarFotoSos = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { foto ->
        if (foto != null) {
            enviarAlertaSos(foto)
        } else {
            Toast.makeText(this, "Foto cancelada. Alerta SOS não enviado.", Toast.LENGTH_SHORT).show()
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
        verificarTipoUsuario()
        configurarInterface()
        solicitarPermissaoNotificacao()
        atualizarTokenFCM()
    }

    private fun solicitarPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permissão de notificações negada. Você não receberá alertas SOS.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun atualizarTokenFCM() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
            db.collection("usuarios").document(uid).update("fcmToken", token)
        }
    }

    private fun verificarTipoUsuario() {
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val tipo = document.getString("tipo") ?: "rastreado"
                    ajustarInterfacePorTipo(tipo)
                    
                    if (tipo == "rastreado") {
                        iniciarAtualizacaoLocalizacaoRastreado()
                        escutarPropriosDados(uid)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar dados do usuário", Toast.LENGTH_SHORT).show()
            }
    }

    private fun ajustarInterfacePorTipo(tipo: String) {
        val spinnerUsuario = findViewById<Spinner>(R.id.spinnerUsuarioRastreado)
        val textoSelecione = findViewById<TextView>(R.id.textoSelecioneUsuario)
        val botaoPanico = findViewById<View>(R.id.botaoPanico)

        if (tipo == "rastreado") {
            spinnerUsuario.visibility = View.GONE
            textoSelecione.text = "Meu Status"
            botaoPanico.visibility = View.VISIBLE
        } else {
            spinnerUsuario.visibility = View.VISIBLE
            textoSelecione.text = "Usuário rastreado"
            botaoPanico.visibility = View.GONE
            carregarUsuariosVinculados()
        }
    }

    private fun carregarUsuariosVinculados() {
        val emailResponsavel = auth.currentUser?.email ?: return
        
        db.collection("usuarios")
            .whereEqualTo("emailResponsavel", emailResponsavel)
            .addSnapshotListener { documents, _ ->
                if (documents != null) {
                    listaUsuariosRastreados.clear()
                    val nomes = mutableListOf<String>()
                    for (doc in documents) {
                        val data = doc.data.toMutableMap()
                        data["uid"] = doc.id
                        listaUsuariosRastreados.add(data)
                        nomes.add(doc.getString("nome") ?: "Sem nome")
                    }
                    
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nomes)
                    findViewById<Spinner>(R.id.spinnerUsuarioRastreado).adapter = adapter
                }
            }
    }

    private fun iniciarAtualizacaoLocalizacaoRastreado() {
        val intent = Intent(this, TrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun atualizarLocalizacaoNoFirestore(lat: Double, lon: Double) {
        val uid = auth.currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>(
            "localizacao" to FirestoreGeoPoint(lat, lon),
            "gpsAtivo" to true,
            "ultimaAtualizacao" to System.currentTimeMillis()
        )
        db.collection("usuarios").document(uid).update(updates)
            .addOnFailureListener {
                // Se o documento não tiver o campo 'localizacao' ainda, o update funciona,
                // mas se o documento não existir, falharia. Aqui tratamos apenas o erro de rede/permissão.
            }
    }

    private fun escutarPropriosDados(uid: String) {
        monitoramentoUsuario?.remove()
        monitoramentoUsuario = db.collection("usuarios").document(uid)
            .addSnapshotListener { doc, _ ->
                doc?.let {
                    exibirDadosUsuario(it.data)
                    val cerca = it.get("cercaEletronica") as? Map<String, Any>
                    desenharCercaNoMapa(cerca)
                }
            }
    }

    private fun monitorarUsuarioSelecionado(uid: String) {
        monitoramentoUsuario?.remove()
        monitoramentoUsuario = db.collection("usuarios").document(uid)
            .addSnapshotListener { doc, _ ->
                doc?.let {
                    val data = it.data
                    exibirDadosUsuario(data)
                    
                    val geo = it.getGeoPoint("localizacao")
                    if (geo != null) {
                        val ponto = GeoPoint(geo.latitude, geo.longitude)
                        map.controller.animateTo(ponto)
                    }

                    // Desenha a cerca eletrônica se houver uma ativa
                    val cerca = it.get("cercaEletronica") as? Map<String, Any>
                    desenharCercaNoMapa(cerca)
                }
            }
        monitorarHistoricoUsuario(uid)
    }

    private fun desenharCercaNoMapa(cerca: Map<String, Any>?) {
        // Remover polígonos anteriores (usados para a cerca)
        map.overlays.removeAll { it is org.osmdroid.views.overlay.Polygon }

        if (cerca == null || cerca["ativa"] == false) {
            map.invalidate()
            return
        }

        val lat = (cerca["latitude"] as? Number)?.toDouble() ?: return
        val lon = (cerca["longitude"] as? Number)?.toDouble() ?: return
        val raio = (cerca["raio"] as? Number)?.toDouble() ?: return

        val circulo = org.osmdroid.views.overlay.Polygon()
        circulo.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(GeoPoint(lat, lon), raio)
        circulo.fillPaint.color = 0x33FF0000 // Vermelho transparente
        circulo.outlinePaint.color = 0xFFFF0000.toInt() // Borda vermelha
        circulo.outlinePaint.strokeWidth = 2f
        
        map.overlays.add(circulo)
        map.invalidate()
    }

    private fun monitorarHistoricoUsuario(uid: String) {
        monitoramentoHistorico?.remove()
        // Desenha a rota recente no mapa
        monitoramentoHistorico = db.collection("usuarios").document(uid)
            .collection("historico_posicoes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val pontos = mutableListOf<GeoPoint>()
                    for (doc in snapshots.documents) {
                        val lat = doc.getDouble("latitude") ?: 0.0
                        val lon = doc.getDouble("longitude") ?: 0.0
                        pontos.add(GeoPoint(lat, lon))
                    }
                    desenharRotaNoMapa(pontos)
                }
            }
    }

    private fun desenharRotaNoMapa(pontos: List<GeoPoint>) {
        // Remover polylines anteriores se existirem
        map.overlays.removeAll { it is org.osmdroid.views.overlay.Polyline }
        
        if (pontos.size < 2) return

        val polyline = org.osmdroid.views.overlay.Polyline()
        polyline.setPoints(pontos)
        polyline.outlinePaint.color = 0xFF3B82F6.toInt() // Cor azul
        polyline.outlinePaint.strokeWidth = 10f
        map.overlays.add(polyline)
        map.invalidate()
    }

    private fun exibirDadosUsuario(data: Map<String, Any>?) {
        if (data == null) return
        
        val status = findViewById<TextView>(R.id.textoStatusUsuario)
        val gps = findViewById<TextView>(R.id.textoGpsUsuario)
        val velocidade = findViewById<TextView>(R.id.textoVelocidadeUsuario)
        val atualizacao = findViewById<TextView>(R.id.textoAtualizacaoUsuario)
        val cardStatus = findViewById<androidx.cardview.widget.CardView>(R.id.cardStatus)
        val imageSos = findViewById<android.widget.ImageView>(R.id.imageSos)
        val botaoResolverSos = findViewById<android.widget.Button>(R.id.botaoResolverSos)

        val statusSos = data["statusSos"] as? String ?: "inativo"
        val uidUsuario = data["uid"] as? String ?: ""
        
        if (statusSos == "ativo") {
            val mensagemAlerta = data["ultimoAlertaMensagem"] as? String ?: "EM EMERGÊNCIA!"
            status.text = mensagemAlerta
            status.setTextColor(0xFFFFFFFF.toInt())
            cardStatus.setCardBackgroundColor(0xFFDC2626.toInt())
            
            val fotoUrl = data["fotoUrl"] as? String
            if (!fotoUrl.isNullOrEmpty()) {
                imageSos.visibility = android.view.View.VISIBLE
                Glide.with(this).load(fotoUrl).into(imageSos)
            } else {
                imageSos.visibility = android.view.View.GONE
            }

            botaoResolverSos.visibility = android.view.View.VISIBLE
            botaoResolverSos.setOnClickListener {
                resolverSos(uidUsuario)
            }
        } else {
            val isAtivo = data["statusRastreamento"] as? Boolean ?: true
            status.text = if (isAtivo) "Seguro" else "Inativo"
            status.setTextColor(if (isAtivo) 0xFF15803D.toInt() else 0xFFDC2626.toInt())
            cardStatus.setCardBackgroundColor(0xFFFFFFFF.toInt())
            imageSos.visibility = android.view.View.GONE
            botaoResolverSos.visibility = android.view.View.GONE
        }
        
        gps.text = if (data["gpsAtivo"] as? Boolean == true) "Ativo" else "Sem sinal"
        velocidade.text = "${data["velocidade"] ?: 0} km/h"
        
        val timestamp = data["ultimaAtualizacao"] as? Long ?: data["criadoEm"] as? Long ?: 0
        if (timestamp > 0) {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            atualizacao.text = sdf.format(java.util.Date(timestamp))
        }
    }

    private fun resolverSos(uidUsuario: String) {
        val updates = hashMapOf<String, Any>(
            "statusSos" to "inativo",
            "ultimoAlertaMensagem" to ""
        )
        db.collection("usuarios").document(uidUsuario).update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Status de segurança restabelecido", Toast.LENGTH_SHORT).show()
            }
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
        val spinnerUsuario = findViewById<Spinner>(R.id.spinnerUsuarioRastreado)
        spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (listaUsuariosRastreados.isNotEmpty() && position < listaUsuariosRastreados.size) {
                    val uid = listaUsuariosRastreados[position]["uid"] as String
                    monitorarUsuarioSelecionado(uid)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        findViewById<View>(R.id.botaoPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        findViewById<View>(R.id.botaoConfiguracoes).setOnClickListener {
            val intent = Intent(this, ConfiguracoesActivity::class.java)
            // Se houver um usuário selecionado no spinner, passamos o UID dele
            val spinnerUsuario = findViewById<Spinner>(R.id.spinnerUsuarioRastreado)
            if (spinnerUsuario.visibility == View.VISIBLE && listaUsuariosRastreados.isNotEmpty()) {
                val position = spinnerUsuario.selectedItemPosition
                if (position >= 0 && position < listaUsuariosRastreados.size) {
                    val uid = listaUsuariosRastreados[position]["uid"] as? String
                    intent.putExtra("targetUserId", uid)
                }
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.botaoHistorico).setOnClickListener {
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

        findViewById<View>(R.id.botaoPanico).setOnClickListener {
            val opcoesSos = arrayOf("Enviar com foto", "Enviar sem foto", "Cancelar")
            AlertDialog.Builder(this)
                .setTitle("Enviar alerta SOS?")
                .setItems(opcoesSos) { dialog, opcao ->
                    when (opcao) {
                        0 -> iniciarCapturaSos()
                        1 -> enviarAlertaSos(null)
                        else -> dialog.dismiss()
                    }
                }
                .show()
        }
    }

    private fun enviarAlertaSos(foto: android.graphics.Bitmap?) {
        val uid = auth.currentUser?.uid ?: return
        val localizacao = locationOverlay.myLocation
        if (localizacao == null) {
            Toast.makeText(this, "Localização não disponível para SOS", Toast.LENGTH_SHORT).show()
            return
        }

        val alertaId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        val alertaData = mutableMapOf<String, Any>(
            "uid" to uid,
            "latitude" to localizacao.latitude,
            "longitude" to localizacao.longitude,
            "timestamp" to timestamp,
            "status" to "ativo"
        )

        if (foto != null) {
            val baos = ByteArrayOutputStream()
            foto.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos)
            val data = baos.toByteArray()

            val storageRef = FirebaseStorage.getInstance().reference.child("sos_fotos/$alertaId.jpg")
            
            // Usamos o uploadTask direto para ter mais controle
            storageRef.putBytes(data)
                .addOnSuccessListener { taskSnapshot ->
                    // Após o sucesso do upload, solicitamos a URL
                    taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                        alertaData["fotoUrl"] = uri.toString()
                        salvarAlertaNoFirestore(alertaId, alertaData)
                    }?.addOnFailureListener { e ->
                        android.util.Log.e("SOS_ERROR", "Erro ao obter URL: ${e.message}")
                        salvarAlertaNoFirestore(alertaId, alertaData)
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("SOS_ERROR", "Erro no upload da imagem: ${e.message}")
                    Toast.makeText(this, "Falha ao subir foto, enviando apenas localização", Toast.LENGTH_SHORT).show()
                    salvarAlertaNoFirestore(alertaId, alertaData)
                }
        } else {
            salvarAlertaNoFirestore(alertaId, alertaData)
        }
    }

    private fun salvarAlertaNoFirestore(alertaId: String, data: Map<String, Any>) {
        db.collection("alertas").document(alertaId).set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "ALERTA SOS ENVIADO!", Toast.LENGTH_LONG).show()
                // Também atualiza o status do usuário para 'em_alerta'
                auth.currentUser?.uid?.let { uid ->
                    db.collection("usuarios").document(uid).update("statusSos", "ativo")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao registrar SOS no banco de dados", Toast.LENGTH_SHORT).show()
            }
    }

    private fun iniciarCapturaSos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            capturarFotoSos.launch(null)
        } else {
            solicitarPermissaoCamera.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        // Ao voltar ao app, garantimos que a localização no mapa esteja ativa
        if (locationOverlay.isMyLocationEnabled) {
            // Se necessário, reativar algo específico da UI
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
