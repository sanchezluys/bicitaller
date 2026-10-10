package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetalleTrabajoScreen
import com.example.ui.screens.DocumentoTicketScreen
import com.example.ui.screens.HistorialBicicletaScreen
import com.example.ui.screens.NuevaRecepcionScreen
import com.example.ui.theme.BiciTallerTheme
import com.example.ui.viewmodel.BiciTallerViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: BiciTallerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BiciTallerTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    viewModel.snackbarEvent.collectLatest { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToNuevaRecepcion = {
                                    navController.navigate("nueva_recepcion")
                                },
                                onNavigateToDetalleTrabajo = { id ->
                                    navController.navigate("detalle_trabajo/$id")
                                }
                            )
                        }

                        composable("nueva_recepcion") {
                            NuevaRecepcionScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onRecepcionCreada = { id ->
                                    navController.popBackStack()
                                    navController.navigate("detalle_trabajo/$id")
                                }
                            )
                        }

                        composable(
                            route = "detalle_trabajo/{trabajoId}",
                            arguments = listOf(navArgument("trabajoId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val trabajoId = backStackEntry.arguments?.getLong("trabajoId") ?: 0L
                            DetalleTrabajoScreen(
                                trabajoId = trabajoId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToDocumento = { id ->
                                    navController.navigate("documento_ticket/$id")
                                },
                                onNavigateToHistorial = { biciId ->
                                    navController.navigate("historial_bicicleta/$biciId")
                                }
                            )
                        }

                        composable(
                            route = "documento_ticket/{trabajoId}",
                            arguments = listOf(navArgument("trabajoId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val trabajoId = backStackEntry.arguments?.getLong("trabajoId") ?: 0L
                            DocumentoTicketScreen(
                                trabajoId = trabajoId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "historial_bicicleta/{bicicletaId}",
                            arguments = listOf(navArgument("bicicletaId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val biciId = backStackEntry.arguments?.getLong("bicicletaId") ?: 0L
                            HistorialBicicletaScreen(
                                bicicletaId = biciId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToTrabajo = { tId ->
                                    navController.navigate("detalle_trabajo/$tId")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
