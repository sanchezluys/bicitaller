package com.example

import com.example.data.model.EstadoPago
import com.example.data.model.EstadoTrabajo
import com.example.data.model.Trabajo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCalculoSaldoPendiente() {
    val trabajo = Trabajo(
      bicicletaId = 1,
      descripcionFalla = "Ajuste frenos",
      costoManoObra = 30000.0,
      costoTotal = 85000.0,
      montoAbonado = 40000.0,
      estadoPago = EstadoPago.ABONADO
    )
    assertEquals(45000.0, trabajo.saldoPendiente, 0.001)
  }

  @Test
  fun testGarantiaCalculo() {
    val ahora = System.currentTimeMillis()
    val trabajoEntregado = Trabajo(
      bicicletaId = 1,
      fechaIngreso = ahora - (10L * 24 * 60 * 60 * 1000),
      fechaEntrega = ahora - (5L * 24 * 60 * 60 * 1000),
      descripcionFalla = "Mantenimiento",
      estadoTrabajo = EstadoTrabajo.ENTREGADO,
      garantiaDias = 30
    )
    assertTrue(trabajoEntregado.isGarantiaActiva(ahora))

    val trabajoVencido = Trabajo(
      bicicletaId = 1,
      fechaIngreso = ahora - (40L * 24 * 60 * 60 * 1000),
      fechaEntrega = ahora - (35L * 24 * 60 * 60 * 1000),
      descripcionFalla = "Cambio cadena",
      estadoTrabajo = EstadoTrabajo.ENTREGADO,
      garantiaDias = 30
    )
    assertFalse(trabajoVencido.isGarantiaActiva(ahora))
  }
}

