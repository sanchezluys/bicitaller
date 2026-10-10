package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Bicicleta
import com.example.data.model.Cliente
import com.example.data.model.FotoEvidencia
import com.example.data.model.RepuestoUsado
import com.example.data.model.Trabajo
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nombre ASC")
    fun getAllClientes(): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes WHERE id = :id")
    fun getClienteById(id: Long): Flow<Cliente?>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun getClienteByIdOnce(id: Long): Cliente?

    @Query("SELECT * FROM clientes WHERE nombre LIKE '%' || :query || '%' OR telefono LIKE '%' || :query || '%'")
    fun searchClientes(query: String): Flow<List<Cliente>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCliente(cliente: Cliente): Long

    @Update
    suspend fun updateCliente(cliente: Cliente)

    @Delete
    suspend fun deleteCliente(cliente: Cliente)
}

@Dao
interface BicicletaDao {
    @Query("SELECT * FROM bicicletas ORDER BY marca ASC, modelo ASC")
    fun getAllBicicletas(): Flow<List<Bicicleta>>

    @Query("SELECT * FROM bicicletas WHERE clienteId = :clienteId ORDER BY id DESC")
    fun getBicicletasByCliente(clienteId: Long): Flow<List<Bicicleta>>

    @Query("SELECT * FROM bicicletas WHERE id = :id")
    fun getBicicletaById(id: Long): Flow<Bicicleta?>

    @Query("SELECT * FROM bicicletas WHERE id = :id")
    suspend fun getBicicletaByIdOnce(id: Long): Bicicleta?

    @Query("SELECT * FROM bicicletas WHERE numeroSerie = :serial LIMIT 1")
    suspend fun getBicicletaBySerialOnce(serial: String): Bicicleta?

    @Query("SELECT * FROM bicicletas WHERE numeroSerie LIKE '%' || :query || '%' OR marca LIKE '%' || :query || '%' OR modelo LIKE '%' || :query || '%'")
    fun searchBicicletas(query: String): Flow<List<Bicicleta>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBicicleta(bicicleta: Bicicleta): Long

    @Update
    suspend fun updateBicicleta(bicicleta: Bicicleta)

    @Delete
    suspend fun deleteBicicleta(bicicleta: Bicicleta)
}

data class TrabajoJoinItem(
    val trabajoId: Long,
    val bicicletaId: Long,
    val fechaIngreso: Long,
    val fechaEntrega: Long?,
    val descripcionFalla: String,
    val estadoTrabajo: String,
    val estadoPago: String,
    val costoManoObra: Double,
    val costoTotal: Double,
    val montoAbonado: Double,
    val garantiaDias: Int,
    val accesoriosDejados: String,
    val notas: String,
    // Bicicleta fields
    val biciMarca: String,
    val biciModelo: String,
    val biciColor: String,
    val biciNumeroSerie: String,
    val biciTipo: String,
    // Cliente fields
    val clienteId: Long,
    val clienteNombre: String,
    val clienteTelefono: String,
    val clienteEmail: String
)

@Dao
interface TrabajoDao {
    @Query("SELECT * FROM trabajos ORDER BY fechaIngreso DESC")
    fun getAllTrabajos(): Flow<List<Trabajo>>

    @Query("SELECT * FROM trabajos WHERE id = :id")
    fun getTrabajoById(id: Long): Flow<Trabajo?>

    @Query("SELECT * FROM trabajos WHERE id = :id")
    suspend fun getTrabajoByIdOnce(id: Long): Trabajo?

    @Query("SELECT * FROM trabajos WHERE bicicletaId = :bicicletaId ORDER BY fechaIngreso DESC")
    fun getTrabajosByBicicleta(bicicletaId: Long): Flow<List<Trabajo>>

