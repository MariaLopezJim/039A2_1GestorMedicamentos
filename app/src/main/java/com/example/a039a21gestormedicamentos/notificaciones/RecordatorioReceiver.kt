package com.example.a039a21gestormedicamentos.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.a039a21gestormedicamentos.data.local.AppDatabase
import java.time.LocalDate

class RecordatorioReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val datos = DatosRecordatorio.desde(intent) ?: return

        enSegundoPlano {
            val yaTomado = AppDatabase.obtener(context).medicamentoDao()
                .existeToma(datos.medicamentoId, LocalDate.now().toString(), datos.minutoDelDia)

            if (!yaTomado) {
                NotificacionHelper.mostrar(context, datos)
            }

            // La alarma diaria se reprograma para mañana (las pospuestas no)
            if (intent.action == AlarmaScheduler.ACCION_RECORDATORIO) {
                AlarmaScheduler.programarDiaria(context, datos)
            }
        }
    }
}