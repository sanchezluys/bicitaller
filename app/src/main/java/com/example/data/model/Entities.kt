package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EstadoTrabajo(val titulo: String) {
    PRESUPUESTO_GENERADO("Presupuesto Generado"),
    APROBADO_CLIENTE("Aprobado por Cliente"),
    EN_REPARACION("En Reparación"),
    LISTO_ENTREGA("Listo para Entrega"),
    ENTREGADO("Entregado")
}

enum class EstadoPago(val titulo: String) {
    PENDIENTE("Pendiente"),
    ABONADO("Abonado"),
    PAGADO("Pagado")
}

@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val telefono: String,
    val email: String = "",
    val fechaRegistro: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bicicletas",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clienteId"]), Index(value = ["numeroSerie"])]
)
data class Bicicleta(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clienteId: Long,
    val marca: String,
    val modelo: String,
    val color: String,
    val numeroSerie: String,
    val tipo: String = "MTB" // MTB, Ruta, Urbana, Gravel, BMX, Eléctrica, etc.
)

@Entity(
    tableName = "trabajos",
    foreignKeys = [
        ForeignKey(
            entity = Bicicleta::class,
            parentColumns = ["id"],
            childColumns = ["bicicletaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bicicletaId"])]
)
data class Trabajo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bicicletaId: Long,
    val fechaIngreso: Long = System.currentTimeMillis(),
    val fechaEntrega: Long? = null,
    val descripcionFalla: String,
    val estadoTrabajo: EstadoTrabajo = EstadoTrabajo.PRESUPUESTO_GENERADO,
    val estadoPago: EstadoPago = EstadoPago.PENDIENTE,
    val costoManoObra: Double = 0.0,
    val costoTotal: Double = 0.0,
    val montoAbonado: Double = 0.0,
    val garantiaDias: Int = 30,
    val accesoriosDejados: String = "", // e.g. "Luces, Casco, Bomba"
    val notas: String = ""
) {
    val saldoPendiente: Double
        get() = (costoTotal - montoAbonado).coerceAtLeast(0.0)

    val fechaFinGarantia: Long?
        get() {
            val baseDate = fechaEntrega ?: if (estadoTrabajo == EstadoTrabajo.ENTREGADO) fechaIngreso else null
            return baseDate?.let { it + (garantiaDias * 24L * 60L * 60L * 1000L) }
        }

    fun isGarantiaActiva(ahora: Long = System.currentTimeMillis()): Boolean {
        val fin = fechaFinGarantia ?: return false
        return ahora <= fin
    }
}

@Entity(
    tableName = "repuestos_usados",
    foreignKeys = [
        ForeignKey(
            entity = Trabajo::class,
            parentColumns = ["id"],
            childColumns = ["trabajoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trabajoId"])]
)
data class RepuestoUsado(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trabajoId: Long,
    val nombreDescripcion: String,
    val costoUnitario: Double,
    val cantidad: Int = 1
) {
    val subtotal: Double
        get() = costoUnitario * cantidad
}

@Entity(
    tableName = "fotos_evidencia",
    foreignKeys = [
        ForeignKey(
            entity = Trabajo::class,
            parentColumns = ["id"],
            childColumns = ["trabajoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trabajoId"])]
)
data class FotoEvidencia(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trabajoId: Long,
    val rutaArchivo: String,
    val fecha: Long = System.currentTimeMillis()
)

// Aggregate Data for Detailed Views & Lists
data class TrabajoCompleto(
    val trabajo: Trabajo,
    val bicicleta: Bicicleta,
    val cliente: Cliente,
    val totalRepuestos: Double = 0.0
)
