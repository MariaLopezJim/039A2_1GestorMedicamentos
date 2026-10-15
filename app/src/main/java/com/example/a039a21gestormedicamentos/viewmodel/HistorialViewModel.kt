package com.example.a039a21gestormedicamentos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a039a21gestormedicamentos.data.local.AppDatabase
import com.example.a039a21gestormedicamentos.data.local.TomaEntity
import com.example.a039a21gestormedicamentos.data.local.aModelo
import com.example.a039a21gestormedicamentos.data.model.Medicamento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

enum class RangoHistorial(val etiqueta: String, val dias: Int) {
    SEMANA("Últimos 7 días", 7),
    MES("Último mes", 30)
}

enum class EstadoToma { TOMADA, OMITIDA, PENDIENTE }

data class TomaHistorial(
    val nombre: String,
    val dosis: String,
    val horaProgramada: LocalTime,
    val estado: EstadoToma,
    val tomadaEn: LocalDateTime?
)

data class DiaHistorial(val fecha: LocalDate, val tomas: List<TomaHistorial>) {
    val tomadas: Int get() = tomas.count { it.estado == EstadoToma.TOMADA }
    val omitidas: Int get() = tomas.count { it.estado == EstadoToma.OMITIDA }

    /** Porcentaje sobre tomas ya vencidas; null si aún no hay ninguna que evaluar. */
    val porcentaje: Int?
        get() = (tomadas + omitidas).takeIf { it > 0 }?.let { tomadas * 100 / it }
}

data class ResumenHistorial(val dias: List<DiaHistorial> = emptyList()) {
    val tomadas: Int get() = dias.sumOf { it.tomadas }
    val omitidas: Int get() = dias.sumOf { it.omitidas }
    val porcentaje: Int?
        get() = (tomadas + omitidas).takeIf { it > 0 }?.let { tomadas * 100 / it }
}

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.obtener(application).medicamentoDao()

    private val _rango = MutableStateFlow(RangoHistorial.SEMANA)
    val rango: StateFlow<RangoHistorial> = _rango.asStateFlow()

    fun cambiarRango(nuevo: RangoHistorial) {
        _rango.value = nuevo
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val resumen: StateFlow<ResumenHistorial> = _rango
        .flatMapLatest { rango ->
            val hoy = LocalDate.now()
            val desde = hoy.minusDays(rango.dias - 1L)
            combine(
                dao.observarMedicamentos(),
                dao.observarTomasEntre(desde.toString(), hoy.toString())
            ) { meds, tomas ->
                construirHistorial(meds.map { it.aModelo() }, tomas, desde, hoy)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumenHistorial())

    private fun construirHistorial(
        medicamentos: List<Medicamento>,
        tomas: List<TomaEntity>,
        desde: LocalDate,
        hoy: LocalDate
    ): ResumenHistorial {
        val zona = ZoneId.systemDefault()
        val ahora = LocalDateTime.now()

        val dias = generateSequence(hoy) { it.minusDays(1) }
            .takeWhile { !it.isBefore(desde) }
            .map { fecha ->
                val tomasDelDia = tomas.filter { it.fecha == fecha.toString() }

                val items = medicamentos.flatMap { m ->
                    val registro = Instant.ofEpochMilli(m.fechaRegistroMillis)
                        .atZone(zona).toLocalDateTime()
                    val dosis = "${m.dosisCantidad} ${m.dosisUnidad}"

                    m.horarios.mapNotNull { hora ->
                        val programada = LocalDateTime.of(fecha, hora)
                        val toma = tomasDelDia.find {
                            it.medicamentoId == m.id && it.minutoProgramado == hora.toSecondOfDay() / 60
                        }
                        when {
                            toma != null -> TomaHistorial(
                                m.nombre, dosis, hora, EstadoToma.TOMADA,
                                Instant.ofEpochMilli(toma.tomadaEnMillis).atZone(zona).toLocalDateTime()
                            )
                            programada.isBefore(registro) -> null // Aún no existía el medicamento
                            programada.isAfter(ahora) -> TomaHistorial(m.nombre, dosis, hora, EstadoToma.PENDIENTE, null)
                            else -> TomaHistorial(m.nombre, dosis, hora, EstadoToma.OMITIDA, null)
                        }
                    }
                }.sortedBy { it.horaProgramada }

                DiaHistorial(fecha, items)
            }
            .filter { it.tomas.isNotEmpty() }
            .toList()

        return ResumenHistorial(dias)
    }
}