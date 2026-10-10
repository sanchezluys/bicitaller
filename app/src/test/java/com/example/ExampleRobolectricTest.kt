package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.EstadoPago
import com.example.data.model.EstadoTrabajo
import com.example.data.model.Trabajo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BiciTaller", appName)
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
      fechaIngreso = ahora - (10L * 24 * 60 * 60 * 1000), // 10 days ago
      fechaEntrega = ahora - (5L * 24 * 60 * 60 * 1000), // delivered 5 days ago
      descripcionFalla = "Mantenimiento",
      estadoTrabajo = EstadoTrabajo.ENTREGADO,
      garantiaDias = 30
    )
    assertTrue(trabajoEntregado.isGarantiaActiva(ahora))

    val trabajoVencido = Trabajo(
      bicicletaId = 1,
      fechaIngreso = ahora - (40L * 24 * 60 * 60 * 1000),
      fechaEntrega = ahora - (35L * 24 * 60 * 60 * 1000), // delivered 35 days ago with 30-day warranty
      descripcionFalla = "Cambio cadena",
      estadoTrabajo = EstadoTrabajo.ENTREGADO,
      garantiaDias = 30
    )
    assertFalse(trabajoVencido.isGarantiaActiva(ahora))
  }
}
