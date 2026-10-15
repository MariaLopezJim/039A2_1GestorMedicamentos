package com.example.a039a21gestormedicamentos.ui.screens


import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.a039a21gestormedicamentos.data.model.FRECUENCIAS_HORAS
import com.example.a039a21gestormedicamentos.data.model.UNIDADES_DOSIS
import com.example.a039a21gestormedicamentos.data.model.calcularHorarios
import com.example.a039a21gestormedicamentos.viewmodel.ErroresFormulario
import com.example.a039a21gestormedicamentos.viewmodel.MedicamentoViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroMedicamentoScreen(
    viewModel: MedicamentoViewModel,
    onGuardado: () -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val formato = remember { DateTimeFormatter.ofPattern("HH:mm") }

    var nombre by rememberSaveable { mutableStateOf("") }
    var dosis by rememberSaveable { mutableStateOf("") }
    var unidad by rememberSaveable { mutableStateOf(UNIDADES_DOSIS.first()) }
    var cantidadEnvase by rememberSaveable { mutableStateOf("") }
    var frecuencia by rememberSaveable { mutableIntStateOf(8) }
    var horaInicio by rememberSaveable { mutableStateOf(LocalTime.of(8, 0)) }
    var errores by remember { mutableStateOf(ErroresFormulario()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo medicamento") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Nombre ---
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; errores = errores.copy(nombre = null) },
                label = { Text("Nombre del medicamento *") },
                isError = errores.nombre != null,
                supportingText = { errores.nombre?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // --- Dosis ---
            OutlinedTextField(
                value = dosis,
                onValueChange = { dosis = it; errores = errores.copy(dosis = null) },
                label = { Text("Dosis (cantidad) *") },
                isError = errores.dosis != null,
                supportingText = { errores.dosis?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Unidad", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UNIDADES_DOSIS.forEach { opcion ->
                    FilterChip(
                        selected = unidad == opcion,
                        onClick = { unidad = opcion },
                        label = { Text(opcion) }
                    )
                }
                // --- Inventario (opcional) ---
                OutlinedTextField(
                    value = cantidadEnvase,
                    onValueChange = { cantidadEnvase = it; errores = errores.copy(inventario = null) },
                    label = { Text("Cantidad en la caja o frasco (opcional)") },
                    suffix = { Text(unidad) },
                    isError = errores.inventario != null,
                    supportingText = {
                        Text(errores.inventario ?: "Te avisaremos cuando queden 3 dosis o menos")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- Frecuencia ---
            Text("Frecuencia", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FRECUENCIAS_HORAS.forEach { horas ->
                    FilterChip(
                        selected = frecuencia == horas,
                        onClick = { frecuencia = horas },
                        label = { Text(if (horas == 24) "1 vez al día" else "Cada $horas h") }
                    )
                }
            }

            // --- Hora de la primera toma ---
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> horaInicio = LocalTime.of(h, m) },
                        horaInicio.hour,
                        horaInicio.minute,
                        true
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Primera toma: ${horaInicio.format(formato)}")
            }

            // --- Vista previa de horarios ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tomarás este medicamento a las:", style = MaterialTheme.typography.labelLarge)
                    Text(
                        calcularHorarios(horaInicio, frecuencia).joinToString(" · ") { it.format(formato) },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // --- Guardar ---
            Button(
                onClick = {
                    errores = viewModel.registrarMedicamento(nombre, dosis, unidad, frecuencia, horaInicio, cantidadEnvase)
                    if (errores.hayErrores) {
                        Toast.makeText(context, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Medicamento registrado", Toast.LENGTH_SHORT).show()
                        onGuardado()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar medicamento")
            }
        }
    }
}