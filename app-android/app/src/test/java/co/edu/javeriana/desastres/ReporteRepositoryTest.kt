package co.edu.javeriana.desastres

import co.edu.javeriana.desastres.data.DatosInvalidos
import co.edu.javeriana.desastres.data.ReporteRepository
import co.edu.javeriana.desastres.data.ResultadoSync
import co.edu.javeriana.desastres.data.local.EstadoSync
import co.edu.javeriana.desastres.data.local.ReporteDao
import co.edu.javeriana.desastres.data.local.ReporteEntity
import co.edu.javeriana.desastres.data.remote.ApiFactory
import co.edu.javeriana.desastres.data.remote.ApiService
import co.edu.javeriana.desastres.data.remote.HealthResponse
import co.edu.javeriana.desastres.data.remote.LoteRequest
import co.edu.javeriana.desastres.data.remote.LoteResponse
import co.edu.javeriana.desastres.data.remote.RechazoDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/** DAO en memoria con la misma semántica que las consultas de Room. */
private class DaoEnMemoria : ReporteDao {
    val filas = MutableStateFlow<Map<String, ReporteEntity>>(emptyMap())
    private fun cambiar(ids: Collection<String>, f: (ReporteEntity) -> ReporteEntity) {
        filas.value = filas.value.mapValues { (id, r) -> if (id in ids) f(r) else r }
    }

    override suspend fun insertar(reporte: ReporteEntity) {
        check(reporte.id !in filas.value)
        filas.value = filas.value + (reporte.id to reporte)
    }
    override fun observarTodos(): Flow<List<ReporteEntity>> = filas.map { it.values.toList() }
    override fun observarPendientes(): Flow<Int> =
        filas.map { m -> m.values.count { it.estadoSync == EstadoSync.PENDIENTE } }
    override suspend fun pendientes(limite: Int) = filas.value.values
        .filter { it.estadoSync == EstadoSync.PENDIENTE }.sortedBy { it.capturadoEn }.take(limite)
    override suspend fun marcarSincronizados(ids: List<String>, cuando: Long) =
        cambiar(ids) { it.copy(estadoSync = EstadoSync.SINCRONIZADO, sincronizadoEn = cuando, ultimoError = null) }
    override suspend fun marcarRechazado(id: String, motivo: String) =
        cambiar(listOf(id)) { it.copy(estadoSync = EstadoSync.RECHAZADO, ultimoError = motivo) }
    override suspend fun registrarFallo(ids: List<String>, error: String) =
        cambiar(ids) { it.copy(intentos = it.intentos + 1, ultimoError = error) }

    fun estados() = filas.value.values.map { it.estadoSync }
}

/** Servidor falso: idempotente por id, como el real. */
private class ServidorFalso : ApiService {
    var caido = false
    val recibidos = mutableMapOf<String, Int>()
    var rechazarNivel: Int? = null
    var lotes = 0

    override suspend fun enviarLote(lote: LoteRequest): LoteResponse {
        if (caido) throw IOException("sin red")
        lotes++
        val (malos, buenos) = lote.reportes.partition { it.nivel == rechazarNivel }
        buenos.forEach { recibidos[it.id] = it.nivel }
        return LoteResponse(
            aceptados = buenos.map { it.id },
            rechazados = malos.map { RechazoDto(it.id, "nivel inválido") },
        )
    }
    override suspend fun health() = HealthResponse("ok")
}

class ReporteRepositoryTest {
    private val dao = DaoEnMemoria()
    private val servidor = ServidorFalso()
    private var t = 1_000L
    private val repo = ReporteRepository(dao, { servidor }, { "test" }, { t++ })

    private suspend fun crear(nivel: Int = 3) = repo.crear(4.6, -74.0, 5f, nivel, " Evento ", "")

    @Test fun `crear guarda localmente como pendiente`() = runTest {
        val r = crear()
        assertEquals(EstadoSync.PENDIENTE, dao.filas.value[r.id]!!.estadoSync)
        assertEquals("Evento", r.evento)
        assertEquals(null, r.descripcion)
    }

    @Test fun `crear rechaza datos invalidos sin guardar`() = runTest {
        try {
            repo.crear(4.6, -74.0, null, 7, null, null); fail()
        } catch (e: DatosInvalidos) {
            assertTrue(dao.filas.value.isEmpty())
        }
    }

    @Test fun `sin conexion los reportes quedan pendientes y luego se sincronizan`() = runTest {
        repeat(3) { crear() }
        servidor.caido = true
        assertTrue(repo.sincronizarPendientes() is ResultadoSync.SinConexion)
        assertTrue(dao.estados().all { it == EstadoSync.PENDIENTE })
        assertTrue(dao.filas.value.values.all { it.intentos == 1 })

        servidor.caido = false
        assertEquals(ResultadoSync.Exito(3, 0), repo.sincronizarPendientes())
        assertTrue(dao.estados().all { it == EstadoSync.SINCRONIZADO })
        assertEquals(3, servidor.recibidos.size)
    }

    @Test fun `envia en varios lotes`() = runTest {
        repeat(25) { crear() }
        assertEquals(ResultadoSync.Exito(25, 0), repo.sincronizarPendientes(tamanoLote = 10))
        assertEquals(3, servidor.lotes)
    }

    @Test fun `reportes rechazados no se reintentan`() = runTest {
        crear(nivel = 2); crear(nivel = 5)
        servidor.rechazarNivel = 5
        assertEquals(ResultadoSync.Exito(1, 1), repo.sincronizarPendientes())
        assertEquals(setOf(EstadoSync.SINCRONIZADO, EstadoSync.RECHAZADO), dao.estados().toSet())
        assertEquals(ResultadoSync.Exito(0, 0), repo.sincronizarPendientes())
    }

    @Test fun `normaliza url del servidor`() {
        assertEquals("http://192.168.1.5:8000/", ApiFactory.normalizarUrl(" 192.168.1.5:8000 "))
        assertEquals("https://x.org/", ApiFactory.normalizarUrl("https://x.org/"))
    }
}
