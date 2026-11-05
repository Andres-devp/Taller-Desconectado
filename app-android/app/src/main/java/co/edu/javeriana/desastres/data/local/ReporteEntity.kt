package co.edu.javeriana.desastres.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Estado del reporte frente al sistema central. */
enum class EstadoSync {
    /** Guardado solo en el dispositivo; se enviará cuando haya conexión. */
    PENDIENTE,
    /** Confirmado por el sistema central. */
    SINCRONIZADO,
    /** El sistema central lo rechazó por datos inválidos; no se reintenta. */
    RECHAZADO,
}

/**
 * Reporte de un sitio de desastre. La base local (Room) es la fuente de verdad
 * de la app: todo se guarda aquí primero, haya o no conexión.
 */
@Entity(tableName = "reportes", indices = [Index("estadoSync")])
data class ReporteEntity(
    /** UUID generado en el dispositivo; es la clave de idempotencia en el servidor. */
    @PrimaryKey val id: String,
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Float?,
    val nivel: Int,
    val evento: String?,
    val descripcion: String?,
    /** Epoch en milisegundos (UTC) del momento de la captura. */
    val capturadoEn: Long,
    val dispositivoId: String,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
    val intentos: Int = 0,
    val ultimoError: String? = null,
    val sincronizadoEn: Long? = null,
)
