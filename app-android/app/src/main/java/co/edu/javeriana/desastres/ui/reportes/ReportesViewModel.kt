package co.edu.javeriana.desastres.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.javeriana.desastres.data.ReporteRepository
import co.edu.javeriana.desastres.data.ResultadoSync
import co.edu.javeriana.desastres.data.local.ReporteEntity
import co.edu.javeriana.desastres.sync.ConnectivityMonitor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReportesViewModel(
    private val repo: ReporteRepository,
    conectividad: ConnectivityMonitor,
) : ViewModel() {
    val reportes: StateFlow<List<ReporteEntity>> =
        repo.reportes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pendientes: StateFlow<Int> =
        repo.pendientes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val enLinea: StateFlow<Boolean> = conectividad.enLinea

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes = _mensajes.receiveAsFlow()

    /**
     * Sincronización manual inmediata. Se ejecuta directamente (no vía
     * WorkManager) para dar respuesta al usuario y para funcionar también en
     * redes locales sin salida a internet, donde Android no marca la red como
     * "validada".
     */
    fun sincronizarAhora() {
        if (_sincronizando.value) return
        _sincronizando.value = true
        viewModelScope.launch {
            val msg = when (val r = repo.sincronizarPendientes()) {
                is ResultadoSync.Exito ->
                    if (r.enviados == 0 && r.rechazados == 0) "No hay reportes pendientes"
                    else buildString {
                        append("${r.enviados} reporte(s) sincronizado(s)")
                        if (r.rechazados > 0) append(", ${r.rechazados} rechazado(s) por el servidor")
                    }
                is ResultadoSync.SinConexion -> "Sin conexión con el sistema central. Se reintentará automáticamente."
                is ResultadoSync.Error -> r.mensaje
            }
            _sincronizando.value = false
            _mensajes.send(msg)
        }
    }
}
