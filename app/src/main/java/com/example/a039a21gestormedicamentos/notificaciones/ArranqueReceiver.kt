package com.example.a039a21gestormedicamentos.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.a039a21gestormedicamentos.data.local.AppDatabase
import com.example.a039a21gestormedicamentos.data.local.aModelo

class ArranqueReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        enSegundoPlano {
            AppDatabase.obtener(context).medicamentoDao()
                .obtenerMedicamentos()
                .forEach { AlarmaScheduler.programarMedicamento(context, it.aModelo()) }
        }
    }
}