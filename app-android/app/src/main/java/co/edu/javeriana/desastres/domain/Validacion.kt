package co.edu.javeriana.desastres.domain

/** Reglas del dominio, iguales a las del sistema central (app/domain/entities.py). */
object Validacion {
    const val NIVEL_MIN = 1
    const val NIVEL_MAX = 5

    fun errores(latitud: Double?, longitud: Double?, nivel: Int?): List<String> = buildList {
        when {
            latitud == null -> add("Falta la latitud")
            latitud !in -90.0..90.0 -> add("La latitud debe estar entre -90 y 90")
        }
        when {
            longitud == null -> add("Falta la longitud")
            longitud !in -180.0..180.0 -> add("La longitud debe estar entre -180 y 180")
        }
        when {
            nivel == null -> add("Seleccione el nivel de afectación")
            nivel !in NIVEL_MIN..NIVEL_MAX -> add("El nivel debe estar entre $NIVEL_MIN y $NIVEL_MAX")
        }
    }

    /** Acepta coma o punto decimal, como se escribe en teclados en español. */
    fun parsearCoordenada(texto: String): Double? =
        texto.trim().replace(',', '.').toDoubleOrNull()

    fun etiquetaNivel(nivel: Int): String = when (nivel) {
        1 -> "Leve"
        2 -> "Moderado"
        3 -> "Serio"
        4 -> "Grave"
        5 -> "Crítico"
        else -> "?"
    }
}
