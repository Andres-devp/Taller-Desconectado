package co.edu.javeriana.desastres.ui.nuevo

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.edu.javeriana.desastres.domain.Validacion
import co.edu.javeriana.desastres.ui.theme.colorNivel

@Composable
fun NuevoReporteScreen(vm: NuevoReporteViewModel, snackbar: SnackbarHostState, modifier: Modifier) {
    val e by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.mensajes.collect { snackbar.showSnackbar(it) } }

    val permisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { res -> if (res.values.any { it }) vm.obtenerUbicacion() }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Nuevo reporte", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Funciona sin conexión: el reporte se guarda en el dispositivo y se envía al sistema central cuando vuelva la red.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Posición", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(
                    onClick = {
                        if (vm.tienePermiso()) vm.obtenerUbicacion()
                        else permisos.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ))
                    },
                    enabled = !e.buscandoUbicacion,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (e.buscandoUbicacion) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Buscando señal GPS…")
                    } else {
                        Icon(Icons.Filled.MyLocation, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Obtener mi ubicación")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = e.latitud, onValueChange = vm::onLatitud, label = { Text("Latitud") },
                        singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = e.longitud, onValueChange = vm::onLongitud, label = { Text("Longitud") },
                        singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                e.precision?.let {
                    Text("Precisión aproximada: ±${it.toInt()} m", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Nivel de afectación", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (n in Validacion.NIVEL_MIN..Validacion.NIVEL_MAX) {
                        BotonNivel(n, seleccionado = e.nivel == n, onClick = { vm.onNivel(n) },
                            modifier = Modifier.weight(1f))
                    }
                }
                Text(
                    e.nivel?.let { "Nivel $it · ${Validacion.etiquetaNivel(it)}" }
                        ?: "1 = leve … 5 = crítico",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (e.nivel != null) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. Detalles (opcional)", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = e.evento, onValueChange = vm::onEvento, singleLine = true,
                    label = { Text("Evento (p. ej. Inundación Mocoa)") }, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = e.descripcion, onValueChange = vm::onDescripcion, minLines = 2,
                    label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        e.errores.forEach {
            Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = vm::guardar, enabled = !e.guardando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Filled.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("Guardar reporte")
        }
    }
}

@Composable
private fun BotonNivel(n: Int, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val color = colorNivel(n)
    if (seleccionado) {
        Button(
            onClick = onClick, modifier = modifier.height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) { Text("$n", style = MaterialTheme.typography.titleLarge) }
    } else {
        OutlinedButton(
            onClick = onClick, modifier = modifier.height(56.dp),
            border = BorderStroke(2.dp, color),
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) { Text("$n", style = MaterialTheme.typography.titleLarge, color = color) }
    }
}
