package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.dao.TrabajoJoinItem
import com.example.data.model.EstadoPago
import com.example.data.model.EstadoTrabajo
import com.example.data.model.RepuestoUsado
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentGenerator {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun formatMoney(amount: Double): String {
        return try {
            currencyFormat.format(amount)
        } catch (_: Exception) {
            "$$amount"
        }
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Generates a WhatsApp formatted string message ready to send
     */
    fun createWhatsAppMessage(
        item: TrabajoJoinItem,
        repuestos: List<RepuestoUsado>,
        esPresupuesto: Boolean,
        nombreTaller: String = "BiciTaller Especializado"
    ): String {
        val sb = StringBuilder()
        val docTipo = if (esPresupuesto) "📋 *PRESUPUESTO DE REPARACIÓN*" else "🧾 *TICKET DE SERVICIO Y ENTREGA*"

        sb.append("🚲 *$nombreTaller*\n")
        sb.append("$docTipo\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("👤 *Cliente:* ${item.clienteNombre}\n")
        sb.append("📱 *Teléfono:* ${item.clienteTelefono}\n")
        sb.append("🚲 *Bicicleta:* ${item.biciMarca} ${item.biciModelo} (${item.biciColor})\n")
        if (item.biciNumeroSerie.isNotBlank()) {
            sb.append("🔢 *N° Serie:* ${item.biciNumeroSerie}\n")
        }
        sb.append("📅 *Fecha Ingreso:* ${formatDate(item.fechaIngreso)}\n")
        sb.append("⚠️ *Diagnóstico / Falla:* ${item.descripcionFalla}\n")
        if (item.accesoriosDejados.isNotBlank()) {
            sb.append("🎒 *Accesorios dejados:* ${item.accesoriosDejados}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🔧 *DESGLOSE DE COSTOS*\n")

        if (repuestos.isNotEmpty()) {
            sb.append("⚙️ *Repuestos e insumos:*\n")
            for (r in repuestos) {
                sb.append("  • ${r.nombreDescripcion} x${r.cantidad}: ${formatMoney(r.subtotal)}\n")
            }
        }

        sb.append("🛠️ *Mano de obra:* ${formatMoney(item.costoManoObra)}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💰 *TOTAL:* ${formatMoney(item.costoTotal)}\n")
        if (!esPresupuesto) {
            sb.append("💵 *Abonado:* ${formatMoney(item.montoAbonado)}\n")
            val saldo = (item.costoTotal - item.montoAbonado).coerceAtLeast(0.0)
            sb.append("📌 *Saldo Pendiente:* ${formatMoney(saldo)}\n")
            sb.append("📊 *Estado de Pago:* ${item.estadoPago}\n")
            sb.append("🛡️ *Garantía del trabajo:* ${item.garantiaDias} días\n")
        }

        sb.append("\n_¡Gracias por confiar en $nombreTaller!_")
        return sb.toString()
    }

    /**
     * Share text directly via WhatsApp or standard share intent
     */
    fun shareViaWhatsApp(context: Context, phone: String, message: String) {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to standard chooser
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Enviar documento"))
        }
    }

    /**
     * Generates a clean A4 PDF file using Android's native PdfDocument
     */
    fun generatePdfDocument(
        context: Context,
        item: TrabajoJoinItem,
        repuestos: List<RepuestoUsado>,
        esPresupuesto: Boolean,
        nombreTaller: String = "BiciTaller Especializado"
    ): File {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            color = Color.rgb(255, 109, 0) // Amber orange
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = Color.rgb(15, 23, 42)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = Color.rgb(30, 41, 59)
        }
        val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
            color = Color.rgb(15, 23, 42)
        }
        val linePaint = Paint().apply {
            strokeWidth = 1f
            color = Color.rgb(203, 213, 225)
        }
        val boxPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
        }

        var y = 45f

        // Top Banner / Header
        canvas.drawText(nombreTaller, 40f, y, titlePaint)
        y += 20f
        val docTitulo = if (esPresupuesto) "PRESUPUESTO DE SERVICIO #${item.trabajoId}" else "ORDEN / TICKET FINAL #${item.trabajoId}"
        canvas.drawText(docTitulo, 40f, y, headerPaint)
        canvas.drawText("Fecha: ${formatDate(System.currentTimeMillis())}", 380f, y, textPaint)
        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Customer & Bike Info Box
        canvas.drawRoundRect(40f, y, 555f, y + 105f, 8f, 8f, boxPaint)
        val boxY = y + 20f
        canvas.drawText("DATOS DEL CLIENTE", 55f, boxY, boldTextPaint)
        canvas.drawText("DATOS DE LA BICICLETA", 310f, boxY, boldTextPaint)

        canvas.drawText("Nombre: ${item.clienteNombre}", 55f, boxY + 18f, textPaint)
        canvas.drawText("Teléfono: ${item.clienteTelefono}", 55f, boxY + 34f, textPaint)
        if (item.clienteEmail.isNotBlank()) {
            canvas.drawText("Email: ${item.clienteEmail}", 55f, boxY + 50f, textPaint)
        }

        canvas.drawText("Bicicleta: ${item.biciMarca} ${item.biciModelo}", 310f, boxY + 18f, textPaint)
        canvas.drawText("Color: ${item.biciColor} | Tipo: ${item.biciTipo}", 310f, boxY + 34f, textPaint)
        val serialText = if (item.biciNumeroSerie.isNotBlank()) item.biciNumeroSerie else "No especificado"
        canvas.drawText("N° Serie: $serialText", 310f, boxY + 50f, textPaint)
        canvas.drawText("Estado Orden: ${item.estadoTrabajo}", 310f, boxY + 66f, textPaint)

        y += 125f

        // Fault Description & Accessories
        canvas.drawText("DIAGNÓSTICO Y DESCRIPCIÓN DE FALLA:", 40f, y, boldTextPaint)
        y += 16f
        canvas.drawText(item.descripcionFalla, 40f, y, textPaint)
        y += 20f

        if (item.accesoriosDejados.isNotBlank()) {
            canvas.drawText("Accesorios dejados: ${item.accesoriosDejados}", 40f, y, textPaint)
            y += 20f
        }

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Items Table Header
        canvas.drawText("CONCEPTO / REPUESTO", 40f, y, boldTextPaint)
        canvas.drawText("CANT.", 340f, y, boldTextPaint)
        canvas.drawText("VALOR UNIT.", 410f, y, boldTextPaint)
        canvas.drawText("SUBTOTAL", 495f, y, boldTextPaint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f

        // Spare Parts Items
        for (r in repuestos) {
            canvas.drawText(r.nombreDescripcion, 40f, y, textPaint)
            canvas.drawText("${r.cantidad}", 350f, y, textPaint)
            canvas.drawText(formatMoney(r.costoUnitario), 410f, y, textPaint)
            canvas.drawText(formatMoney(r.subtotal), 495f, y, textPaint)
            y += 18f
        }

        // Labor Cost
        canvas.drawText("Mano de Obra Especializada", 40f, y, textPaint)
        canvas.drawText("1", 350f, y, textPaint)
        canvas.drawText(formatMoney(item.costoManoObra), 410f, y, textPaint)
        canvas.drawText(formatMoney(item.costoManoObra), 495f, y, textPaint)
        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Totals Box
        canvas.drawText("COSTO TOTAL:", 370f, y, boldTextPaint)
        canvas.drawText(formatMoney(item.costoTotal), 485f, y, boldTextPaint)
        y += 20f

        if (!esPresupuesto) {
            canvas.drawText("Monto Abonado:", 370f, y, textPaint)
            canvas.drawText(formatMoney(item.montoAbonado), 485f, y, textPaint)
            y += 18f
            val saldo = (item.costoTotal - item.montoAbonado).coerceAtLeast(0.0)
            canvas.drawText("Saldo Pendiente:", 370f, y, boldTextPaint)
            canvas.drawText(formatMoney(saldo), 485f, y, boldTextPaint)
            y += 25f
        }

        // Terms and Warranty Footer
        y = (y + 10f).coerceAtLeast(650f)
        canvas.drawRoundRect(40f, y, 555f, y + 80f, 6f, 6f, boxPaint)
        canvas.drawText("TÉRMINOS Y CONDICIONES DE GARANTÍA", 55f, y + 20f, boldTextPaint)
        canvas.drawText("• Este trabajo cuenta con garantía de ${item.garantiaDias} días calendario a partir de su entrega.", 55f, y + 36f, textPaint)
        canvas.drawText("• No cubre daños por mal uso, accidentes posteriores o manipulaciones de terceros.", 55f, y + 50f, textPaint)
        canvas.drawText("• Indispensable presentar este ticket o número de serie de la bicicleta para cualquier reclamo.", 55f, y + 64f, textPaint)

        pdf.finishPage(page)

        val file = File(context.cacheDir, "BiciTaller_${item.trabajoId}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdf.writeTo(out)
        }
        pdf.close()
        return file
    }

    /**
     * Generates a thermal-style ticket image for easy sharing via photo apps or WhatsApp
     */
    fun generateTicketBitmap(
        item: TrabajoJoinItem,
        repuestos: List<RepuestoUsado>,
        esPresupuesto: Boolean,
        nombreTaller: String = "BiciTaller"
    ): Bitmap {
        val width = 600
        val height = 800 + (repuestos.size * 30)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 28f
            color = Color.rgb(255, 109, 0)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            color = Color.BLACK
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.BLACK
        }
        val linePaint = Paint().apply {
            strokeWidth = 2f
            color = Color.LTGRAY
        }

        var y = 50f
        canvas.drawText("🚲 $nombreTaller", width / 2f, y, headerPaint)
        y += 30f
        val docTipo = if (esPresupuesto) "PRESUPUESTO #${item.trabajoId}" else "TICKET DE SERVICIO #${item.trabajoId}"
        canvas.drawText(docTipo, width / 2f, y, subPaint)
        y += 25f
        canvas.drawLine(30f, y, width - 30f, y, linePaint)
        y += 35f

        canvas.drawText("Cliente: ${item.clienteNombre}", 35f, y, boldPaint)
        y += 25f
        canvas.drawText("Teléfono: ${item.clienteTelefono}", 35f, y, textPaint)
        y += 25f
        canvas.drawText("Bici: ${item.biciMarca} ${item.biciModelo} (${item.biciColor})", 35f, y, textPaint)
        y += 25f
        if (item.biciNumeroSerie.isNotBlank()) {
            canvas.drawText("N° Serie: ${item.biciNumeroSerie}", 35f, y, textPaint)
            y += 25f
        }
        canvas.drawText("Fecha: ${formatDate(item.fechaIngreso)}", 35f, y, textPaint)
        y += 30f
        canvas.drawLine(30f, y, width - 30f, y, linePaint)
        y += 30f

        canvas.drawText("Falla reportada:", 35f, y, boldPaint)
        y += 25f
        canvas.drawText(item.descripcionFalla.take(50), 35f, y, textPaint)
        y += 35f
        canvas.drawLine(30f, y, width - 30f, y, linePaint)
        y += 30f

        canvas.drawText("DESGLOSE:", 35f, y, boldPaint)
        y += 25f
        for (r in repuestos) {
            canvas.drawText("• ${r.nombreDescripcion} x${r.cantidad}", 35f, y, textPaint)
            val subStr = formatMoney(r.subtotal)
            canvas.drawText(subStr, width - 40f - textPaint.measureText(subStr), y, textPaint)
            y += 25f
        }
        canvas.drawText("• Mano de obra", 35f, y, textPaint)
        val moStr = formatMoney(item.costoManoObra)
        canvas.drawText(moStr, width - 40f - textPaint.measureText(moStr), y, textPaint)
        y += 30f
        canvas.drawLine(30f, y, width - 30f, y, linePaint)
        y += 35f

        canvas.drawText("TOTAL:", 35f, y, boldPaint)
        val totStr = formatMoney(item.costoTotal)
        canvas.drawText(totStr, width - 40f - boldPaint.measureText(totStr), y, boldPaint)
        y += 30f

        if (!esPresupuesto) {
            canvas.drawText("Abonado:", 35f, y, textPaint)
            val abStr = formatMoney(item.montoAbonado)
            canvas.drawText(abStr, width - 40f - textPaint.measureText(abStr), y, textPaint)
            y += 25f

            val saldo = (item.costoTotal - item.montoAbonado).coerceAtLeast(0.0)
            canvas.drawText("Saldo Pendiente:", 35f, y, boldPaint)
            val salStr = formatMoney(saldo)
            canvas.drawText(salStr, width - 40f - boldPaint.measureText(salStr), y, boldPaint)
            y += 35f

            canvas.drawText("Garantía: ${item.garantiaDias} días", 35f, y, textPaint)
            y += 35f
        }

        canvas.drawText("¡Gracias por su preferencia!", width / 2f, y, subPaint)

        return bitmap
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File {
        val file = File(context.cacheDir, "$fileName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    /**
     * Share file via Android FileProvider Intent
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
