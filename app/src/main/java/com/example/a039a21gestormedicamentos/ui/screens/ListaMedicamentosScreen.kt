package com.example.a039a21gestormedicamentos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.a039a21gestormedicamentos.data.local.TomaEntity
import com.example.a039a21gestormedicamentos.data.model.Medicamento
import com.example.a039a21gestormedicamentos.data.model.formatearCantidad
import com.example.a039a21gestormedicamentos.viewmodel.MedicamentoViewModel
import com.example.a039a21gestormedicamentos.viewmodel.TomaPendiente
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
private val formatoAuditoria = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm:ss")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaMedicamentosScreen(
    viewModel: MedicamentoViewModel,
    onAgregarClick: () -> Unit,
    onHistorialClick: () -> Unit
) {
    val medicamentos by viewModel.medicamentos.collectAsStateWithLifecycle()
    val agenda by viewModel.agendaDeHoy.collectAsStateWithLifecycle()
    val porAgotarse = medicamentos.filter { it.inventarioCritico }

    var medicamentoAReabastecer by remember { mutableStateOf<Medicamento?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis medicamentos") },
                actions = {
                    IconButton(onClick = onHistorialClick) {
                        Icon(Icons.Default.DateRange, contentDescription = "Ver historial")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAgregarClick) {
                Icon(Icons.Default.Add, contentDescription = "Agregar medicamento")
            }
        }
    ) { padding ->
        if (medicamentos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aún no tienes medicamentos registrados.\nToca + para agregar uno.",
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- Aviso de inventario crítico ---
                if (porAgotarse.isNotEmpty()) {
                    item { AlertaInventario(porAgotarse) }
                }

                // --- Pendientes ---
                item { Encabezado("Pendientes de hoy (${agenda.pendientes.size})") }
                if (agenda.pendientes.isEmpty()) {
                    item { Text("¡Todo al día! No tienes tomas pendientes.") }
                }
                items(agenda.pendientes, key = { "p-${it.medicamento.id}-${it.hora}" }) {
                    PendienteCard(it)
                }

                // --- Tomados ---
                item { Encabezado("Tomados hoy (${agenda.tomadas.size})") }
                if (agenda.tomadas.isEmpty()) {
                    item { Text("Aún no registras tomas hoy.") }
                }
                items(agenda.tomadas, key = { "t-${it.id}" }) {
                    TomadaCard(it)
                }

                // --- Catálogo ---
                item { Encabezado("Todos mis medicamentos") }
                items(medicamentos, key = { "m-${it.id}" }) { medicamento ->
                    MedicamentoCard(
                        medicamento = medicamento,
                        onReabastecer = { medicamentoAReabastecer = medicamento }
                    )
                }
            }
        }
    }

    medicamentoAReabastecer?.let { medicamento ->
        DialogoReabastecer(
            medicamento = medicamento,
            onConfirmar = { cantidad -> viewModel.reabastecer(medicamento.id, cantidad) },
            onCerrar = { medicamentoAReabastecer = null }
        )
    }
}

@Composable
private fun Encabezado(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun AlertaInventario(medicamentos: List<Medicamento>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Medicamentos por agotarse",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                medicamentos.forEach { m ->
                    val dosis = m.dosisRestantes ?: 0
                    Text(
                        if (dosis == 0) "• ${m.nombre}: agotado"
                        else "• ${m.nombre}: quedan $dosis dosis"
                    )
                }
            }
        }
    }
}

@Composable
private fun PendienteCard(pendiente: TomaPendiente) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                pendiente.hora.format(formatoHora),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
                Text(pendiente.medicamento.nombre, style = MaterialTheme.typography.titleMedium)
                Text("${pendiente.medicamento.dosisCantidad} ${pendiente.medicamento.dosisUnidad}")
            }
        }
    }
}

@Composable
private fun TomadaCard(toma: TomaEntity) {
    val programada = LocalTime.ofSecondOfDay(toma.minutoProgramado * 60L).format(formatoHora)
    val confirmada = Instant.ofEpochMilli(toma.tomadaEnMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatoAuditoria)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("✔ ${toma.nombreMedicamento} · ${toma.dosis}", style = MaterialTheme.typography.titleMedium)
            Text("Programado: $programada")
            Text("Tomado el $confirmada", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MedicamentoCard(medicamento: Medicamento, onReabastecer: () -> Unit) {
    val critico = medicamento.inventarioCritico

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (critico) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(medicamento.nombre, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Dosis: ${medicamento.dosisCantidad} ${medicamento.dosisUnidad}")
            Text("Cada ${medicamento.frecuenciaHoras} horas")
            Text(
                "Horarios: " + medicamento.horarios.joinToString(" · ") { it.format(formatoHora) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (critico) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (critico) {
                    Icon(Icons.Default.Warning, contentDescription = "Inventario bajo", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = medicamento.inventario?.let {
                        "Quedan ${formatearCantidad(it)} ${medicamento.dosisUnidad} · ${medicamento.dosisRestantes} dosis"
                    } ?: "Sin control de inventario",
                    fontWeight = if (critico) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onReabastecer) {
                    Text(if (medicamento.inventario == null) "Agregar" else "Reabastecer")
                }
            }
        }
    }
}

@Composable
private fun DialogoReabastecer(
    medicamento: Medicamento,
    onConfirmar: (String) -> Boolean,
    onCerrar: () -> Unit
) {
    var cantidad by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Reabastecer ${medicamento.nombre}") },
        text = {
            OutlinedTextField(
                value = cantidad,
                onValueChange = { cantidad = it; error = false },
                label = { Text("Cantidad a agregar") },
                suffix = { Text(medicamento.dosisUnidad) },
                isError = error,
                supportingText = { if (error) Text("Ingresa una cantidad válida mayor a 0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (onConfirmar(cantidad)) onCerrar() else error = true
            }) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )
}