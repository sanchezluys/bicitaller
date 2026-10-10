package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.dao.TrabajoJoinItem
import com.example.data.model.Bicicleta
import com.example.data.model.Cliente
import com.example.data.model.EstadoTrabajo
import com.example.data.model.RepuestoUsado
import com.example.data.model.Trabajo
import com.example.ui.components.LargeTactileButton
import com.example.ui.theme.BiciOrangePrimary
import com.example.ui.theme.PagoPendiente
import com.example.ui.viewmodel.BiciTallerViewModel
import com.example.util.DocumentGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentoTicketScreen(
    trabajoId: Long,
    viewModel: BiciTallerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val trabajo by viewModel.getTrabajoDetail(trabajoId).collectAsStateWithLifecycle()
    val repuestos by viewModel.getRepuestosForTrabajo(trabajoId).collectAsStateWithLifecycle()

    var cliente by remember { mutableStateOf<Cliente?>(null) }
    var bicicleta by remember { mutableStateOf<Bicicleta?>(null) }
    var esPresupuesto by remember { mutableStateOf(false) }
    var tallerNombre by remember { mutableStateOf("BiciTaller") }

    LaunchedEffect(trabajo) {
        val t = trabajo ?: return@LaunchedEffect
        esPresupuesto = (t.estadoTrabajo == EstadoTrabajo.PRESUPUESTO_GENERADO)
        val b = viewModel.getBicicletaByIdOnce(t.bicicletaId)
        bicicleta = b
        if (b != null) {
            cliente = viewModel.getClienteByIdOnce(b.clienteId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (esPresupuesto) "Presupuesto #$trabajoId" else "Ticket de Entrega #$trabajoId",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        val currentTrabajo = trabajo
        val currentBici = bicicleta
        val currentCliente = cliente

        if (currentTrabajo == null || currentBici == null || currentCliente == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando documento...")
            }
        } else {
            val joinItem = TrabajoJoinItem(
                trabajoId = currentTrabajo.id,
                bicicletaId = currentBici.id,
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
                biciMarca = currentBici.marca,
                biciModelo = currentBici.modelo,
                biciColor = currentBici.color,
                biciNumeroSerie = currentBici.numeroSerie,
                biciTipo = currentBici.tipo,
                clienteId = currentCliente.id,
                clienteNombre = currentCliente.nombre,
                clienteTelefono = currentCliente.telefono,
                clienteEmail = currentCliente.email
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Document Type Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !esPresupuesto,
                        onClick = { esPresupuesto = false },
                        label = { Text("Ticket Final / Recibo", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = esPresupuesto,
                        onClick = { esPresupuesto = true },
                        label = { Text("Presupuesto", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Workshop Name field (editable for branding)
                OutlinedTextField(
                    value = tallerNombre,
                    onValueChange = { tallerNombre = it },
                    label = { Text("Nombre o Marca de su Taller") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // LIVE DOCUMENT PREVIEW CARD (Invoice style)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBike,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tallerNombre,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                Text(
                                    text = if (esPresupuesto) "PRESUPUESTO ESTIMADO" else "TICKET DE SERVICIO Y ENTREGA",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "ORDEN #${currentTrabajo.id}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = DocumentGenerator.formatDate(System.currentTimeMillis()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Client and Bike block
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "CLIENTE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                Text(text = currentCliente.nombre, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(text = currentCliente.telefono, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "BICICLETA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                Text(text = "${currentBici.marca} ${currentBici.modelo}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(text = "${currentBici.color} • ${if (currentBici.numeroSerie.isNotBlank()) "SN: " + currentBici.numeroSerie else "Sin serial"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Diagnosis
                        Text(text = "DIAGNÓSTICO:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(text = currentTrabajo.descripcionFalla, style = MaterialTheme.typography.bodySmall)

                        if (currentTrabajo.accesoriosDejados.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Accesorios recibidos: ${currentTrabajo.accesoriosDejados}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Cost Breakdown
                        Text(text = "DESGLOSE DE SERVICIOS Y REPUESTOS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))

                        repuestos.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "• ${r.nombreDescripcion} x${r.cantidad}", style = MaterialTheme.typography.bodySmall)
                                Text(text = DocumentGenerator.formatMoney(r.subtotal), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "• Mano de obra calificada", style = MaterialTheme.typography.bodySmall)
                            Text(text = DocumentGenerator.formatMoney(currentTrabajo.costoManoObra), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Total and Payment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "TOTAL:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                            Text(
                                text = DocumentGenerator.formatMoney(currentTrabajo.costoTotal),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            )
                        }

                        if (!esPresupuesto) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Abonado:", style = MaterialTheme.typography.bodySmall)
                                Text(text = DocumentGenerator.formatMoney(currentTrabajo.montoAbonado), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Saldo Pendiente:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = DocumentGenerator.formatMoney(currentTrabajo.saldoPendiente),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = if (currentTrabajo.saldoPendiente > 0) PagoPendiente else Color(0xFF10B981))
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🛡️ Términos de garantía: ${currentTrabajo.garantiaDias} días por mano de obra y repuestos instalados.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ACTION BUTTONS
                LargeTactileButton(
                    text = "Compartir por WhatsApp",
                    icon = Icons.Default.Share,
                    onClick = {
                        val msg = DocumentGenerator.createWhatsAppMessage(joinItem, repuestos, esPresupuesto, tallerNombre)
                        DocumentGenerator.shareViaWhatsApp(context, currentCliente.telefono, msg)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White,
                    testTag = "button_share_whatsapp"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LargeTactileButton(
                        text = "Guardar PDF",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                            try {
                                val file = DocumentGenerator.generatePdfDocument(context, joinItem, repuestos, esPresupuesto, tallerNombre)
                                DocumentGenerator.shareFile(context, file, "application/pdf", "Compartir Presupuesto o Ticket PDF")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "button_export_pdf"
                    )

                    LargeTactileButton(
                        text = "Guardar Imagen",
                        icon = Icons.Default.Image,
                        onClick = {
                            try {
                                val bmp = DocumentGenerator.generateTicketBitmap(joinItem, repuestos, esPresupuesto, tallerNombre)
                                val file = DocumentGenerator.saveBitmapToFile(context, bmp, "Ticket_Bici_${currentTrabajo.id}")
                                DocumentGenerator.shareFile(context, file, "image/png", "Compartir Imagen del Ticket")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al generar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "button_export_image"
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
