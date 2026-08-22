package com.catedra.eureka.ui.mapa

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.catedra.eureka.R
import com.catedra.eureka.data.model.Reporte
import com.catedra.eureka.data.model.Usuario
import com.catedra.eureka.utils.NavegacionHelper
import com.catedra.eureka.utils.PermisoHelper
import com.catedra.eureka.utils.UbicacionHelper
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MapFragment : Fragment() {

    private val TAG = "MapFragment"

    private val viewModel: MapViewModel by viewModels()
    private lateinit var mapa: MapView

    private var ubicacionActual: GeoPoint? = null
    private var marcadorPropio: Marker? = null
    private var circuloRadio: Polygon? = null
    private var mapaListo = false

    private val permisoLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedidos = permisos.entries.filter { it.value }.map { it.key }
        val denegados = permisos.entries.filter { !it.value }.map { it.key }
        Log.d(TAG, "Permisos concedidos: $concedidos")
        Log.d(TAG, "Permisos denegados: $denegados")

        if (PermisoHelper.tienePermisoUbicacion(this)) {
            Log.d(TAG, "Permisos aceptados, obteniendo ubicación")
            obtenerYMostrarUbicacion()
        } else {
            Log.w(TAG, "Permisos denegados, centrando en Buenos Aires por defecto")
            centrarEnFallback()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        val vista = inflater.inflate(R.layout.fragment_map, container, false)
        mapa = vista.findViewById(R.id.mapaAlertas)

        val toolbar = vista.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        configurarMapaInicial()
        //verificarPermisosYUbicar()
        observarEstado()

        return vista
    }

    private fun configurarMapaInicial() {
        Log.d(TAG, "Configurando mapa inicial")
        mapa.setMultiTouchControls(true)
        mapa.controller.setZoom(14.0)
    }

    private fun verificarPermisosYUbicar() {
        if (PermisoHelper.tienePermisoUbicacion(this)) {
            Log.d(TAG, "Permisos ya concedidos, obteniendo ubicación")
            obtenerYMostrarUbicacion()
        } else {
            Log.d(TAG, "Solicitando permisos de ubicación")
            PermisoHelper.solicitarPermisos(this, permisoLauncher)
        }
    }

    private fun obtenerYMostrarUbicacion() {
        Log.d(TAG, "Iniciando obtención de ubicación")
        UbicacionHelper.obtenerUbicacionActual(
            context = requireContext(),
            onResultado = { lat, lng, direccion ->
                if (!isAdded || !mapaListo) return@obtenerUbicacionActual
                Log.d(TAG, "Ubicación propia obtenida: lat=$lat, lng=$lng, dir=$direccion")
                ubicacionActual = GeoPoint(lat, lng)
                mapa.controller.setCenter(ubicacionActual)
                dibujarMarcadorPropio()
                viewModel.guardarUbicacionUsuario(lat, lng)
                val usuario = viewModel.uiState.value.usuario
                if (usuario != null) {
                    dibujarCirculoRadio(usuario)
                } else {
                    Log.d(TAG, "Usuario aún no cargado, el círculo se dibujará cuando llegue")
                }
            },
            onFallo = {
                if (!isAdded || !mapaListo) return@obtenerUbicacionActual
                Log.w(TAG, "Falló obtención de ubicación, usando fallback")
                centrarEnFallback()
            }
        )
    }

    private fun centrarEnFallback() {
        val fallback = GeoPoint(-34.6037, -58.3816)
        Log.d(TAG, "Centrando en fallback: Buenos Aires")
        mapa.controller.setCenter(fallback)
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { estado ->
                Log.d(TAG, "Estado actualizado: ${estado.reportes.size} reportes, usuario=${estado.usuario?.id}, error=${estado.error}")
                if (estado.error != null) {
                    Log.e(TAG, "Error en estado: ${estado.error}")
                }
                mostrarReportesEnMapa(estado.reportes)
                val usuario = estado.usuario
                if (usuario != null && ubicacionActual != null) {
                    dibujarCirculoRadio(usuario)
                }
            }
        }
    }

    private fun dibujarMarcadorPropio() {
        val posicion = ubicacionActual ?: return
        Log.d(TAG, "Dibujando marcador propio en $posicion")

        if (marcadorPropio != null) {
            mapa.overlays.remove(marcadorPropio)
        }

        marcadorPropio = Marker(mapa).apply {
            position = posicion
            title = "Mi ubicación"
            
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = androidx.core.content.ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_mi_ubicacion
            )
        }

        mapa.overlays.add(marcadorPropio)
        mapa.invalidate()
    }

    private fun dibujarCirculoRadio(usuario: Usuario) {
        val centro = ubicacionActual ?: run {
            Log.d(TAG, "No hay ubicación aún para dibujar círculo")
            return
        }

        val radioMetros = usuario.radioAlertaKm * 1000.0
        Log.d(TAG, "Dibujando círculo de radio: ${usuario.radioAlertaKm} km (${radioMetros} m)")

        if (circuloRadio != null) {
            mapa.overlays.remove(circuloRadio)
        }

        circuloRadio = Polygon().apply {
            points = Polygon.pointsAsCircle(centro, radioMetros)
            fillColor = Color.argb(40, 33, 150, 243)
            strokeColor = Color.argb(180, 33, 150, 243)
            strokeWidth = 3f
        }

        mapa.overlays.add(0, circuloRadio)
        mapa.invalidate()
        Log.d(TAG, "Círculo dibujado correctamente")
    }

    private fun mostrarReportesEnMapa(lista: List<Reporte>) {
        if (!isAdded || !mapaListo) return
        Log.d(TAG, "Actualizando marcadores de reportes: ${lista.size}")

        val circulo = circuloRadio
        val marcadorPropioActual = marcadorPropio

        mapa.overlays.clear()

        if (circulo != null) mapa.overlays.add(circulo)
        if (marcadorPropioActual != null) mapa.overlays.add(marcadorPropioActual)

        var marcadoresAgregados = 0
        for (reporte in lista) {
            val lat = reporte.latitud
            val lng = reporte.longitud
            if (lat == 0.0 && lng == 0.0) {
                Log.w(TAG, "Reporte ${reporte.id} sin coordenadas válidas, se omite")
                continue
            }

            Log.d(TAG, "Agregando marcador para reporte ${reporte.id} en lat=$lat, lng=$lng")
            val marcador = Marker(mapa).apply {
                position = GeoPoint(lat, lng)
                title = reporte.nombre.ifEmpty { reporte.animal }
                snippet = reporte.descripcion
                setOnMarkerClickListener { _, _ ->
                    mapa.post {
                        if (isAdded && !parentFragmentManager.isStateSaved) {
                            Log.d(TAG, "Click en marcador de reporte: ${reporte.id}")
                            NavegacionHelper.irADetalles(
                                this@MapFragment,
                                R.id.fragmentContainer,
                                reporte.id
                            )
                        }
                    }
                    true
                }
            }

            cargarIconoReporte(reporte, marcador)
            mapa.overlays.add(marcador)
            marcadoresAgregados++
        }

        Log.d(TAG, "Marcadores de reportes dibujados: $marcadoresAgregados")
        mapa.invalidate()
    }

    private fun cargarIconoReporte(reporte: Reporte, marcador: Marker) {
        if (reporte.fotoUrl.isNotEmpty()) {
            Glide.with(this)
                .asBitmap()
                .load(reporte.fotoUrl)
                .override(120, 120)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        Log.d(TAG, "Foto cargada para reporte ${reporte.id}")
                        marcador.icon = BitmapDrawable(resources, resource)
                        mapa.invalidate()
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        Log.w(TAG, "Falló carga de foto para reporte ${reporte.id}, usando ícono default")
                        marcador.icon = androidx.core.content.ContextCompat.getDrawable(
                            requireContext(),
                            org.osmdroid.library.R.drawable.marker_default
                        )
                        mapa.invalidate()
                    }
                })
        } else {
            Log.d(TAG, "Reporte ${reporte.id} sin foto, usando ícono default")
            marcador.icon = androidx.core.content.ContextCompat.getDrawable(
                requireContext(),
                org.osmdroid.library.R.drawable.marker_default
            )
        }
    }

    override fun onResume() {
        super.onResume()
        mapa.onResume()
        mapaListo = true  
        Log.d(TAG, "onResume - mapa activo")
        verificarPermisosYUbicar()

        val estadoActual = viewModel.uiState.value
        mostrarReportesEnMapa(estadoActual.reportes)
        val usuario = estadoActual.usuario
        if (usuario != null && ubicacionActual != null) {
            dibujarCirculoRadio(usuario)
        }
    }

    override fun onPause() {
        super.onPause()
        mapaListo = false 
        Log.d(TAG, "onPause - mapa pausado")
        mapa.onPause()
    }
}