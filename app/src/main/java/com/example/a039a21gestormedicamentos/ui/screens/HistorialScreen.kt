package com.example.a039a21gestormedicamentos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.a039a21gestormedicamentos.viewmodel.DiaHistorial
import com.example.a039a21gestormedicamentos.viewmodel.EstadoToma
import com.example.a039a21gestormedicamentos.viewmodel.HistorialViewModel
import com.example.a039a21gestormedicamentos.viewmodel.RangoHistorial
import com.example.a039a21gestormedicamentos.viewmodel.TomaHistorial
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val VerdeCumplido = Color(0xFF2E7D32)
private val RojoOmitido = Color(0xFFC62828)
private val AmbarParcial = Color(0xFFF9A825)
private val GrisPendiente = Color(0xFF757575)

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
private val formatoDia = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-MX"))

/** Verde ≥ 80 %, ámbar 50–79 %, rojo < 50 %. */
private fun colorPorcentaje(porcentaje: Int?): Color = when {
    porcentaje == null -> GrisPendiente
    porcentaje >= 80 -> VerdeCumplido
    porcentaje >= 50 -> AmbarParcial
    else -> RojoOmitido
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    onVolver: () -> Unit,
    viewModel: HistorialViewModel = viewModel()
) {
    val rango by viewModel.rango.collectAsStateWithLifecycle()
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Filtro de rango ---
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangoHistorial.entries.forEach { opcion ->
                        FilterChip(
                            selected = rango == opcion,
                            onClick = { viewModel.cambiarRango(opcion) },
                            label = { Text(opcion.etiqueta) }
                        )
                    }
                }
            }

            // --- Resumen general ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Cumplimiento del periodo", style = MaterialTheme.typography.labelLarge)
                        Text(
                            resumen.porcentaje?.let { "$it %" } ?: "Sin datos",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = colorPorcentaje(resumen.porcentaje)
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (resumen.porcentaje ?: 0) / 100f },
                            color = colorPorcentaje(resumen.porcentaje),
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${resumen.tomadas} tomadas · ${resumen.omitidas} omitidas")
                    }
                }
            }

            // --- Leyenda ---
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Leyenda(VerdeCumplido, "Tomada")
                    Leyenda(RojoOmitido, "Omitida")
                    Leyenda(GrisPendiente, "Pendiente")
                }
            }

            // --- Días ---
            if (resumen.dias.isEmpty()) {
                item {
                    Text(
                        "No hay tomas programadas en este periodo.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                    )
                }
            }
            items(resumen.dias, key = { it.fecha.toString() }) { dia ->
                DiaCard(dia)
            }
        }
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.width(4.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DiaCard(dia: DiaHistorial) {
    val titulo = if (dia.fecha == LocalDate.now()) "Hoy"
    else dia.fecha.format(formatoDia).replaceFirstChar { it.uppercase() }
    val color = colorPorcentaje(dia.porcentaje)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    dia.porcentaje?.let { "$it %" } ?: "—",
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (dia.porcentaje ?: 0) / 100f },
                color = color,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                dia.tomas.forEach { FilaToma(it) }
            }
        }
    }
}

@Composable
private fun FilaToma(toma: TomaHistorial) {
    val (color, icono, detalle) = when (toma.estado) {
        EstadoToma.TOMADA -> Triple(
            VerdeCumplido, Icons.Default.CheckCircle,
            "Tomado ${toma.tomadaEn?.format(formatoHora).orEmpty()}"
        )
        EstadoToma.OMITIDA -> Triple(RojoOmitido, Icons.Default.Close, "Omitido")
        EstadoToma.PENDIENTE -> Triple(GrisPendiente, Icons.Default.Notifications, "Pendiente")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${toma.horaProgramada.format(formatoHora)} · ${toma.nombre}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(toma.dosis, style = MaterialTheme.typography.bodySmall)
        }
        Text(detalle, color = color, style = MaterialTheme.typography.labelMedium)
    }
}