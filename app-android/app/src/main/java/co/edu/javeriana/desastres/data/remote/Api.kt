package co.edu.javeriana.desastres.data.remote

import co.edu.javeriana.desastres.data.local.ReporteEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Contrato del sistema central (ver sincronizacion/app/api). */
interface ApiService {
    @POST("api/v1/reportes/lote")
    suspend fun enviarLote(@Body lote: LoteRequest): LoteResponse

    @GET("health")
    suspend fun health(): HealthResponse
}

@Serializable
data class ReporteDto(
    val id: String,
    val latitud: Double,
    val longitud: Double,
    val nivel: Int,
    @SerialName("capturado_en") val capturadoEn: String,
    val evento: String? = null,
    val descripcion: String? = null,
    @SerialName("precision_m") val precisionM: Double? = null,
    @SerialName("dispositivo_id") val dispositivoId: String? = null,
)

@Serializable
data class LoteRequest(val reportes: List<ReporteDto>)

@Serializable
data class RechazoDto(val id: String? = null, val motivo: String)

@Serializable
data class LoteResponse(
    val aceptados: List<String>,
    val nuevos: Int = 0,
    val duplicados: Int = 0,
    val rechazados: List<RechazoDto> = emptyList(),
)

@Serializable
data class HealthResponse(val status: String)

fun ReporteEntity.aDto() = ReporteDto(
    id = id,
    latitud = latitud,
    longitud = longitud,
    nivel = nivel,
    capturadoEn = Instant.ofEpochMilli(capturadoEn).toString(),
    evento = evento,
    descripcion = descripcion,
    precisionM = precisionMetros?.toDouble(),
    dispositivoId = dispositivoId,
)

object ApiFactory {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cache = mutableMapOf<String, ApiService>()

    /** La URL del servidor es configurable en la app, así que se crea un cliente por URL. */
    @Synchronized
    fun crear(baseUrl: String): ApiService {
        val url = normalizarUrl(baseUrl)
        return cache.getOrPut(url) {
            Retrofit.Builder()
                .baseUrl(url)
                .client(cliente)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(ApiService::class.java)
        }
    }

    fun normalizarUrl(url: String): String {
        var u = url.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
        if (!u.endsWith("/")) u += "/"
        return u
    }
}
