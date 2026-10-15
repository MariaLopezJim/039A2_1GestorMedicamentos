package com.example.a039a21gestormedicamentos.data.model

import java.time.LocalTime
import java.util.UUID

/** Con cuántas dosis restantes se considera que el inventario es crítico. */
const val DOSIS_CRITICAS = 3

data class Medicamento(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val dosisCantidad: String,
    val dosisUnidad: String,
    val frecuenciaHoras: Int,
    val horaInicio: LocalTime,
    val fechaRegistroMillis: Long = System.currentTimeMillis(),
    val inventario: Double? = null   // null = el usuario no lleva control de inventario
) {
    val horarios: List<LocalTime>
        get() = calcularHorarios(horaInicio, frecuenciaHoras)

    /** Cantidad numérica de una dosis (ej. "1" → 1.0, "2,5" → 2.5). */
    val dosisNumerica: Double
        get() = dosisCantidad.replace(',', '.').toDoubleOrNull() ?: 1.0

    /** Cuántas dosis completas alcanzan con el inventario actual. */
    val dosisRestantes: Int?
        get() = inventario?.let { (it / dosisNumerica).toInt() }

    val inventarioCritico: Boolean
        get() = dosisRestantes?.let { it <= DOSIS_CRITICAS } ?: false
}

val UNIDADES_DOSIS = listOf("pastillas", "ml", "gotas", "mg", "cápsulas")
val FRECUENCIAS_HORAS = listOf(4, 6, 8, 12, 24)

/** Genera las horas exactas de toma en un día a partir de la primera toma. */
fun calcularHorarios(inicio: LocalTime, frecuenciaHoras: Int): List<LocalTime> =
    (0 until 24 / frecuenciaHoras)
        .map { inicio.plusHours((it * frecuenciaHoras).toLong()) }
        .sorted()

/** Muestra 12.0 como "12" y 2.5 como "2.5". */
fun formatearCantidad(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString() else "%.1f".format(valor)