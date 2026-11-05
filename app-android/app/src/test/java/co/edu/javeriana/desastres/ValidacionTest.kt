package co.edu.javeriana.desastres

import co.edu.javeriana.desastres.domain.Validacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionTest {
    @Test fun `reporte valido no tiene errores`() {
        assertTrue(Validacion.errores(4.6, -74.08, 3).isEmpty())
    }

    @Test fun `niveles fuera de 1 a 5 se rechazan`() {
        assertEquals(1, Validacion.errores(0.0, 0.0, 0).size)
        assertEquals(1, Validacion.errores(0.0, 0.0, 6).size)
        for (n in 1..5) assertTrue(Validacion.errores(0.0, 0.0, n).isEmpty())
    }

    @Test fun `coordenadas fuera de rango o faltantes se rechazan`() {
        assertEquals(2, Validacion.errores(91.0, -181.0, 3).size)
        assertEquals(3, Validacion.errores(null, null, null).size)
    }

    @Test fun `acepta coma decimal`() {
        assertEquals(4.5, Validacion.parsearCoordenada(" 4,5 ")!!, 1e-9)
        assertEquals(-74.08, Validacion.parsearCoordenada("-74.08")!!, 1e-9)
        assertNull(Validacion.parsearCoordenada("abc"))
    }
}
