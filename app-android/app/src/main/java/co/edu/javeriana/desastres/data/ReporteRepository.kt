package co.edu.javeriana.desastres.data

import co.edu.javeriana.desastres.data.local.ReporteDao
import co.edu.javeriana.desastres.data.local.ReporteEntity
import co.edu.javeriana.desastres.data.remote.ApiService
import co.edu.javeriana.desastres.data.remote.LoteRequest
import co.edu.javeriana.desastres.data.remote.aDto
import co.edu.javeriana.desastres.domain.Validacion
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.util.UUID

/** Resultado de un intento de sincronización. */
sealed interface ResultadoSync {
    data class Exito(val enviados: Int, val rechazados: Int) : ResultadoSync
    /** Problema transitorio (sin red, servidor caído): se debe reintentar. */
    data class SinConexion(val mensaje: String) : ResultadoSync
    /** Error permanente de la petición (p. ej. 4xx): reintentar no ayuda. */
    data class Error(val mensaje: String) : ResultadoSync
}

class DatosInvalidos(val errores: List<String>) : IllegalArgumentException(errores.joinToString("\n"))

/**
 * Repositorio offline-first: guarda siempre en local y sincroniza con el
 * sistema central cuando hay conexión.
 */
class ReporteRepository(
    private val dao: ReporteDao,
    private val api: suspend () -> ApiService,
    private val dispositivoId: suspend () -> String,
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    val reportes: Flow<List<ReporteEntity>> = dao.observarTodos()
    val pendientes: Flow<Int> = dao.observarPendientes()

    suspend fun crear(
        latitud: Double?,
        longitud: Double?,
        precisionMetros: Float?,
        nivel: Int?,
        evento: String?,
        descripcion: String?,
    ): ReporteEntity {
        val errores = Validacion.errores(latitud, longitud, nivel)
        if (errores.isNotEmpty()) throw DatosInvalidos(errores)
        val reporte = ReporteEntity(
            id = UUID.randomUUID().toString(),
            latitud = latitud!!,
            longitud = longitud!!,
            precisionMetros = precisionMetros,
            nivel = nivel!!,
            evento = evento?.trim()?.takeIf { it.isNotEmpty() }?.take(120),
            descripcion = descripcion?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
            capturadoEn = reloj(),
            dispositivoId = dispositivoId(),
        )
        dao.insertar(reporte)
        return reporte
    }

    /**
     * Envía todos los reportes pendientes en lotes. Es seguro llamarlo varias
     * veces o en paralelo con el worker: el servidor es idempotente por `id`.
     */
    suspend fun sincronizarPendientes(tamanoLote: Int = TAMANO_LOTE): ResultadoSync {
        var enviados = 0
        var rechazados = 0
        val procesados = mutableSetOf<String>()
        while (true) {
            val lote = dao.pendientes(tamanoLote).filterNot { it.id in procesados }
            if (lote.isEmpty()) break
            val ids = lote.map { it.id }
            procesados += ids

            val respuesta = try {
                api().enviarLote(LoteRequest(lote.map { it.aDto() }))
            } catch (e: IOException) {
                dao.registrarFallo(ids, e.message ?: "sin conexión")
                return ResultadoSync.SinConexion(e.message ?: "Sin conexión con el servidor")
            } catch (e: retrofit2.HttpException) {
                dao.registrarFallo(ids, "HTTP ${e.code()}")
                return if (e.code() >= 500 || e.code() == 408 || e.code() == 429) {
                    ResultadoSync.SinConexion("El servidor respondió ${e.code()}")
                } else {
                    ResultadoSync.Error("El servidor rechazó la petición (${e.code()})")
                }
            }

            val aceptados = respuesta.aceptados.filter { it in ids }
            if (aceptados.isNotEmpty()) dao.marcarSincronizados(aceptados, reloj())
            respuesta.rechazados.forEach { r ->
                if (r.id != null && r.id in ids) {
                    dao.marcarRechazado(r.id, r.motivo)
                    rechazados++
                }
            }
            enviados += aceptados.size
        }
        return ResultadoSync.Exito(enviados, rechazados)
    }

    suspend fun probarConexion(): Result<String> = runCatching { api().health().status }

    companion object {
        const val TAMANO_LOTE = 100
    }
}
