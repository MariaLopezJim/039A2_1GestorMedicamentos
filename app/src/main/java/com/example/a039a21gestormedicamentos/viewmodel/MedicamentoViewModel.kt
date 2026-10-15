package com.example.a039a21gestormedicamentos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a039a21gestormedicamentos.data.local.AppDatabase
import com.example.a039a21gestormedicamentos.data.local.TomaEntity
import com.example.a039a21gestormedicamentos.data.local.aEntidad
import com.example.a039a21gestormedicamentos.data.local.aModelo
import com.example.a039a21gestormedicamentos.data.model.Medicamento
import com.example.a039a21gestormedicamentos.notificaciones.AlarmaScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class ErroresFormulario(
    val nombre: String? = null,
    val dosis: String? = null,
    val inventario: String? = null
) {
    val hayErrores: Boolean get() = nombre != null || dosis != null || inventario != null
}

data class TomaPendiente(val medicamento: Medicamento, val hora: LocalTime)

data class AgendaDelDia(
    val pendientes: List<TomaPendiente> = emptyList(),
    val tomadas: List<TomaEntity> = emptyList()
)

class MedicamentoViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.obtener(application).medicamentoDao()

    val medicamentos: StateFlow<List<Medicamento>> = dao.observarMedicamentos()
        .map { lista -> lista.map { it.aModelo() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val agendaDeHoy: StateFlow<AgendaDelDia> = combine(
        medicamentos,
        dao.observarTomasDelDia(LocalDate.now().toString())
    ) { meds, tomas ->
        val pendientes = meds
            .flatMap { m -> m.horarios.map { hora -> TomaPendiente(m, hora) } }
            .filter { p ->
                tomas.none {
                    it.medicamentoId == p.medicamento.id &&
                            it.minutoProgramado == p.hora.toSecondOfDay() / 60
                }
            }
            .sortedBy { it.hora }
        AgendaDelDia(pendientes, tomas)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaDelDia())

    fun registrarMedicamento(
        nombre: String,
        dosis: String,
        unidad: String,
        frecuenciaHoras: Int,
        horaInicio: LocalTime,
        cantidadEnvase: String
    ): ErroresFormulario {
        val cantidadDosis = aNumero(dosis)
        val cantidadInventario = aNumero(cantidadEnvase)

        val errores = ErroresFormulario(
            nombre = if (nombre.isBlank()) "El nombre del medicamento es obligatorio" else null,
            dosis = when {
                dosis.isBlank() -> "La dosis es obligatoria"
                cantidadDosis == null || cantidadDosis <= 0 -> "Ingresa una cantidad válida mayor a 0"
                else -> null
            },
            inventario = when {
                cantidadEnvase.isBlank() -> null // Es opcional
                cantidadInventario == null || cantidadInventario <= 0 -> "Ingresa una cantidad válida mayor a 0"
                else -> null
            }
        )

        if (!errores.hayErrores) {
            val nuevo = Medicamento(
                nombre = nombre.trim(),
                dosisCantidad = dosis.trim(),
                dosisUnidad = unidad,
                frecuenciaHoras = frecuenciaHoras,
                horaInicio = horaInicio,
                inventario = cantidadInventario
            )
            viewModelScope.launch {
                dao.insertarMedicamento(nuevo.aEntidad())
                AlarmaScheduler.programarMedicamento(getApplication(), nuevo)
            }
        }
        return errores
    }

    /** Devuelve false si la cantidad no es válida. */
    fun reabastecer(medicamentoId: String, cantidad: String): Boolean {
        val valor = aNumero(cantidad)
        if (valor == null || valor <= 0) return false
        viewModelScope.launch { dao.reabastecer(medicamentoId, valor) }
        return true
    }

    private fun aNumero(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()
}