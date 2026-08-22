package com.catedra.eureka.ui.reportes

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.catedra.eureka.R
import com.catedra.eureka.data.model.EstadoReporte
import com.catedra.eureka.data.model.Reporte
import com.catedra.eureka.databinding.ItemTarjetaReporteBinding
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.toColorInt

//ES PARA ADAPTAR EL CONTENIDO DE LAS CARDS DEPENDIENDO SI ES ENCONTRADO O PERDIDO

class ReporteAdapter(
    private val onReporteClick: (Reporte) -> Unit) : ListAdapter<Reporte, ReporteAdapter.ReporteViewHolder>(ReporteDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReporteViewHolder {
        val binding = ItemTarjetaReporteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReporteViewHolder(binding, onReporteClick)
    }

    override fun onBindViewHolder(holder: ReporteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReporteViewHolder(
        private val binding: ItemTarjetaReporteBinding,
        private val onReporteClick: (Reporte) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val formateadorFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun bind(reporte: Reporte) = with(binding) {

            root.setOnClickListener { onReporteClick(reporte) }

            textFecha.text = formateadorFecha.format(reporte.fechaCreacion.toDate())
            if (reporte.telefono > 0L) {textTelefono.text = "📞 ${reporte.telefono}"}

            val backgroundPuntito = indicadorEstado.background as? GradientDrawable ?: GradientDrawable().apply {
                shape = GradientDrawable.OVAL
            }

            if (reporte.estado == EstadoReporte.PERDIDO.name) {
                
                // Cambio de idioma acá porque no está en el xml
                textSubtitulo.text = when (reporte.estado) {
                    "PERDIDO" -> textSubtitulo.context.getString(R.string.tarjeta_perdido)
                    "ENCONTRADO" -> textSubtitulo.context.getString(R.string.tarjeta_encontrado)
                    else -> reporte.estado
                }

                val colorPerdido = ContextCompat.getColor(root.context, R.color.mascota_perdida)
                backgroundPuntito.setColor(colorPerdido)

                textTitulo.text = reporte.nombre.ifBlank { "${reporte.animal}"}
                textDescripcion.text = reporte.descripcion.ifBlank { "Sin descripción" }

            } else {
                textSubtitulo.text =  textSubtitulo.context.getString(R.string.tarjeta_encontrado)
                val colorEncontrado = ContextCompat.getColor(root.context, R.color.mascota_encontrada)
                backgroundPuntito.setColor(colorEncontrado)
                textTitulo.text = reporte.nombre.ifBlank { "${reporte.animal}"}

                textDescripcion.text = reporte.descripcion
            }

            indicadorEstado.background = backgroundPuntito

            if (reporte.fotoUrl.isNotBlank()) {
                Glide.with(root.context)
                    .load(reporte.fotoUrl)
                    .centerCrop()
                    .into(imgMascota)
            } else {
                imgMascota.setImageResource(R.drawable.ilustracion2)
            }
        }
    }
        object ReporteDiffCallback : DiffUtil.ItemCallback<Reporte>() {
        override fun areItemsTheSame(oldItem: Reporte, newItem: Reporte): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Reporte, newItem: Reporte): Boolean {
            return oldItem == newItem
        }
    }
}