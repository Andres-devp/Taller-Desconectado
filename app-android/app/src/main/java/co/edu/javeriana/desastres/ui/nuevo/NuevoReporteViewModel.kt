package co.edu.javeriana.desastres.ui.nuevo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.javeriana.desastres.data.DatosInvalidos
import co.edu.javeriana.desastres.data.ReporteRepository
import co.edu.javeriana.desastres.domain.Validacion
import co.edu.javeriana.desastres.location.LocationProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class NuevoReporteEstado(
    val latitud: String = "",
    val longitud: String = "",
    val precision: Float? = null,
    val nivel: Int? = null,
    val evento: String = "",
    val descripcion: String = "",
    val buscandoUbicacion: Boolean = false,
    val guardando: Boolean = false,
    val errores: List<String> = emptyList(),
)

class NuevoReporteViewModel(
    private val repo: ReporteRepository,
    private val ubicacion: LocationProvider,
    private val alGuardar: () -> Unit,
) : ViewModel() {
    private val _estado = MutableStateFlow(NuevoReporteEstado())
    val estado: StateFlow<NuevoReporteEstado> = _estado

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes = _mensajes.receiveAsFlow()

    fun tienePermiso() = ubicacion.tienePermiso()

    fun onLatitud(v: String) = _estado.update { it.copy(latitud = v, precision = null) }
    fun onLongitud(v: String) = _estado.update { it.copy(longitud = v, precision = null) }
    fun onNivel(v: Int) = _estado.update { it.copy(nivel = v) }
    fun onEvento(v: String) = _estado.update { it.copy(evento = v) }
    fun onDescripcion(v: String) = _estado.update { it.copy(descripcion = v) }

    fun obtenerUbicacion() {
        if (_estado.value.buscandoUbicacion) return
        if (!ubicacion.ubicacionActivada()) {
            _mensajes.trySend("Active la ubicación (GPS) del dispositivo")
            return
        }
        _estado.update { it.copy(buscandoUbicacion = true) }
        viewModelScope.launch {
            val loc = ubicacion.ubicacionActual()
            _estado.update {
                if (loc == null) it.copy(buscandoUbicacion = false)
                else it.copy(
                    buscandoUbicacion = false,
                    latitud = String.format(Locale.US, "%.6f", loc.latitude),
                    longitud = String.format(Locale.US, "%.6f", loc.longitude),
                    precision = if (loc.hasAccuracy()) loc.accuracy else null,
                )
            }
            if (loc == null) _mensajes.send("No se pudo obtener la ubicación. Intente al aire libre o escríbala a mano.")
        }
    }

    fun guardar() {
        val e = _estado.value
        if (e.guardando) return
        _estado.update { it.copy(guardando = true, errores = emptyList()) }
        viewModelScope.launch {
            try {
                repo.crear(
                    latitud = Validacion.parsearCoordenada(e.latitud),
                    longitud = Validacion.parsearCoordenada(e.longitud),
                    precisionMetros = e.precision,
                    nivel = e.nivel,
                    evento = e.evento,
                    descripcion = e.descripcion,
                )
                // Se conserva el evento: en campo se suelen reportar varios sitios del mismo.
                _estado.value = NuevoReporteEstado(evento = e.evento)
                alGuardar()
                _mensajes.send("Reporte guardado en el dispositivo. Se sincronizará cuando haya conexión.")
            } catch (ex: DatosInvalidos) {
                _estado.update { it.copy(guardando = false, errores = ex.errores) }
            }
        }
    }
}
