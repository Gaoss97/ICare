package com.example.icare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class HistoricoPonto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0,
    val geopoint: Any? = null
)

class HistoricoAdapter(
    private var pontos: List<HistoricoPonto>,
    private val onVerMapaClick: (HistoricoPonto) -> Unit
) : RecyclerView.Adapter<HistoricoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textoData: TextView = view.findViewById(R.id.textoData)
        val textoHorario: TextView = view.findViewById(R.id.textoHorario)
        val textoDetalhes: TextView = view.findViewById(R.id.textoDetalhes)
        val botaoVerNoMapa: Button = view.findViewById(R.id.botaoVerNoMapa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historico, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ponto = pontos[position]
        val sdfData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = Date(ponto.timestamp)

        holder.textoData.text = sdfData.format(date)
        holder.textoHorario.text = "Horário: ${sdfHora.format(date)}"
        holder.textoDetalhes.text = "Lat: ${String.format("%.5f", ponto.latitude)}, Lon: ${String.format("%.5f", ponto.longitude)}"
        
        holder.botaoVerNoMapa.setOnClickListener {
            onVerMapaClick(ponto)
        }
    }

    override fun getItemCount() = pontos.size

    fun atualizarPontos(novosPontos: List<HistoricoPonto>) {
        pontos = novosPontos
        notifyDataSetChanged()
    }
}
