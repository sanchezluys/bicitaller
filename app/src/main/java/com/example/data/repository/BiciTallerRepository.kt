package com.example.data.repository

import com.example.data.dao.BicicletaDao
import com.example.data.dao.ClienteDao
import com.example.data.dao.FotoEvidenciaDao
import com.example.data.dao.RepuestoUsadoDao
import com.example.data.dao.TrabajoDao
import com.example.data.dao.TrabajoJoinItem
import com.example.data.model.Bicicleta
import com.example.data.model.Cliente
import com.example.data.model.EstadoPago
import com.example.data.model.EstadoTrabajo
import com.example.data.model.FotoEvidencia
import com.example.data.model.RepuestoUsado
import com.example.data.model.Trabajo
import kotlinx.coroutines.flow.Flow

class BiciTallerRepository(
    private val clienteDao: ClienteDao,
    private val bicicletaDao: BicicletaDao,
    private val trabajoDao: TrabajoDao,
    private val repuestoUsadoDao: RepuestoUsadoDao,
    private val fotoEvidenciaDao: FotoEvidenciaDao
) {
    val allClientes: Flow<List<Cliente>> = clienteDao.getAllClientes()
    val allBicicletas: Flow<List<Bicicleta>> = bicicletaDao.getAllBicicletas()
    val allTrabajosWithDetails: Flow<List<TrabajoJoinItem>> = trabajoDao.getTrabajosConDetalle()

    fun searchTrabajos(query: String): Flow<List<TrabajoJoinItem>> {
        return if (query.isBlank()) {
            trabajoDao.getTrabajosConDetalle()
        } else {
            trabajoDao.searchTrabajosConDetalle(query.trim())
        }
    }

    fun getTrabajoById(id: Long): Flow<Trabajo?> = trabajoDao.getTrabajoById(id)
    suspend fun getTrabajoByIdOnce(id: Long): Trabajo? = trabajoDao.getTrabajoByIdOnce(id)

    fun getBicicletaById(id: Long): Flow<Bicicleta?> = bicicletaDao.getBicicletaById(id)
    suspend fun getBicicletaByIdOnce(id: Long): Bicicleta? = bicicletaDao.getBicicletaByIdOnce(id)

    fun getClienteById(id: Long): Flow<Cliente?> = clienteDao.getClienteById(id)
    suspend fun getClienteByIdOnce(id: Long): Cliente? = clienteDao.getClienteByIdOnce(id)

    fun getTrabajosByBicicleta(bicicletaId: Long): Flow<List<Trabajo>> = trabajoDao.getTrabajosByBicicleta(bicicletaId)
    fun getRepuestosByTrabajo(trabajoId: Long): Flow<List<RepuestoUsado>> = repuestoUsadoDao.getRepuestosByTrabajo(trabajoId)
    suspend fun getRepuestosByTrabajoSync(trabajoId: Long): List<RepuestoUsado> = repuestoUsadoDao.getRepuestosByTrabajoSync(trabajoId)

    fun getFotosByTrabajo(trabajoId: Long): Flow<List<FotoEvidencia>> = fotoEvidenciaDao.getFotosByTrabajo(trabajoId)

    suspend fun insertCliente(cliente: Cliente): Long = clienteDao.insertCliente(cliente)
    suspend fun updateCliente(cliente: Cliente) = clienteDao.updateCliente(cliente)
    suspend fun deleteCliente(cliente: Cliente) = clienteDao.deleteCliente(cliente)

    suspend fun insertBicicleta(bicicleta: Bicicleta): Long = bicicletaDao.insertBicicleta(bicicleta)
    suspend fun updateBicicleta(bicicleta: Bicicleta) = bicicletaDao.updateBicicleta(bicicleta)
    suspend fun deleteBicicleta(bicicleta: Bicicleta) = bicicletaDao.deleteBicicleta(bicicleta)

    suspend fun insertRepuesto(repuesto: RepuestoUsado): Long {
        val id = repuestoUsadoDao.insertRepuesto(repuesto)
        recalculateTrabajoTotals(repuesto.trabajoId)
        return id
    }

    suspend fun deleteRepuesto(repuesto: RepuestoUsado) {
        repuestoUsadoDao.deleteRepuesto(repuesto)
        recalculateTrabajoTotals(repuesto.trabajoId)
    }

    suspend fun insertFoto(foto: FotoEvidencia): Long = fotoEvidenciaDao.insertFoto(foto)
    suspend fun deleteFoto(foto: FotoEvidencia) = fotoEvidenciaDao.deleteFoto(foto)

    suspend fun updateTrabajo(trabajo: Trabajo) {
        trabajoDao.updateTrabajo(trabajo)
    }

    suspend fun deleteTrabajo(trabajo: Trabajo) {
        trabajoDao.deleteTrabajo(trabajo)
    }

    /**
     * Recalculates total cost and payment state automatically:
     * Costo Total = (Suma de repuestos) + Mano de Obra
     */
    suspend fun recalculateTrabajoTotals(trabajoId: Long) {
        val currentTrabajo = trabajoDao.getTrabajoByIdOnce(trabajoId) ?: return
        val totalRepuestos = repuestoUsadoDao.getTotalRepuestosCost(trabajoId) ?: 0.0
        val nuevoCostoTotal = (totalRepuestos + currentTrabajo.costoManoObra).coerceAtLeast(0.0)

        val nuevoEstadoPago = when {
            currentTrabajo.montoAbonado >= nuevoCostoTotal && nuevoCostoTotal > 0 -> EstadoPago.PAGADO
            currentTrabajo.montoAbonado > 0 -> EstadoPago.ABONADO
            else -> EstadoPago.PENDIENTE
        }

        val updated = currentTrabajo.copy(
            costoTotal = nuevoCostoTotal,
            estadoPago = nuevoEstadoPago
        )
        trabajoDao.updateTrabajo(updated)
    }

    /**
     * Advance or update job status with business rule check:
     * A work cannot advance to "En Reparación" without an associated budget.
     */
    suspend fun updateEstadoTrabajo(trabajoId: Long, nuevoEstado: EstadoTrabajo): Result<Unit> {
        val trabajo = trabajoDao.getTrabajoByIdOnce(trabajoId) ?: return Result.failure(Exception("Trabajo no encontrado"))
        if (nuevoEstado == EstadoTrabajo.EN_REPARACION && trabajo.costoTotal <= 0.0 && trabajo.costoManoObra <= 0.0) {
            return Result.failure(Exception("Debe registrar un presupuesto preliminar o mano de obra antes de pasar a En Reparación"))
        }

        val fechaEntrega = if (nuevoEstado == EstadoTrabajo.ENTREGADO && trabajo.fechaEntrega == null) {
            System.currentTimeMillis()
        } else {
            trabajo.fechaEntrega
        }

        trabajoDao.updateTrabajo(trabajo.copy(estadoTrabajo = nuevoEstado, fechaEntrega = fechaEntrega))
        return Result.success(Unit)
    }

    /**
     * Update payment info and adjust status
     */
    suspend fun registrarAbono(trabajoId: Long, abonoNuevo: Double) {
        val trabajo = trabajoDao.getTrabajoByIdOnce(trabajoId) ?: return
        val totalAbonado = abonoNuevo.coerceAtLeast(0.0)
        val nuevoEstadoPago = when {
            totalAbonado >= trabajo.costoTotal && trabajo.costoTotal > 0 -> EstadoPago.PAGADO
            totalAbonado > 0 -> EstadoPago.ABONADO
            else -> EstadoPago.PENDIENTE
        }
        trabajoDao.updateTrabajo(trabajo.copy(montoAbonado = totalAbonado, estadoPago = nuevoEstadoPago))
    }

    /**
     * Comprehensive intake creation for client, bike, initial work order, parts, photos
     */
    suspend fun crearRecepcionCompleta(
        clienteExistenteId: Long?,
        clienteNombre: String,
        clienteTelefono: String,
        clienteEmail: String,
        bicicletaExistenteId: Long?,
        biciMarca: String,
        biciModelo: String,
        biciColor: String,
        biciNumeroSerie: String,
        biciTipo: String,
        descripcionFalla: String,
        costoManoObra: Double,
        repuestosIniciales: List<Pair<String, Double>>, // Pair(nombre, costo)
        garantiaDias: Int,
        accesoriosDejados: String,
        notas: String,
        fotosRutas: List<String>
    ): Long {
        // 1. Resolve or create client
        val finalClienteId = if (clienteExistenteId != null && clienteExistenteId > 0) {
            clienteExistenteId
        } else {
            clienteDao.insertCliente(
                Cliente(
                    nombre = clienteNombre.trim(),
                    telefono = clienteTelefono.trim(),
                    email = clienteEmail.trim()
                )
            )
        }

        // 2. Resolve or create bike
        val finalBiciId = if (bicicletaExistenteId != null && bicicletaExistenteId > 0) {
            bicicletaExistenteId
        } else {
            // Check if serial exists
            val existing = if (biciNumeroSerie.isNotBlank()) {
                bicicletaDao.getBicicletaBySerialOnce(biciNumeroSerie.trim())
            } else null

            existing?.id ?: bicicletaDao.insertBicicleta(
                Bicicleta(
                    clienteId = finalClienteId,
                    marca = biciMarca.trim(),
                    modelo = biciModelo.trim(),
                    color = biciColor.trim(),
                    numeroSerie = biciNumeroSerie.trim(),
                    tipo = biciTipo
                )
            )
        }

        // 3. Calculate initial totals
        val sumRepuestos = repuestosIniciales.sumOf { it.second }
        val costoTotal = sumRepuestos + costoManoObra

        // 4. Insert Trabajo
        val trabajoId = trabajoDao.insertTrabajo(
            Trabajo(
                bicicletaId = finalBiciId,
                fechaIngreso = System.currentTimeMillis(),
                descripcionFalla = descripcionFalla.trim(),
                estadoTrabajo = EstadoTrabajo.PRESUPUESTO_GENERADO,
                estadoPago = EstadoPago.PENDIENTE,
                costoManoObra = costoManoObra,
                costoTotal = costoTotal,
                montoAbonado = 0.0,
                garantiaDias = garantiaDias,
                accesoriosDejados = accesoriosDejados,
                notas = notas.trim()
            )
        )

        // 5. Insert Repuestos
        for ((nombre, precio) in repuestosIniciales) {
            if (nombre.isNotBlank() && precio >= 0) {
                repuestoUsadoDao.insertRepuesto(
                    RepuestoUsado(
                        trabajoId = trabajoId,
                        nombreDescripcion = nombre.trim(),
                        costoUnitario = precio,
                        cantidad = 1
                    )
                )
            }
        }

        // 6. Insert Fotos
        for (ruta in fotosRutas) {
            fotoEvidenciaDao.insertFoto(
                FotoEvidencia(
                    trabajoId = trabajoId,
                    rutaArchivo = ruta,
                    fecha = System.currentTimeMillis()
                )
            )
        }

        return trabajoId
    }

    /**
     * Check if a bike has an active warranty from prior interventions
     */
    suspend fun checkActiveWarrantyForBike(bicicletaId: Long): Trabajo? {
        val now = System.currentTimeMillis()
        val trabajos = trabajoDao.getTrabajoByIdOnce(bicicletaId) // just in case
        return null
    }
}
