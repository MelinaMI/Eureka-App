package com.catedra.eureka

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.catedra.eureka.data.services.AuthService
import com.catedra.eureka.data.services.CloudinaryService
import com.catedra.eureka.databinding.ActivityMainBinding
import com.catedra.eureka.ui.cuenta.CuentaFragment
import com.catedra.eureka.ui.detalles.DetallesFragment
import com.catedra.eureka.ui.home.HomeFragment
import com.catedra.eureka.ui.login.LoginFragment
import com.catedra.eureka.ui.mapa.MapFragment
import com.catedra.eureka.ui.reportes.ReportesFragment
import com.catedra.eureka.utils.IdiomaHelper
import com.catedra.eureka.utils.NavegacionHelper
import com.catedra.eureka.utils.NotificacionHelper

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    val authService = AuthService()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(IdiomaHelper.aplicarIdioma(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CloudinaryService.inicializar(this)
        NotificacionHelper.crearCanal(this)
        solicitarPermisoNotificaciones()
        verificarGpsActivo()

        if (authService.estaLogueado()) {
            Log.d(TAG, "Usuario ya logueado al iniciar, arrancando listener de alertas")
            val alertaService = (applicationContext as EurekaApplication).alertaService
            alertaService.iniciar(this)
        }

        val fragmentInicial = if (authService.estaLogueado()) HomeFragment() else LoginFragment()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, fragmentInicial)
                .commit()
        }

        procesarIntentDeNotificacion(intent)

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.fragment_home -> HomeFragment()
                R.id.fragment_reportes -> ReportesFragment()
                R.id.fragment_map -> MapFragment()
                R.id.fragment_cuenta -> CuentaFragment()
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
            true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent recibido")
        procesarIntentDeNotificacion(intent)
    }

    private fun procesarIntentDeNotificacion(intent: Intent) {
        val reporteId = intent.getStringExtra(NavegacionHelper.EXTRA_REPORTE_ID) ?: return
        Log.d(TAG, "Notificación tocada, navegando al reporte: $reporteId")
        val fragmentDetalles = DetallesFragment()
        val datos = Bundle()
        datos.putString(NavegacionHelper.EXTRA_REPORTE_ID, reporteId)
        fragmentDetalles.arguments = datos
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragmentDetalles)
            .addToBackStack(null)
            .commit()
    }

    private fun solicitarPermisoNotificaciones() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Solicitando permiso de notificaciones (Android 13+)")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                Log.d(TAG, "Permiso de notificaciones ya concedido")
            }
        }
    }

    private fun verificarGpsActivo() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsActivo = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Log.d(TAG, "GPS activo: $gpsActivo")
        if (!gpsActivo) {
            AlertDialog.Builder(this)
                .setTitle("GPS desactivado")
                .setMessage("Para ver reportes de mascotas cerca tuyo necesitás activar el GPS.")
                .setPositiveButton("Activar") { _, _ ->
                    startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Ahora no") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val concedido = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Resultado permiso notificaciones: concedido=$concedido")
        }
    }

    fun mostrarNavbar(mostrar: Boolean) {
        binding.bottomNavigationView.isVisible = mostrar
    }
}