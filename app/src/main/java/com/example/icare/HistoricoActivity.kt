package com.example.icare

import android.os.Bundle
import android.util.Log
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoricoActivity : AppCompatActivity() {

    private lateinit var adapter: HistoricoAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historico)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        configurarFiltros()

        findViewById<View>(R.id.botaoVoltarHistorico).setOnClickListener {
            finish()
        }

        carregarHistorico()
    }

    private fun setupRecyclerView() {
        val recycler = findViewById<RecyclerView>(R.id.recyclerHistorico)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = HistoricoAdapter(emptyList()) { ponto ->
            Toast.makeText(this, "Ver no mapa: ${ponto.latitude}, ${ponto.longitude}", Toast.LENGTH_SHORT).show()
        }
        recycler.adapter = adapter
    }

    private fun carregarHistorico() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(userId)
            .collection("historico_posicoes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("HistoricoActivity", "Erro ao carregar histórico", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val pontos = snapshot.toObjects(HistoricoPonto::class.java)
                    adapter.atualizarPontos(pontos)
                }
            }
    }

    private fun configurarFiltros() {
        findViewById<Spinner>(R.id.spinnerUsuarioHistorico).adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Meu Histórico")
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
                // Futura implementação de alternância
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        findViewById<Spinner>(R.id.spinnerFiltroHistorico).adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Últimos registros")
        )
    }
}
