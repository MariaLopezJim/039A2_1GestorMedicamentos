package com.example.a039a21gestormedicamentos.notificaciones

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.a039a21gestormedicamentos.data.model.Medicamento
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Datos que viajan dentro de la alarma, para que funcione aunque la app esté cerrada. */
data class DatosRecordatorio(
    val notifId: Int,
    val medicamentoId: String,
    val nombre: String,
    val dosis: String,
    val minutoDelDia: Int
) {
    fun ponerEn(intent: Intent) {
        intent.putExtra(EXTRA_ID, notifId)
        intent.putExtra(EXTRA_MEDICAMENTO_ID, medicamentoId)
        intent.putExtra(EXTRA_NOMBRE, nombre)
        intent.putExtra(EXTRA_DOSIS, dosis)
        intent.putExtra(EXTRA_MINUTO, minutoDelDia)
    }

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_MEDICAMENTO_ID = "extra_medicamento_id"
        private const val EXTRA_NOMBRE = "extra_nombre"
        private const val EXTRA_DOSIS = "extra_dosis"
        private const val EXTRA_MINUTO = "extra_minuto"

        fun desde(intent: Intent): DatosRecordatorio? {
            val medicamentoId = intent.getStringExtra(EXTRA_MEDICAMENTO_ID) ?: return null
            val nombre = intent.getStringExtra(EXTRA_NOMBRE) ?: return null
            return DatosRecordatorio(
                notifId = intent.getIntExtra(EXTRA_ID, 0),
                medicamentoId = medicamentoId,
                nombre = nombre,
                dosis = intent.getStringExtra(EXTRA_DOSIS).orEmpty(),
                minutoDelDia = intent.getIntExtra(EXTRA_MINUTO, 0)
            )
        }
    }
}

object AlarmaScheduler {

    const val ACCION_RECORDATORIO = "com.example.a039a21gestormedicamentos.RECORDATORIO"
    const val ACCION_POSPUESTO = "com.example.a039a21gestormedicamentos.RECORDATORIO_POSPUESTO"

    fun puedeProgramarExactas(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    fun programarMedicamento(context: Context, medicamento: Medicamento) {
        medicamento.horarios.forEach { hora ->
            val datos = DatosRecordatorio(
                notifId = "${medicamento.id}-$hora".hashCode(),
                medicamentoId = medicamento.id,
                nombre = medicamento.nombre,
                dosis = "${medicamento.dosisCantidad} ${medicamento.dosisUnidad}",
                minutoDelDia = hora.toSecondOfDay() / 60
            )
            programarDiaria(context, datos)
        }
    }

    fun programarDiaria(context: Context, datos: DatosRecordatorio) {
        val hora = LocalTime.ofSecondOfDay(datos.minutoDelDia * 60L)
        programar(context, ACCION_RECORDATORIO, datos, proximaOcurrencia(hora))
    }

    fun posponer(context: Context, datos: DatosRecordatorio, minutos: Int) {
        val momento = System.currentTimeMillis() + minutos * 60_000L
        programar(context, ACCION_POSPUESTO, datos, momento)
    }

    private fun programar(context: Context, accion: String, datos: DatosRecordatorio, momentoMillis: Long) {
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            action = accion
            datos.ponerEn(this)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            datos.notifId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val am = context.getSystemService(AlarmManager::class.java)
        if (puedeProgramarExactas(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momentoMillis, pendingIntent)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momentoMillis, pendingIntent)
        }
    }

    private fun proximaOcurrencia(hora: LocalTime): Long {
        var momento = LocalDateTime.of(LocalDate.now(), hora)
        if (!momento.isAfter(LocalDateTime.now())) momento = momento.plusDays(1)
        return momento.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}