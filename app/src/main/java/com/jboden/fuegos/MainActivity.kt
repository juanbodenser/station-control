package com.jboden.fuegos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jboden.fuegos.data.FuegosDatabase
import com.jboden.fuegos.ui.ajustes.AjustesViewModel
import com.jboden.fuegos.ui.ajustes.AjustesViewModelFactory
import com.jboden.fuegos.ui.ajustes.PantallaAjustes
import com.jboden.fuegos.ui.consumo.ConsumoViewModel
import com.jboden.fuegos.ui.consumo.ConsumoViewModelFactory
import com.jboden.fuegos.ui.consumo.PantallaConsumo
import com.jboden.fuegos.ui.copia.CopiaViewModel
import com.jboden.fuegos.ui.copia.CopiaViewModelFactory
import com.jboden.fuegos.ui.copia.PantallaCopia
import com.jboden.fuegos.ui.historico.HistoricoViewModel
import com.jboden.fuegos.ui.historico.HistoricoViewModelFactory
import com.jboden.fuegos.ui.historico.PantallaHistorico
import com.jboden.fuegos.ui.produccion.ProduccionViewModel
import com.jboden.fuegos.ui.produccion.ProduccionViewModelFactory
import com.jboden.fuegos.ui.recuento.FuegosViewModel
import com.jboden.fuegos.ui.recuento.FuegosViewModelFactory
import com.jboden.fuegos.ui.recuento.PantallaRecuento
import com.jboden.fuegos.ui.theme.FuegosTheme
import com.jboden.fuegos.utils.navegarA
import com.jboden.fuegos.utils.navegarAtras

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = FuegosDatabase.getDatabase(applicationContext)
        val recuentoViewModelFactory = FuegosViewModelFactory(database)
        val ajustesViewModelFactory = AjustesViewModelFactory(database)
        val historicoViewModelFactory = HistoricoViewModelFactory(database)
        val copiaViewModelFactory = CopiaViewModelFactory(database, applicationContext)
        val consumoViewModelFactory = ConsumoViewModelFactory(database)
        val produccionViewModelFactory = ProduccionViewModelFactory(database, applicationContext)

        setContent {
            FuegosTheme {
                val navController = rememberNavController()
                val recuentoViewModel: FuegosViewModel = viewModel(factory = recuentoViewModelFactory)
                val ajustesViewModel: AjustesViewModel = viewModel(factory = ajustesViewModelFactory)
                val historicoViewModel: HistoricoViewModel = viewModel(factory = historicoViewModelFactory)
                val copiaViewModel: CopiaViewModel = viewModel(factory = copiaViewModelFactory)
                val consumoViewModel: ConsumoViewModel = viewModel(factory = consumoViewModelFactory)
                val produccionViewModel: ProduccionViewModel = viewModel(factory = produccionViewModelFactory)

                NavHost(navController = navController, startDestination = "recuento") {
                    composable("recuento") {
                        PantallaRecuento(
                            viewModel = recuentoViewModel,
                            produccionViewModel = produccionViewModel,
                            onNavigateToAjustes = { navController.navegarA("ajustes") },
                            onNavigateToHistorico = { navController.navegarA("historico") },
                            onNavigateToConsumo = { navController.navegarA("consumo") },
                            onNavigateToCopia = { navController.navegarA("copia") }
                        )
                    }
                    composable("ajustes") {
                        PantallaAjustes(
                            viewModel = ajustesViewModel,
                            produccionViewModel = produccionViewModel,
                            onBack = { navController.navegarAtras() }
                        )
                    }
                    composable("historico") {
                        PantallaHistorico(
                            viewModel = historicoViewModel,
                            onBack = { navController.navegarAtras() }
                        )
                    }
                    composable("consumo") {
                        PantallaConsumo(
                            viewModel = consumoViewModel,
                            onBack = { navController.navegarAtras() }
                        )
                    }
                    composable("copia") {
                        PantallaCopia(
                            viewModel = copiaViewModel,
                            onBack = { navController.navegarAtras() }
                        )
                    }
                }
            }
        }
    }
}
