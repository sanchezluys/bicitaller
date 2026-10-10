package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.model.Cliente
import com.example.ui.components.LargeTactileButton
import com.example.ui.viewmodel.BiciTallerViewModel
import com.example.util.ContactsHelper
import com.example.util.DocumentGenerator
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NuevaRecepcionScreen(
    viewModel: BiciTallerViewModel,
    onBack: () -> Unit,
    onRecepcionCreada: (Long) -> Unit
) {
    val context = LocalContext.current
    val clientesExistentes by viewModel.clientes.collectAsStateWithLifecycle()

    // Form State - Cliente
    var clienteNombre by remember { mutableStateOf("") }
    var clienteTelefono by remember { mutableStateOf("") }
    var clienteEmail by remember { mutableStateOf("") }
    var clienteSeleccionadoId by remember { mutableStateOf<Long?>(null) }
    var expandedClientesDropdown by remember { mutableStateOf(false) }

    // Form State - Bicicleta
    var biciMarca by remember { mutableStateOf("") }
    var biciModelo by remember { mutableStateOf("") }
    var biciColor by remember { mutableStateOf("") }
    var biciNumeroSerie by remember { mutableStateOf("") }
    var biciTipo by remember { mutableStateOf("MTB") }

    // Form State - Accesorios
    val listaAccesorios = listOf(
        "Luces (del/tras)", "Casco", "Bomba / Inflador", "Candado / Llave",
        "Ciclocomputador", "Bolsa herramientas", "Guardabarros", "Pedales automáticos"
    )
    val accesoriosSeleccionados = remember { mutableStateListOf<String>() }

    // Form State - Falla
    var descripcionFalla by remember { mutableStateOf("") }
    val fallasComunes = listOf(
        "Mantenimiento General", "Ajuste cambios y frenos", "Purga de frenos hidráulicos",
        "Cambio cadena / piñón", "Centrado de rueda", "Pinchazo / cambio neumático",
        "Ruido pedalier / caja", "Mantenimiento suspensión"
    )

    // Form State - Presupuesto preliminar
    var costoManoObraStr by remember { mutableStateOf("") }
    val repuestosPreliminares = remember { mutableStateListOf<Pair<String, Double>>() }
    var nuevoRepuestoNombre by remember { mutableStateOf("") }
    var nuevoRepuestoCostoStr by remember { mutableStateOf("") }
    var garantiaDias by remember { mutableIntStateOf(30) }
    var notasInternas by remember { mutableStateOf("") }

    // Form State - Fotos de evidencia
    val fotosCapturadas = remember { mutableStateListOf<String>() }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            fotosCapturadas.add(tempCameraUri.toString())
        }
    }

    // Permission launcher for Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "bici_intake_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Contact Picker Launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            val info = ContactsHelper.retrieveContact(context, uri)
            if (info != null) {
                clienteNombre = info.name
                clienteTelefono = info.phone
                clienteSeleccionadoId = null
            }
        }
    }

    // Contact Permission Launcher
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        contactPickerLauncher.launch(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nueva Recepción",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // STEP 1: CLIENTE
            SectionCard(title = "1. Datos del Cliente", icon = Icons.Default.Person) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Desde Contactos", fontSize = 13.sp)
                    }

                    if (clientesExistentes.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expandedClientesDropdown,
                            onExpandedChange = { expandedClientesDropdown = !expandedClientesDropdown },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedButton(
                                onClick = { expandedClientesDropdown = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            ) {
                                Text("Cliente Guardado", fontSize = 13.sp)
                            }
                            ExposedDropdownMenu(
                                expanded = expandedClientesDropdown,
                                onDismissRequest = { expandedClientesDropdown = false }
                            ) {
                                clientesExistentes.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.nombre} (${c.telefono})") },
                                        onClick = {
                                            clienteNombre = c.nombre
                                            clienteTelefono = c.telefono
                                            clienteEmail = c.email
                                            clienteSeleccionadoId = c.id
                                            expandedClientesDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = clienteNombre,
                    onValueChange = {
                        clienteNombre = it
                        clienteSeleccionadoId = null
                    },
                    label = { Text("Nombre Completo *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_cliente_nombre")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clienteTelefono,
                    onValueChange = { clienteTelefono = it },
                    label = { Text("Teléfono / WhatsApp *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_cliente_telefono")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clienteEmail,
                    onValueChange = { clienteEmail = it },
                    label = { Text("Correo Electrónico (Opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // STEP 2: BICICLETA
            SectionCard(title = "2. Datos de la Bicicleta", icon = Icons.Default.DirectionsBike) {
                Text(
                    text = "Marcas Frecuentes:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    val marcas = listOf("Trek", "Specialized", "Giant", "Scott", "Orbea", "GW", "Shimano", "Cannondale", "Venzo")
                    marcas.forEach { marca ->
                        SuggestionChip(
                            onClick = { biciMarca = marca },
                            label = { Text(marca) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = biciMarca,
                        onValueChange = { biciMarca = it },
                        label = { Text("Marca *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_bici_marca")
                    )
                    OutlinedTextField(
                        value = biciModelo,
                        onValueChange = { biciModelo = it },
                        label = { Text("Modelo") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_bici_modelo")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = biciColor,
                        onValueChange = { biciColor = it },
                        label = { Text("Color") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Tipo Selector Chips in Dropdown / Row
                    val tipos = listOf("MTB", "Ruta", "Urbana", "Gravel", "E-Bike", "BMX")
                    var expandedTipo by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedTipo,
                        onExpandedChange = { expandedTipo = !expandedTipo },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = biciTipo,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTipo,
                            onDismissRequest = { expandedTipo = false }
                        ) {
                            tipos.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text(tipo) },
                                    onClick = {
                                        biciTipo = tipo
                                        expandedTipo = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = biciNumeroSerie,
                        onValueChange = { biciNumeroSerie = it },
                        label = { Text("Número de Serie (Cuadro)") },
                        placeholder = { Text("Ej: SN-92842") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_bici_serial")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            val randomNum = Random.nextInt(10000, 99999)
                            biciNumeroSerie = "BT-$randomNum"
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Auto N°", fontSize = 12.sp)
                    }
                }
            }

            // STEP 3: ESTADO INICIAL Y ACCESORIOS
            SectionCard(title = "3. Accesorios y Estado Inicial", icon = Icons.Default.Security) {
                Text(
                    text = "Accesorios que deja el cliente:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    listaAccesorios.forEach { acc ->
                        val isSelected = accesoriosSeleccionados.contains(acc)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) accesoriosSeleccionados.remove(acc)
                                else accesoriosSeleccionados.add(acc)
                            },
                            label = { Text(acc, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Fallas o Trabajos Frecuentes:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    fallasComunes.forEach { falla ->
                        SuggestionChip(
                            onClick = {
                                descripcionFalla = if (descripcionFalla.isBlank()) falla else "$descripcionFalla + $falla"
                            },
                            label = { Text(falla, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = descripcionFalla,
                    onValueChange = { descripcionFalla = it },
                    label = { Text("Descripción Detallada de Falla o Servicio *") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_descripcion_falla")
                )
            }

            // STEP 4: EVIDENCIA FOTOGRÁFICA
            SectionCard(title = "4. Fotos de Evidencia", icon = Icons.Default.CameraAlt) {
                Text(
                    text = "Toma fotos del estado en que ingresa la bicicleta (rayones, accesorios, transmisión):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LargeTactileButton(
                        text = "Tomar Foto",
                        icon = Icons.Default.CameraAlt,
                        onClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "button_take_photo"
                    )
                }

                if (fotosCapturadas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(fotosCapturadas) { uriStr ->
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = Uri.parse(uriStr),
                                    contentDescription = "Foto evidencia",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { fotosCapturadas.remove(uriStr) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(26.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // STEP 5: PRESUPUESTO ESTIMADO
            SectionCard(title = "5. Presupuesto Preliminar", icon = Icons.Default.Security) {
                OutlinedTextField(
                    value = costoManoObraStr,
                    onValueChange = { costoManoObraStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Costo Estimado Mano de Obra ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_mano_obra_estimada")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Repuestos o Insumos Preliminares:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nuevoRepuestoNombre,
                        onValueChange = { nuevoRepuestoNombre = it },
                        label = { Text("Repuesto") },
                        placeholder = { Text("Ej: Cadena 9v") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = nuevoRepuestoCostoStr,
                        onValueChange = { nuevoRepuestoCostoStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Valor") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val cost = nuevoRepuestoCostoStr.toDoubleOrNull() ?: 0.0
                            if (nuevoRepuestoNombre.isNotBlank() && cost >= 0) {
                                repuestosPreliminares.add(Pair(nuevoRepuestoNombre.trim(), cost))
                                nuevoRepuestoNombre = ""
                                nuevoRepuestoCostoStr = ""
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                            .size(46.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                if (repuestosPreliminares.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    repuestosPreliminares.forEachIndexed { idx, pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "• ${pair.first}: ${DocumentGenerator.formatMoney(pair.second)}", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { repuestosPreliminares.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Quitar", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Días de Garantía selector
                Text(
                    text = "Días de Garantía:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    listOf(15, 30, 60, 90).forEach { dias ->
                        FilterChip(
                            selected = garantiaDias == dias,
                            onClick = { garantiaDias = dias },
                            label = { Text("$dias días") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notasInternas,
                    onValueChange = { notasInternas = it },
                    label = { Text("Notas Internas del Taller") },
                    placeholder = { Text("Ej: Cliente viaja el fin de semana, avisar por WhatsApp") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // SUBMIT BUTTON
            LargeTactileButton(
                text = "Guardar y Crear Orden",
                icon = Icons.Default.Check,
                onClick = {
                    val manoObra = costoManoObraStr.toDoubleOrNull() ?: 0.0
                    val accesoriosStr = accesoriosSeleccionados.joinToString(", ")

                    viewModel.guardarRecepcion(
                        clienteExistenteId = clienteSeleccionadoId,
                        clienteNombre = clienteNombre,
                        clienteTelefono = clienteTelefono,
                        clienteEmail = clienteEmail,
                        bicicletaExistenteId = null,
                        biciMarca = biciMarca,
                        biciModelo = biciModelo,
                        biciColor = biciColor,
                        biciNumeroSerie = biciNumeroSerie,
                        biciTipo = biciTipo,
                        descripcionFalla = descripcionFalla,
                        costoManoObra = manoObra,
                        repuestos = repuestosPreliminares.toList(),
                        garantiaDias = garantiaDias,
                        accesoriosDejados = accesoriosStr,
                        notas = notasInternas,
                        fotos = fotosCapturadas.toList(),
                        onSuccess = { newId ->
                            onRecepcionCreada(newId)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("button_guardar_recepcion")
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            content()
        }
    }
}
