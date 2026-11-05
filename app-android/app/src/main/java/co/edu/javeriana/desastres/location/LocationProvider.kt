package co.edu.javeriana.desastres.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Obtiene la posición actual con el LocationManager del sistema. El GPS no
 * necesita internet, por eso la captura funciona totalmente desconectada y sin
 * depender de Google Play Services.
 */
class LocationProvider(private val context: Context) {
    private val lm = context.getSystemService(LocationManager::class.java)

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun ubicacionActivada(): Boolean = LocationManagerCompat.isLocationEnabled(lm)

    @SuppressLint("MissingPermission")
    suspend fun ubicacionActual(timeoutMs: Long = 30_000): Location? {
        if (!tienePermiso()) return null
        val proveedores = buildList {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                lm.isProviderEnabled(LocationManager.FUSED_PROVIDER)) add(LocationManager.FUSED_PROVIDER)
        }
        for (p in proveedores) {
            val loc = withTimeoutOrNull(timeoutMs / proveedores.size.coerceAtLeast(1)) { solicitar(p) }
            if (loc != null) return loc
        }
        // Último recurso: la última posición conocida por cualquier proveedor.
        return proveedores.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun solicitar(proveedor: String): Location? =
        suspendCancellableCoroutine { cont ->
            val cancel = CancellationSignal()
            cont.invokeOnCancellation { cancel.cancel() }
            LocationManagerCompat.getCurrentLocation(
                lm, proveedor, cancel, ContextCompat.getMainExecutor(context),
            ) { loc -> if (cont.isActive) cont.resume(loc) }
        }
}
