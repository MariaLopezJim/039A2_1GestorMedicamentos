package com.example.a039a21gestormedicamentos.notificaciones

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Permite que un BroadcastReceiver haga trabajo en segundo plano (como usar Room). */
fun BroadcastReceiver.enSegundoPlano(bloque: suspend () -> Unit) {
    val resultado = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            bloque()
        } finally {
            resultado.finish()
        }
    }
}