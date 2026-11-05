package co.edu.javeriana.desastres.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.edu.javeriana.desastres.data.ReporteRepository
import co.edu.javeriana.desastres.data.SettingsStore
import co.edu.javeriana.desastres.data.remote.ApiFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class AjustesViewModel(
    private val ajustes: SettingsStore,
    private val repo: ReporteRepository,
) : ViewModel() {
    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url

    private val _dispositivo = MutableStateFlow("")
    val dispositivo: StateFlow<String> = _dispositivo

    private val _probando = MutableStateFlow(false)
    val probando: StateFlow<Boolean> = _probando

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes = _mensajes.receiveAsFlow()

    init {
        viewModelScope.launch {
            _url.value = ajustes.serverUrl.first()
            _dispositivo.value = ajustes.dispositivoId()
        }
    }

    fun onUrl(v: String) { _url.value = v }

    /** Guarda la URL y prueba la conexión con GET /health. */
    fun guardarYProbar() {
        val url = ApiFactory.normalizarUrl(_url.value)
        if (url.toHttpUrlOrNull() == null) {
            _mensajes.trySend("URL inválida")
            return
        }
        _probando.value = true
        viewModelScope.launch {
            ajustes.guardarServerUrl(url)
            _url.value = url
            val r = repo.probarConexion()
            _probando.value = false
            _mensajes.send(
                r.fold(
                    onSuccess = { "Conexión exitosa con el sistema central" },
                    onFailure = { "Guardada, pero no se pudo conectar: ${it.message ?: it.javaClass.simpleName}" },
                ),
            )
        }
    }
}
