package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.dao.TrabajoJoinItem
import com.example.data.model.Bicicleta
import com.example.data.model.Cliente
import com.example.data.model.EstadoPago
import com.example.data.model.EstadoTrabajo
import com.example.data.model.RepuestoUsado
import com.example.data.model.Trabajo
import com.example.ui.components.EstadoPagoBadge
import com.example.ui.components.EstadoTrabajoBadge
import com.example.ui.components.GarantiaBadge
import com.example.ui.components.LargeTactileButton
import com.example.ui.theme.GarantiaActive
import com.example.ui.theme.PagoPendiente
import com.example.ui.viewmodel.BiciTallerViewModel
import com.example.util.DocumentGenerator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleTrabajoScreen(
    trabajoId: Long,
    viewModel: BiciTallerViewModel,
    onBack: () -> Unit,
    onNavigateToDocumento: (Long) -> Unit,
    onNavigateToHistorial: (Long) -> Unit
) {
    val context = LocalContext.current
    val trabajo by viewModel.getTrabajoDetail(trabajoId).collectAsStateWithLifecycle()
    val repuestos by viewModel.getRepuestosForTrabajo(trabajoId).collectAsStateWithLifecycle()
    val fotos by viewModel.getFotosForTrabajo(trabajoId).collectAsStateWithLifecycle()

    var cliente by remember { mutableStateOf<Cliente?>(null) }
    var bicicleta by remember { mutableStateOf<Bicicleta?>(null) }
    var historialBici by remember { mutableStateOf<List<Trabajo>>(emptyList()) }

    // Dialog States
    var showAddRepuestoDialog by remember { mutableStateOf(false) }
    var showAbonoDialog by remember { mutableStateOf(false) }
    var showEditManoObraDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showGarantiaDialog by remember { mutableStateOf(false) }

    LaunchedEffect(trabajo) {
        val t = trabajo ?: return@LaunchedEffect
        val b = viewModel.getBicicletaByIdOnce(t.bicicletaId)
        bicicleta = b
        if (b != null) {
            cliente = viewModel.getClienteByIdOnce(b.clienteId)
        }
    }

    // Camera launcher for new photo
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.agregarFotoEvidencia(trabajoId, tempCameraUri.toString())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "bici_evidencia_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Orden #$trabajoId",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToDocumento(trabajoId) },
                        modifier = Modifier.testTag("action_view_ticket")
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = "Ver Ticket / Presupuesto")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = PagoPendiente)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        val currentTrabajo = trabajo
        if (currentTrabajo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando información...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. STATUS PROGRESSION PIPELINE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estado de la Reparación",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            EstadoTrabajoBadge(estado = currentTrabajo.estadoTrabajo)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Flow Buttons for Stages
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(EstadoTrabajo.values()) { estado ->
                                val isSelected = currentTrabajo.estadoTrabajo == estado
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.cambiarEstado(trabajoId, estado)
                                    },
                                    label = { Text(estado.titulo, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // 2. WARRANTY BADGE / ALERT
                if (currentTrabajo.isGarantiaActiva()) {
                    Surface(
                        color = GarantiaActive.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GarantiaActive)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = GarantiaActive,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Garantía de Servicio Activa",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Vigente por ${currentTrabajo.garantiaDias} días. Vence el: ${DocumentGenerator.formatDate(currentTrabajo.fechaFinGarantia ?: 0)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showGarantiaDialog = true }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar garantía", tint = GarantiaActive)
                            }
                        }
                    }
                }

                // 3. CUSTOMER CARD
                cliente?.let { cl ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cl.nombre,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = cl.telefono,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cl.telefono}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Llamar", fontSize = 13.sp)
                                }

                                LargeTactileButton(
                                    text = "WhatsApp",
                                    icon = Icons.Default.Share,
                                    onClick = {
                                        bicicleta?.let { b ->
                                            val joinItem = TrabajoJoinItem(
                                                trabajoId = currentTrabajo.id,
                                                bicicletaId = b.id,
                                                fechaIngreso = currentTrabajo.fechaIngreso,
                                                fechaEntrega = currentTrabajo.fechaEntrega,
                                                descripcionFalla = currentTrabajo.descripcionFalla,
                                                estadoTrabajo = currentTrabajo.estadoTrabajo.name,
                                                estadoPago = currentTrabajo.estadoPago.name,
                                                costoManoObra = currentTrabajo.costoManoObra,
                                                costoTotal = currentTrabajo.costoTotal,
                                                montoAbonado = currentTrabajo.montoAbonado,
                                                garantiaDias = currentTrabajo.garantiaDias,
                                                accesoriosDejados = currentTrabajo.accesoriosDejados,
                                                notas = currentTrabajo.notas,
                                                biciMarca = b.marca,
                                                biciModelo = b.modelo,
                                                biciColor = b.color,
                                                biciNumeroSerie = b.numeroSerie,
                                                biciTipo = b.tipo,
                                                clienteId = cl.id,
                                                clienteNombre = cl.nombre,
                                                clienteTelefono = cl.telefono,
                                                clienteEmail = cl.email
                                            )
                                            val esPres = currentTrabajo.estadoTrabajo == EstadoTrabajo.PRESUPUESTO_GENERADO
                                            val msg = DocumentGenerator.createWhatsAppMessage(joinItem, repuestos, esPres)
                                            DocumentGenerator.shareViaWhatsApp(context, cl.telefono, msg)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    containerColor = Color(0xFF25D366),
                                    contentColor = Color.White
                                )
                            }
                        }
                    }
                }

                // 4. BICYCLE CARD & HISTORY LINK
                bicicleta?.let { b ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.DirectionsBike, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${b.marca} ${b.modelo}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = b.tipo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Color: ${b.color} • N° Serie: ${if (b.numeroSerie.isNotBlank()) b.numeroSerie else "Sin registro"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { onNavigateToHistorial(b.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ver Hoja de Vida / Historial de la Bici", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 5. DIAGNÓSTICO & ACCESORIOS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Diagnóstico / Falla Reportada",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentTrabajo.descripcionFalla,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (currentTrabajo.accesoriosDejados.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Accesorios recibidos: ${currentTrabajo.accesoriosDejados}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (currentTrabajo.notas.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Notas: ${currentTrabajo.notas}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 6. REPUESTOS E INSUMOS UTILIZADOS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Repuestos Utilizados",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            OutlinedButton(
                                onClick = { showAddRepuestoDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Agregar", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (repuestos.isEmpty()) {
                            Text(
                                text = "No se han agregado repuestos específicos a esta orden.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            repuestos.forEach { r ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${r.nombreDescripcion} (x${r.cantidad})",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "Unitario: ${DocumentGenerator.formatMoney(r.costoUnitario)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = DocumentGenerator.formatMoney(r.subtotal),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    IconButton(
                                        onClick = { viewModel.eliminarRepuesto(r) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = PagoPendiente, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. FINANZAS Y CONTROL DE PAGOS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Finanzas y Liquidación",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            EstadoPagoBadge(estado = currentTrabajo.estadoPago)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Mano de Obra:", style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = DocumentGenerator.formatMoney(currentTrabajo.costoManoObra),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                IconButton(onClick = { showEditManoObraDialog = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        val sumRepuestos = repuestos.sumOf { it.subtotal }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Repuestos:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = DocumentGenerator.formatMoney(sumRepuestos),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "COSTO TOTAL:",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = DocumentGenerator.formatMoney(currentTrabajo.costoTotal),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Monto Abonado:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = DocumentGenerator.formatMoney(currentTrabajo.montoAbonado),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Saldo Pendiente:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = DocumentGenerator.formatMoney(currentTrabajo.saldoPendiente),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentTrabajo.saldoPendiente > 0) PagoPendiente else Color(0xFF10B981)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LargeTactileButton(
                            text = "Registrar o Ajustar Abono",
                            icon = Icons.Default.MonetizationOn,
                            onClick = { showAbonoDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "button_registrar_abono"
                        )
                    }
                }

                // 8. FOTOS DE EVIDENCIA
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fotos de Evidencia",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            OutlinedButton(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Añadir", fontSize = 12.sp)
                            }
                        }

                        if (fotos.isEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No se han capturado fotos para esta orden.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(fotos) { f ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                    ) {
                                        AsyncImage(
                                            model = Uri.parse(f.rutaArchivo),
                                            contentDescription = "Evidencia",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 9. PRIMARY ACTION: GENERATE DOCUMENT / TICKET
                LargeTactileButton(
                    text = "Ver y Generar Ticket / Presupuesto",
                    icon = Icons.Default.Description,
                    onClick = { onNavigateToDocumento(trabajoId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("button_generar_documento")
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // DIALOG: Agregar Repuesto
    if (showAddRepuestoDialog) {
        var nombre by remember { mutableStateOf("") }
        var costoStr by remember { mutableStateOf("") }
        var cantidadStr by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showAddRepuestoDialog = false },
            title = { Text("Agregar Repuesto / Insumo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del Repuesto") },
                        placeholder = { Text("Ej: Pastillas de freno") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = costoStr,
                        onValueChange = { costoStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Costo Unitario ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cantidadStr,
                        onValueChange = { cantidadStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cost = costoStr.toDoubleOrNull() ?: 0.0
                        val cant = cantidadStr.toIntOrNull() ?: 1
                        if (nombre.isNotBlank() && cost >= 0) {
                            viewModel.agregarRepuesto(trabajoId, nombre, cost, cant)
                            showAddRepuestoDialog = false
                        }
                    }
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRepuestoDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIALOG: Registrar Abono
    if (showAbonoDialog) {
        var abonoStr by remember { mutableStateOf(trabajo?.montoAbonado?.toString() ?: "0.0") }

        AlertDialog(
            onDismissRequest = { showAbonoDialog = false },
            title = { Text("Registrar Monto Abonado") },
            text = {
                Column {
                    Text(
                        text = "Total de la orden: ${DocumentGenerator.formatMoney(trabajo?.costoTotal ?: 0.0)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = abonoStr,
                        onValueChange = { abonoStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Monto Abonado ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { abonoStr = (trabajo?.costoTotal ?: 0.0).toString() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Total Pagado", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val abono = abonoStr.toDoubleOrNull() ?: 0.0
                        viewModel.registrarAbono(trabajoId, abono)
                        showAbonoDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbonoDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIALOG: Editar Mano de Obra
    if (showEditManoObraDialog) {
        var moStr by remember { mutableStateOf(trabajo?.costoManoObra?.toString() ?: "0.0") }

        AlertDialog(
            onDismissRequest = { showEditManoObraDialog = false },
            title = { Text("Actualizar Mano de Obra") },
            text = {
                OutlinedTextField(
                    value = moStr,
                    onValueChange = { moStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Costo Mano de Obra ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mo = moStr.toDoubleOrNull() ?: 0.0
                        viewModel.actualizarManoObra(trabajoId, mo)
                        showEditManoObraDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditManoObraDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIALOG: Editar Garantía
    if (showGarantiaDialog) {
        var dias by remember { mutableStateOf(trabajo?.garantiaDias?.toString() ?: "30") }

        AlertDialog(
            onDismissRequest = { showGarantiaDialog = false },
            title = { Text("Días de Garantía") },
            text = {
                OutlinedTextField(
                    value = dias,
                    onValueChange = { dias = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Días de garantía") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val d = dias.toIntOrNull() ?: 30
                        viewModel.actualizarGarantia(trabajoId, d)
                        showGarantiaDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGarantiaDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIALOG: Confirmar Eliminación
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("¿Eliminar Orden #$trabajoId?") },
            text = { Text("Esta acción no se puede deshacer. Se eliminarán los datos de repuestos y evidencia vinculados.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.eliminarTrabajo(trabajoId, onDone = onBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PagoPendiente)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
