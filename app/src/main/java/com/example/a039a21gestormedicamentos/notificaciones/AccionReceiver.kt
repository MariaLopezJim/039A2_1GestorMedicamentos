package com.example.a039a21gestormedicamentos.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.a039a21gestormedicamentos.data.local.AppDatabase
import com.example.a039a21gestormedicamentos.data.local.TomaEntity
import com.example.a039a21gestormedicamentos.data.local.aModelo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class AccionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val datos = DatosRecordatorio.desde(intent) ?: return

        NotificationManagerCompat.from(context).cancel(datos.notifId)

        when (intent.action) {
            NotificacionHelper.ACCION_TOMAR -> registrarToma(context, datos)
            NotificacionHelper.ACCION_POSPONER_10 -> posponer(context, datos, 10)
            NotificacionHelper.ACCION_POSPONER_15 -> posponer(context, datos, 15)
        }
    }

    private fun registrarToma(context: Context, datos: DatosRecordatorio) = enSegundoPlano {
        val dao = AppDatabase.obtener(context).medicamentoDao()
        val hoy = LocalDate.now().toString()

        // Solo se registra y descuenta una vez por toma
        if (!dao.existeToma(datos.medicamentoId, hoy, datos.minutoDelDia)) {
            dao.insertarToma(
                TomaEntity(
                    medicamentoId = datos.medicamentoId,
                    nombreMedicamento = datos.nombre,
                    dosis = datos.dosis,
                    fecha = hoy,
                    minutoProgramado = datos.minutoDelDia,
                    tomadaEnMillis = System.currentTimeMillis()
                )
            )

            val medicamento = dao.obtenerMedicamento(datos.medicamentoId)?.aModelo()
            if (medicamento?.inventario != null) {
                dao.descontarInventario(medicamento.id, medicamento.dosisNumerica)

                val actualizado = dao.obtenerMedicamento(medicamento.id)?.aModelo()
                if (actualizado != null && actualizado.inventarioCritico) {
                    NotificacionHelper.mostrarInventarioBajo(context, actualizado)
                }
            }
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "✔ ${datos.nombre} registrado como tomado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun posponer(context: Context, datos: DatosRecordatorio, minutos: Int) {
        AlarmaScheduler.posponer(context, datos, minutos)
        Toast.makeText(context, "Te lo recordaremos en $minutos minutos", Toast.LENGTH_SHORT).show()
    }
}