    @Query("""
        SELECT 
            t.id AS trabajoId,
            t.bicicletaId AS bicicletaId,
            t.fechaIngreso AS fechaIngreso,
            t.fechaEntrega AS fechaEntrega,
            t.descripcionFalla AS descripcionFalla,
            t.estadoTrabajo AS estadoTrabajo,
            t.estadoPago AS estadoPago,
            t.costoManoObra AS costoManoObra,
            t.costoTotal AS costoTotal,
            t.montoAbonado AS montoAbonado,
            t.garantiaDias AS garantiaDias,
            t.accesoriosDejados AS accesoriosDejados,
            t.notas AS notas,
            b.marca AS biciMarca,
            b.modelo AS biciModelo,
            b.color AS biciColor,
            b.numeroSerie AS biciNumeroSerie,
            b.tipo AS biciTipo,
            c.id AS clienteId,
            c.nombre AS clienteNombre,
            c.telefono AS clienteTelefono,
            c.email AS clienteEmail
        FROM trabajos t
        INNER JOIN bicicletas b ON t.bicicletaId = b.id
        INNER JOIN clientes c ON b.clienteId = c.id
        ORDER BY t.fechaIngreso DESC
    """)
    fun getTrabajosConDetalle(): Flow<List<TrabajoJoinItem>>

    @Query("""
        SELECT 
            t.id AS trabajoId,
            t.bicicletaId AS bicicletaId,
            t.fechaIngreso AS fechaIngreso,
            t.fechaEntrega AS fechaEntrega,
            t.descripcionFalla AS descripcionFalla,
            t.estadoTrabajo AS estadoTrabajo,
            t.estadoPago AS estadoPago,
            t.costoManoObra AS costoManoObra,
            t.costoTotal AS costoTotal,
            t.montoAbonado AS montoAbonado,
            t.garantiaDias AS garantiaDias,
            t.accesoriosDejados AS accesoriosDejados,
            t.notas AS notas,
            b.marca AS biciMarca,
            b.modelo AS biciModelo,
            b.color AS biciColor,
            b.numeroSerie AS biciNumeroSerie,
            b.tipo AS biciTipo,
            c.id AS clienteId,
            c.nombre AS clienteNombre,
            c.telefono AS clienteTelefono,
            c.email AS clienteEmail
        FROM trabajos t
        INNER JOIN bicicletas b ON t.bicicletaId = b.id
        INNER JOIN clientes c ON b.clienteId = c.id
        WHERE c.nombre LIKE '%' || :query || '%' 
           OR b.numeroSerie LIKE '%' || :query || '%'
           OR b.marca LIKE '%' || :query || '%'
           OR b.modelo LIKE '%' || :query || '%'
           OR t.descripcionFalla LIKE '%' || :query || '%'
        ORDER BY t.fechaIngreso DESC
    """)
    fun searchTrabajosConDetalle(query: String): Flow<List<TrabajoJoinItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrabajo(trabajo: Trabajo): Long

    @Update
    suspend fun updateTrabajo(trabajo: Trabajo)

    @Delete
    suspend fun deleteTrabajo(trabajo: Trabajo)
}

@Dao
interface RepuestoUsadoDao {
    @Query("SELECT * FROM repuestos_usados WHERE trabajoId = :trabajoId ORDER BY id ASC")
    fun getRepuestosByTrabajo(trabajoId: Long): Flow<List<RepuestoUsado>>

    @Query("SELECT * FROM repuestos_usados WHERE trabajoId = :trabajoId ORDER BY id ASC")
    suspend fun getRepuestosByTrabajoSync(trabajoId: Long): List<RepuestoUsado>

    @Query("SELECT SUM(costoUnitario * cantidad) FROM repuestos_usados WHERE trabajoId = :trabajoId")
    suspend fun getTotalRepuestosCost(trabajoId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepuesto(repuesto: RepuestoUsado): Long

    @Update
    suspend fun updateRepuesto(repuesto: RepuestoUsado)

    @Delete
    suspend fun deleteRepuesto(repuesto: RepuestoUsado)
}

@Dao
interface FotoEvidenciaDao {
    @Query("SELECT * FROM fotos_evidencia WHERE trabajoId = :trabajoId ORDER BY fecha ASC")
    fun getFotosByTrabajo(trabajoId: Long): Flow<List<FotoEvidencia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoto(foto: FotoEvidencia): Long

    @Delete
    suspend fun deleteFoto(foto: FotoEvidencia)
}
