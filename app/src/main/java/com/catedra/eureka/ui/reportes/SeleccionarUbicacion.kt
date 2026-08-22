package com.catedra.eureka.ui.reportes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.catedra.eureka.databinding.ActivitySeleccionarUbicacionBinding
import com.catedra.eureka.utils.UbicacionHelper
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class SeleccionarUbicacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeleccionarUbicacionBinding
    private var marcadorSeleccionado: Marker? = null

    private var latFinal: Double = 0.0
    private var lngFinal: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, android.preference.PreferenceManager.getDefaultSharedPreferences(this))
        binding = ActivitySeleccionarUbicacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarMapa()
        obtenerUbicacionInicial()

        binding.btnConfirmarUbicacion.setOnClickListener {
            if (latFinal == 0.0 && lngFinal == 0.0) {
                Toast.makeText(this, "Por favor, toca el mapa para marcar un lugar", Toast.LENGTH_SHORT).show()
            } else {
                val intentResultado = Intent().apply {
                    putExtra("lat", latFinal)
                    putExtra("lng", lngFinal)
                }
                setResult(Activity.RESULT_OK, intentResultado)
                finish()
            }
        }
    }

    private fun configurarMapa() {
        binding.mapaSeleccion.setMultiTouchControls(true)
        binding.mapaSeleccion.controller.setZoom(16.0)
        
        val receptorEventos = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { posicionarMarcador(it) }
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }

        binding.mapaSeleccion.overlays.add(MapEventsOverlay(receptorEventos))
    }

    private fun obtenerUbicacionInicial() {
        UbicacionHelper.obtenerUbicacionActual(this,
            onResultado = { lat, lng, _ ->
                val puntoInicial = GeoPoint(lat, lng)
                binding.mapaSeleccion.controller.setCenter(puntoInicial)
                posicionarMarcador(puntoInicial)
            },
            onFallo = {
                // por defecto
                binding.mapaSeleccion.controller.setCenter(GeoPoint(-34.6037, -58.3816))
            }
        )
    }

    private fun posicionarMarcador(punto: GeoPoint) {
        latFinal = punto.latitude
        lngFinal = punto.longitude

        if (marcadorSeleccionado == null) {
            marcadorSeleccionado = Marker(binding.mapaSeleccion)
            binding.mapaSeleccion.overlays.add(marcadorSeleccionado)
        }

        marcadorSeleccionado?.apply {
            position = punto
            title = "Lugar seleccionado"
        }
        binding.mapaSeleccion.invalidate()
    }
}