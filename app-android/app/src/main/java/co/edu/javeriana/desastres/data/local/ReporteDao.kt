package co.edu.javeriana.desastres.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(reporte: ReporteEntity)

    @Query("SELECT * FROM reportes ORDER BY capturadoEn DESC")
    fun observarTodos(): Flow<List<ReporteEntity>>

    @Query("SELECT COUNT(*) FROM reportes WHERE estadoSync = 'PENDIENTE'")
    fun observarPendientes(): Flow<Int>

    @Query("SELECT * FROM reportes WHERE estadoSync = 'PENDIENTE' ORDER BY capturadoEn LIMIT :limite")
    suspend fun pendientes(limite: Int): List<ReporteEntity>

    @Query("UPDATE reportes SET estadoSync = 'SINCRONIZADO', sincronizadoEn = :cuando, ultimoError = NULL WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<String>, cuando: Long)

    @Query("UPDATE reportes SET estadoSync = 'RECHAZADO', ultimoError = :motivo WHERE id = :id")
    suspend fun marcarRechazado(id: String, motivo: String)

    @Query("UPDATE reportes SET intentos = intentos + 1, ultimoError = :error WHERE id IN (:ids)")
    suspend fun registrarFallo(ids: List<String>, error: String)
}
