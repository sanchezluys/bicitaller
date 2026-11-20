package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.example.util.InAppUpdateHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: BiciTallerViewModel by viewModels()
    private lateinit var inAppUpdateHelper: InAppUpdateHelper
    private val updateDownloadedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        inAppUpdateHelper = InAppUpdateHelper(this) {
            updateDownloadedEvent.tryEmit(Unit)
        }
        inAppUpdateHelper.checkForUpdate()

        setContent {
            BiciTallerTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val updateDownloadedMsg = stringResource(R.string.update_downloaded_message)
                val updateRestartAction = stringResource(R.string.update_restart_action)

                LaunchedEffect(Unit) {
                    viewModel.snackbarEvent.collectLatest { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                LaunchedEffect(Unit) {
                    updateDownloadedEvent.collectLatest {
                        val result = snackbarHostState.showSnackbar(
                            message = updateDownloadedMsg,
                            actionLabel = updateRestartAction,
                            duration = SnackbarDuration.Indefinite
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            inAppUpdateHelper.completeUpdate()
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.fillMaxSize()
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

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateHelper.onResume()
    }

    override fun onDestroy() {
        inAppUpdateHelper.dispose()
        super.onDestroy()
    }
}

