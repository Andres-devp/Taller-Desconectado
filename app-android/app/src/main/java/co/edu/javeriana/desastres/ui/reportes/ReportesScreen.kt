package co.edu.javeriana.desastres.ui.reportes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.edu.javeriana.desastres.data.local.EstadoSync
import co.edu.javeriana.desastres.data.local.ReporteEntity
import co.edu.javeriana.desastres.domain.Validacion
import co.edu.javeriana.desastres.ui.theme.colorNivel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportesScreen(vm: ReportesViewModel, snackbar: SnackbarHostState, modifier: Modifier) {
    val reportes by vm.reportes.collectAsStateWithLifecycle()
    val pendientes by vm.pendientes.collectAsStateWithLifecycle()
    val enLinea by vm.enLinea.collectAsStateWithLifecycle()
    val sincronizando by vm.sincronizando.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.mensajes.collect { snackbar.showSnackbar(it) } }

    Column(modifier.fillMaxSize()) {
        BannerConexion(enLinea)
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${reportes.size} reportes", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (pendientes == 0) "Todo sincronizado" else "$pendientes pendiente(s) por sincronizar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = vm::sincronizarAhora, enabled = !sincronizando && pendientes > 0) {
                if (sincronizando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Sync, null)
                Spacer(Modifier.width(6.dp))
                Text("Sincronizar")
            }
        }

        if (reportes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Aún no hay reportes. Cree uno en la pestaña «Nuevo».",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(reportes, key = { it.id }) { TarjetaReporte(it) }
            }
        }
    }
}

@Composable
private fun BannerConexion(enLinea: Boolean) {
    val (fondo, texto, icono) = if (enLinea) {
        Triple(Color(0xFF2E7D32), "En línea · sincronización automática activa", Icons.Filled.Wifi)
    } else {
        Triple(Color(0xFF616161), "Sin conexión · los reportes se guardan en el dispositivo", Icons.Filled.CloudOff)
    }
    Surface(color = fondo, contentColor = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TarjetaReporte(r: ReporteEntity) {
    val formato = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(colorNivel(r.nivel), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("${r.nivel}", color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    r.evento ?: Validacion.etiquetaNivel(r.nivel),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    String.format(Locale.US, "%.5f, %.5f", r.latitud, r.longitud),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    formato.format(Date(r.capturadoEn)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (r.estadoSync == EstadoSync.RECHAZADO && r.ultimoError != null) {
                    Text(r.ultimoError, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            ChipEstado(r)
        }
    }
}

@Composable
private fun ChipEstado(r: ReporteEntity) {
    val (texto, icono) = when (r.estadoSync) {
        EstadoSync.SINCRONIZADO -> "Enviado" to Icons.Filled.CloudDone
        EstadoSync.RECHAZADO -> "Rechazado" to Icons.Filled.ErrorOutline
        EstadoSync.PENDIENTE ->
            (if (r.intentos > 0) "Reintento ${r.intentos}" else "Pendiente") to
                (if (r.intentos > 0) Icons.Filled.CloudUpload else Icons.Filled.Schedule)
    }
    AssistChip(onClick = {}, label = { Text(texto) }, leadingIcon = { Icon(icono, null, Modifier.size(16.dp)) })
}
