package co.edu.javeriana.desastres.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.edu.javeriana.desastres.BuildConfig

@Composable
fun AjustesScreen(vm: AjustesViewModel, snackbar: SnackbarHostState, modifier: Modifier) {
    val url by vm.url.collectAsStateWithLifecycle()
    val dispositivo by vm.dispositivo.collectAsStateWithLifecycle()
    val probando by vm.probando.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.mensajes.collect { snackbar.showSnackbar(it) } }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sistema central", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = url, onValueChange = vm::onUrl, singleLine = true,
                    label = { Text("URL del servidor") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Text(
                    "Emulador: http://10.0.2.2:8000/\nCelular en la misma red Wi-Fi: http://<IP-del-computador>:8000/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = vm::guardarYProbar, enabled = !probando, modifier = Modifier.fillMaxWidth()) {
                    if (probando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Guardar y probar conexión")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dispositivo", style = MaterialTheme.typography.titleMedium)
                Text("Identificador: $dispositivo", style = MaterialTheme.typography.bodyMedium)
                Text("Versión: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
