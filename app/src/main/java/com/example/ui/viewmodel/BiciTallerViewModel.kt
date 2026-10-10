package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.TrabajoJoinItem
import com.example.data.db.BiciTallerDatabase
import com.example.data.model.Bicicleta
import com.example.data.model.Cliente
import com.example.data.model.EstadoPago
import com.example.data.model.EstadoTrabajo
import com.example.data.model.FotoEvidencia
import com.example.data.model.RepuestoUsado
import com.example.data.model.Trabajo
import com.example.data.repository.BiciTallerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardMetrics(
    val trabajosPendientes: Int = 0,
    val listosParaEntrega: Int = 0,
    val entregados: Int = 0,
    val ingresosMes: Double = 0.0,
    val cobrosPendientes: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
class BiciTallerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BiciTallerRepository

    init {
        val db = BiciTallerDatabase.getDatabase(application)
        repository = BiciTallerRepository(
            clienteDao = db.clienteDao(),
            bicicletaDao = db.bicicletaDao(),
            trabajoDao = db.trabajoDao(),
            repuestoUsadoDao = db.repuestoUsadoDao(),
            fotoEvidenciaDao = db.fotoEvidenciaDao()
        )
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filtroEstado = MutableStateFlow<EstadoTrabajo?>(null)
    val filtroEstado: StateFlow<EstadoTrabajo?> = _filtroEstado.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    val clientes: StateFlow<List<Cliente>> = repository.allClientes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bicicletas: StateFlow<List<Bicicleta>> = repository.allBicicletas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All jobs reactively filtered by query and status chip
    val filteredTrabajos: StateFlow<List<TrabajoJoinItem>> = combine(
        _searchQuery.flatMapLatest { query -> repository.searchTrabajos(query) },
        _filtroEstado
    ) { jobs, filter ->
        if (filter == null) {
            jobs
        } else {
            jobs.filter { it.estadoTrabajo == filter.name }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard metrics calculated reactively
    val metrics: StateFlow<DashboardMetrics> = repository.allTrabajosWithDetails.combine(_searchQuery) { allJobs, _ ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        var pendientes = 0
        var listos = 0
        var entregados = 0
        var ingresos = 0.0
        var porCobrar = 0.0

        for (item in allJobs) {
            when (item.estadoTrabajo) {
                EstadoTrabajo.LISTO_ENTREGA.name -> listos++
                EstadoTrabajo.ENTREGADO.name -> entregados++
                else -> pendientes++
            }

            // Incomes this month
            cal.timeInMillis = item.fechaIngreso
            if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                ingresos += item.montoAbonado
            }

            val saldo = (item.costoTotal - item.montoAbonado).coerceAtLeast(0.0)
            porCobrar += saldo
        }

        DashboardMetrics(
            trabajosPendientes = pendientes,
            listosParaEntrega = listos,
            entregados = entregados,
            ingresosMes = ingresos,
            cobrosPendientes = porCobrar
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFiltroEstado(estado: EstadoTrabajo?) {
        _filtroEstado.value = estado
    }

    // Detail Screen flows
    fun getTrabajoDetail(trabajoId: Long): StateFlow<Trabajo?> {
        return repository.getTrabajoById(trabajoId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun getRepuestosForTrabajo(trabajoId: Long): StateFlow<List<RepuestoUsado>> {
        return repository.getRepuestosByTrabajo(trabajoId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getFotosForTrabajo(trabajoId: Long): StateFlow<List<FotoEvidencia>> {
        return repository.getFotosByTrabajo(trabajoId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getHistorialForBicicleta(bicicletaId: Long): StateFlow<List<Trabajo>> {
        return repository.getTrabajosByBicicleta(bicicletaId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    suspend fun getBicicletaByIdOnce(id: Long): Bicicleta? = repository.getBicicletaByIdOnce(id)
    suspend fun getClienteByIdOnce(id: Long): Cliente? = repository.getClienteByIdOnce(id)
    suspend fun getTrabajoByIdOnce(id: Long): Trabajo? = repository.getTrabajoByIdOnce(id)
    suspend fun getRepuestosSync(trabajoId: Long): List<RepuestoUsado> = repository.getRepuestosByTrabajoSync(trabajoId)

    fun cambiarEstado(trabajoId: Long, nuevoEstado: EstadoTrabajo) {
        viewModelScope.launch {
            val result = repository.updateEstadoTrabajo(trabajoId, nuevoEstado)
            if (result.isSuccess) {
                _snackbarEvent.emit("Estado actualizado a: ${nuevoEstado.titulo}")
            } else {
                _snackbarEvent.emit(result.exceptionOrNull()?.message ?: "Error al actualizar estado")
            }
        }
    }

    fun agregarRepuesto(trabajoId: Long, nombre: String, costoUnitario: Double, cantidad: Int) {
        viewModelScope.launch {
            if (nombre.isBlank() || costoUnitario < 0 || cantidad <= 0) {
                _snackbarEvent.emit("Ingrese datos de repuesto válidos")
                return@launch
            }
            repository.insertRepuesto(
                RepuestoUsado(
                    trabajoId = trabajoId,
                    nombreDescripcion = nombre.trim(),
                    costoUnitario = costoUnitario,
                    cantidad = cantidad
                )
            )
            _snackbarEvent.emit("Repuesto agregado")
        }
    }

    fun eliminarRepuesto(repuesto: RepuestoUsado) {
        viewModelScope.launch {
            repository.deleteRepuesto(repuesto)
            _snackbarEvent.emit("Repuesto eliminado")
        }
    }

    fun actualizarManoObra(trabajoId: Long, nuevaManoObra: Double) {
        viewModelScope.launch {
            val trabajo = repository.getTrabajoByIdOnce(trabajoId) ?: return@launch
            repository.updateTrabajo(trabajo.copy(costoManoObra = nuevaManoObra))
            repository.recalculateTrabajoTotals(trabajoId)
            _snackbarEvent.emit("Mano de obra actualizada")
        }
    }

    fun registrarAbono(trabajoId: Long, montoAbonado: Double) {
        viewModelScope.launch {
            repository.registrarAbono(trabajoId, montoAbonado)
            _snackbarEvent.emit("Abono registrado correctamente")
        }
    }

    fun actualizarGarantia(trabajoId: Long, dias: Int) {
        viewModelScope.launch {
            val trabajo = repository.getTrabajoByIdOnce(trabajoId) ?: return@launch
            repository.updateTrabajo(trabajo.copy(garantiaDias = dias))
            _snackbarEvent.emit("Días de garantía actualizados")
        }
    }

    fun agregarFotoEvidencia(trabajoId: Long, ruta: String) {
        viewModelScope.launch {
            repository.insertFoto(
                FotoEvidencia(
                    trabajoId = trabajoId,
                    rutaArchivo = ruta
                )
            )
            _snackbarEvent.emit("Foto de evidencia guardada")
        }
    }

    fun eliminarTrabajo(trabajoId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            val t = repository.getTrabajoByIdOnce(trabajoId)
            if (t != null) {
                repository.deleteTrabajo(t)
                _snackbarEvent.emit("Orden eliminada")
                onDone()
            }
        }
    }

    fun guardarRecepcion(
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
        repuestos: List<Pair<String, Double>>,
        garantiaDias: Int,
        accesoriosDejados: String,
        notas: String,
        fotos: List<String>,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            if (clienteNombre.isBlank() && clienteExistenteId == null) {
                _snackbarEvent.emit("El nombre del cliente es obligatorio")
                return@launch
            }
            if (biciMarca.isBlank() && bicicletaExistenteId == null) {
                _snackbarEvent.emit("La marca de la bicicleta es obligatoria")
                return@launch
            }
            if (descripcionFalla.isBlank()) {
                _snackbarEvent.emit("Por favor describa la falla o trabajo solicitado")
                return@launch
            }

            try {
                val id = repository.crearRecepcionCompleta(
                    clienteExistenteId = clienteExistenteId,
                    clienteNombre = clienteNombre,
                    clienteTelefono = clienteTelefono,
                    clienteEmail = clienteEmail,
                    bicicletaExistenteId = bicicletaExistenteId,
                    biciMarca = biciMarca,
                    biciModelo = biciModelo,
                    biciColor = biciColor,
                    biciNumeroSerie = biciNumeroSerie,
                    biciTipo = biciTipo,
                    descripcionFalla = descripcionFalla,
                    costoManoObra = costoManoObra,
                    repuestosIniciales = repuestos,
                    garantiaDias = garantiaDias,
                    accesoriosDejados = accesoriosDejados,
                    notas = notas,
                    fotosRutas = fotos
                )
                _snackbarEvent.emit("¡Recepción registrada exitosamente!")
                onSuccess(id)
            } catch (e: Exception) {
                _snackbarEvent.emit("Error al guardar: ${e.message}")
            }
        }
    }
}
