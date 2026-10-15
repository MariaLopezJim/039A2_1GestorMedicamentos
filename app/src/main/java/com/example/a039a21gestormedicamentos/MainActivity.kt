package com.example.a039a21gestormedicamentos


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.a039a21gestormedicamentos.ui.screens.ListaMedicamentosScreen
import com.example.a039a21gestormedicamentos.ui.screens.RegistroMedicamentoScreen
import com.example.a039a21gestormedicamentos.ui.theme.A21GestorMedicamentosTheme
import com.example.a039a21gestormedicamentos.viewmodel.MedicamentoViewModel
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.a039a21gestormedicamentos.notificaciones.AlarmaScheduler
import com.example.a039a21gestormedicamentos.ui.screens.HistorialScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            A21GestorMedicamentosTheme {
                MediRecordatorioApp()
            }
        }
    }
}

@Composable
fun MediRecordatorioApp() {
    SolicitarPermisos()

    val navController = rememberNavController()
    // Un solo ViewModel compartido por ambas pantallas
    val viewModel: MedicamentoViewModel = viewModel()

    NavHost(navController = navController, startDestination = "lista") {
        composable("lista") {
            ListaMedicamentosScreen(
                viewModel = viewModel,
                onAgregarClick = { navController.navigate("registro") },
                onHistorialClick = { navController.navigate("historial") }
            )
        }
        composable("registro") {
            RegistroMedicamentoScreen(
                viewModel = viewModel,
                onGuardado = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }
        composable("historial") {
            HistorialScreen(onVolver = { navController.popBackStack() })
        }
    }
}

@Composable
fun SolicitarPermisos() {
    val context = LocalContext.current

    // Permiso de notificaciones (Android 13+)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) {
            Toast.makeText(
                context,
                "Sin permiso de notificaciones no podremos recordarte tus medicamentos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Permiso de alarmas exactas (Android 12+; en Android 14+ viene desactivado)
    var mostrarDialogoAlarmas by remember {
        mutableStateOf(!AlarmaScheduler.puedeProgramarExactas(context))
    }

    if (mostrarDialogoAlarmas && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAlarmas = false },
            title = { Text("Permitir alarmas exactas") },
            text = { Text("Para avisarte a la hora exacta de cada toma, activa el permiso de alarmas y recordatorios.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoAlarmas = false
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }) { Text("Activar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoAlarmas = false }) { Text("Ahora no") }
            }
        )
    }
}