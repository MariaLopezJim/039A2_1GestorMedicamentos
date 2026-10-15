package com.example.a039a21gestormedicamentos.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.a039a21gestormedicamentos.data.model.Medicamento
import java.time.LocalTime

@Entity(tableName = "medicamentos")
data class MedicamentoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val dosisCantidad: String,
    val dosisUnidad: String,
    val frecuenciaHoras: Int,
    val horaInicioMinutos: Int,
    @ColumnInfo(defaultValue = "0") val fechaRegistroMillis: Long,
    val inventario: Double?
)

/** Registro de auditoría: cada vez que el paciente confirma una toma. */
@Entity(tableName = "tomas", indices = [Index("medicamentoId")])
data class TomaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicamentoId: String,
    val nombreMedicamento: String,
    val dosis: String,
    val fecha: String,           // Día de la toma, ej. "2026-10-07"
    val minutoProgramado: Int,   // Hora a la que estaba programada (minutos del día)
    val tomadaEnMillis: Long     // Fecha y hora exacta en que se confirmó
)

fun Medicamento.aEntidad() = MedicamentoEntity(
    id = id,
    nombre = nombre,
    dosisCantidad = dosisCantidad,
    dosisUnidad = dosisUnidad,
    frecuenciaHoras = frecuenciaHoras,
    horaInicioMinutos = horaInicio.toSecondOfDay() / 60,
    fechaRegistroMillis = fechaRegistroMillis,
    inventario = inventario
)

fun MedicamentoEntity.aModelo() = Medicamento(
    id = id,
    nombre = nombre,
    dosisCantidad = dosisCantidad,
    dosisUnidad = dosisUnidad,
    frecuenciaHoras = frecuenciaHoras,
    horaInicio = LocalTime.ofSecondOfDay(horaInicioMinutos * 60L),
    fechaRegistroMillis = fechaRegistroMillis,
    inventario = inventario
)