package co.edu.javeriana.desastres.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import co.edu.javeriana.desastres.DesastresApp
import co.edu.javeriana.desastres.data.ResultadoSync
import java.util.concurrent.TimeUnit

/**
 * Envía los reportes pendientes al sistema central.
 *
 * WorkManager solo lo ejecuta cuando hay red (restricción CONNECTED), sobrevive
 * a reinicios del dispositivo y a que el usuario cierre la app, y reintenta con
 * backoff exponencial ante fallos transitorios.
 */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as DesastresApp).container.repositorio
        return when (val r = repo.sincronizarPendientes()) {
            is ResultadoSync.Exito -> {
                Log.i(TAG, "sincronización OK: ${r.enviados} enviados, ${r.rechazados} rechazados")
                Result.success()
            }
            is ResultadoSync.SinConexion -> {
                Log.w(TAG, "sincronización pospuesta: ${r.mensaje}")
                Result.retry()
            }
            is ResultadoSync.Error -> {
                Log.e(TAG, "sincronización fallida: ${r.mensaje}")
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val UNICO_INMEDIATO = "sync-inmediata"
        private const val UNICO_PERIODICO = "sync-periodica"

        private val conRed = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Se llama al guardar un reporte y al recuperar la conexión. */
        fun encolar(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(conRed)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            // APPEND_OR_REPLACE: si ya hay una sincronización en curso, esta corre
            // después y recoge los reportes creados mientras tanto.
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNICO_INMEDIATO, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        /** Respaldo: revisa pendientes cada 15 minutos (mínimo permitido por Android). */
        fun programarPeriodica(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(conRed)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNICO_PERIODICO, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
