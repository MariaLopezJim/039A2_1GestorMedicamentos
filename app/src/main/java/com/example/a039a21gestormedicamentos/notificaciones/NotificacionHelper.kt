package com.example.a039a21gestormedicamentos.notificaciones

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.a039a21gestormedicamentos.MainActivity
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.example.a039a21gestormedicamentos.data.model.Medicamento
import com.example.a039a21gestormedicamentos.data.model.formatearCantidad

object NotificacionHelper {

    const val CANAL_ID = "recordatorios_medicamentos"
    const val ACCION_TOMAR = "com.example.a039a21gestormedicamentos.TOMAR"
    const val ACCION_POSPONER_10 = "com.example.a039a21gestormedicamentos.POSPONER_10"
    const val ACCION_POSPONER_15 = "com.example.a039a21gestormedicamentos.POSPONER_15"

    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL_ID,
            "Recordatorios de medicamentos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos para tomar tus medicamentos a tiempo"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(canal)
    }

    @SuppressLint("MissingPermission") // El permiso se verifica abajo
    fun mostrar(context: Context, datos: DatosRecordatorio) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        crearCanal(context)

        val hora = LocalTime.ofSecondOfDay(datos.minutoDelDia * 60L)
            .format(DateTimeFormatter.ofPattern("HH:mm"))

        // Al tocar la notificación se abre la app
        val abrirApp = PendingIntent.getActivity(
            context,
            datos.notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Hora de tomar ${datos.nombre}")
            .setContentText("Dosis: ${datos.dosis} · $hora")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Te toca tomar ${datos.dosis} de ${datos.nombre} (programado a las $hora).")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .addAction(0, "Tomar", accion(context, ACCION_TOMAR, datos))
            .addAction(0, "+10 min", accion(context, ACCION_POSPONER_10, datos))
            .addAction(0, "+15 min", accion(context, ACCION_POSPONER_15, datos))
            .build()

        NotificationManagerCompat.from(context).notify(datos.notifId, notificacion)
    }

    private fun accion(context: Context, accion: String, datos: DatosRecordatorio): PendingIntent {
        val intent = Intent(context, AccionReceiver::class.java).apply {
            action = accion
            datos.ponerEn(this)
        }
        return PendingIntent.getBroadcast(
            context,
            datos.notifId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    const val CANAL_INVENTARIO_ID = "inventario_medicamentos"

    @SuppressLint("MissingPermission") // El permiso se verifica abajo
    fun mostrarInventarioBajo(context: Context, medicamento: Medicamento) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val canal = NotificationChannel(
            CANAL_INVENTARIO_ID,
            "Inventario bajo",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Avisos cuando un medicamento está por terminarse" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)

        val dosis = medicamento.dosisRestantes ?: 0
        val restante = formatearCantidad(medicamento.inventario ?: 0.0)

        val titulo = if (dosis == 0) "Se terminó ${medicamento.nombre}"
        else "Quedan pocas dosis de ${medicamento.nombre}"
        val texto = if (dosis == 0) "Ya no alcanza para otra dosis. Reabastécelo lo antes posible."
        else "Te quedan $restante ${medicamento.dosisUnidad} ($dosis dosis). Reabastécelo pronto."

        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_INVENTARIO_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        // Un id por medicamento: si ya hay un aviso, se actualiza en lugar de duplicarse
        NotificationManagerCompat.from(context)
            .notify("inventario-${medicamento.id}".hashCode(), notificacion)
    }

}