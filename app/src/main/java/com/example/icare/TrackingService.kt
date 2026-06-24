package com.example.icare

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var lastHistoryUpdateTime = 0L
    private var alertaCercaAtivo = false
    private var cercaConfig: Map<String, Any>? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null

    companion object {
        private const val CHANNEL_ID = "TrackingServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
        iniciarOuvinteCerca()
        
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocationInFirestore(location)
                    processarCercaEletronica(location)
                }
            }
        }
    }

    private fun iniciarOuvinteCerca() {
        val userId = auth.currentUser?.uid ?: return
        userListener = db.collection("usuarios").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    cercaConfig = snapshot.get("cercaEletronica") as? Map<String, Any>
                }
            }
    }

    private fun processarCercaEletronica(location: Location) {
        val cerca = cercaConfig ?: return
        if (cerca["ativa"] == true) {
            val centroLat = (cerca["latitude"] as? Number)?.toDouble() ?: return
            val centroLon = (cerca["longitude"] as? Number)?.toDouble() ?: return
            val raio = (cerca["raio"] as? Number)?.toDouble() ?: return
            
            val resultados = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, centroLat, centroLon, resultados)
            val distancia = resultados[0]

            if (distancia > raio) {
                if (!alertaCercaAtivo) {
                    enviarAlertaCerca("Saiu da área segura: ${cerca["nome"]}")
                    alertaCercaAtivo = true
                }
            } else {
                alertaCercaAtivo = false
            }
        }
    }

    private fun enviarAlertaCerca(mensagem: String) {
        val userId = auth.currentUser?.uid ?: return
        val alertaId = "CERCA_" + System.currentTimeMillis()
        
        val alertaData = hashMapOf(
            "uid" to userId,
            "tipo" to "CERCA",
            "mensagem" to mensagem,
            "timestamp" to System.currentTimeMillis(),
            "status" to "ativo"
        )

        db.collection("alertas").document(alertaId).set(alertaData)
        
        // Atualiza o status e a mensagem do último alerta para o responsável ver
        db.collection("usuarios").document(userId).update(
            "statusSos", "ativo",
            "ultimoAlertaMensagem", mensagem
        )
        
        Log.w("TrackingService", "ALERTA DE CERCA: $mensagem")
    }

    private fun updateLocationInFirestore(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        
        val updates = hashMapOf<String, Any>(
            "localizacao" to GeoPoint(location.latitude, location.longitude),
            "gpsAtivo" to true,
            "ultimaAtualizacao" to System.currentTimeMillis()
        )

        db.collection("usuarios").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("TrackingService", "Localização atualizada: ${location.latitude}, ${location.longitude}")
                salvarNoHistorico(userId, location)
            }
            .addOnFailureListener { e ->
                Log.e("TrackingService", "Erro ao atualizar localização", e)
            }
    }

    private fun salvarNoHistorico(userId: String, location: Location) {
        val currentTime = System.currentTimeMillis()
        // Salva no histórico apenas a cada 2 minutos para economizar Firestore
        if (currentTime - lastHistoryUpdateTime < 120000) return

        val historicoData = hashMapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to currentTime,
            "geopoint" to GeoPoint(location.latitude, location.longitude)
        )

        db.collection("usuarios").document(userId)
            .collection("historico_posicoes")
            .add(historicoData)
            .addOnSuccessListener {
                lastHistoryUpdateTime = currentTime
                Log.d("TrackingService", "Ponto salvo no histórico")
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        startLocationUpdates()
        
        return START_STICKY
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e("TrackingService", "Lost location permission. Could not request updates. $unlikely")
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ICare Ativo")
            .setContentText("Sua localização está sendo protegida em tempo real.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "ICare Tracking Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        userListener?.remove()
    }
}
