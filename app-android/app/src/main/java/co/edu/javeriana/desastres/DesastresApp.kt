package co.edu.javeriana.desastres

import android.app.Application
import android.content.Context
import co.edu.javeriana.desastres.data.ReporteRepository
import co.edu.javeriana.desastres.data.SettingsStore
import co.edu.javeriana.desastres.data.local.AppDatabase
import co.edu.javeriana.desastres.data.remote.ApiFactory
import co.edu.javeriana.desastres.location.LocationProvider
import co.edu.javeriana.desastres.sync.ConnectivityMonitor
import co.edu.javeriana.desastres.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Contenedor de dependencias (inyección manual, sin frameworks). */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob())
    val ajustes = SettingsStore(context)
    val baseDatos = AppDatabase.crear(context)
    val ubicacion = LocationProvider(context)
    val conectividad = ConnectivityMonitor(context, scope)
    val repositorio = ReporteRepository(
        dao = baseDatos.reporteDao(),
        api = { ApiFactory.crear(ajustes.serverUrlActual()) },
        dispositivoId = { ajustes.dispositivoId() },
    )
}

class DesastresApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncWorker.programarPeriodica(this)
        // Por si quedaron pendientes de una sesión anterior.
        SyncWorker.encolar(this)
        // Al recuperar la conexión, sincronizar de inmediato.
        container.scope.launch {
            container.conectividad.enLinea.drop(1).filter { it }.collect {
                SyncWorker.encolar(this@DesastresApp)
            }
        }
    }
}
