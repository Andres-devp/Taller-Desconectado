package co.edu.javeriana.desastres.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.javeriana.desastres.DesastresApp
import co.edu.javeriana.desastres.sync.SyncWorker
import co.edu.javeriana.desastres.ui.ajustes.AjustesScreen
import co.edu.javeriana.desastres.ui.ajustes.AjustesViewModel
import co.edu.javeriana.desastres.ui.nuevo.NuevoReporteScreen
import co.edu.javeriana.desastres.ui.nuevo.NuevoReporteViewModel
import co.edu.javeriana.desastres.ui.reportes.ReportesScreen
import co.edu.javeriana.desastres.ui.reportes.ReportesViewModel
import co.edu.javeriana.desastres.ui.theme.TemaDesastres

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val c = (application as DesastresApp).container
        setContent {
            TemaDesastres {
                val nuevoVm = viewModel {
                    NuevoReporteViewModel(c.repositorio, c.ubicacion) { SyncWorker.encolar(applicationContext) }
                }
                val reportesVm = viewModel { ReportesViewModel(c.repositorio, c.conectividad) }
                val ajustesVm = viewModel { AjustesViewModel(c.ajustes, c.repositorio) }
                AppRaiz(nuevoVm, reportesVm, ajustesVm)
            }
        }
    }
}

@Composable
private fun AppRaiz(
    nuevoVm: NuevoReporteViewModel,
    reportesVm: ReportesViewModel,
    ajustesVm: AjustesViewModel,
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val pendientes by reportesVm.pendientes.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pestana == 0, onClick = { pestana = 0 },
                    icon = { Icon(Icons.Filled.AddLocationAlt, null) }, label = { Text("Nuevo") },
                )
                NavigationBarItem(
                    selected = pestana == 1, onClick = { pestana = 1 },
                    icon = {
                        BadgedBox(badge = { if (pendientes > 0) Badge { Text("$pendientes") } }) {
                            Icon(Icons.AutoMirrored.Filled.List, null)
                        }
                    },
                    label = { Text("Reportes") },
                )
                NavigationBarItem(
                    selected = pestana == 2, onClick = { pestana = 2 },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Ajustes") },
                )
            }
        },
    ) { padding ->
        val mod = Modifier.padding(padding)
        when (pestana) {
            0 -> NuevoReporteScreen(nuevoVm, snackbar, mod)
            1 -> ReportesScreen(reportesVm, snackbar, mod)
            else -> AjustesScreen(ajustesVm, snackbar, mod)
        }
    }
}
