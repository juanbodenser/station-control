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
import com.jboden.fuegos.ui.produccion.PantallaProduccion
import com.jboden.fuegos.ui.produccion.ProduccionViewModel
import com.jboden.fuegos.ui.produccion.ProduccionViewModelFactory
import com.jboden.fuegos.ui.recuento.FuegosViewModel
import com.jboden.fuegos.ui.recuento.FuegosViewModelFactory
import com.jboden.fuegos.ui.recuento.PantallaRecuento
import com.jboden.fuegos.ui.theme.FuegosTheme

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
                            onNavigateToProduccion = { navController.navigate("produccion") },
                            onNavigateToAjustes = { navController.navigate("ajustes") },
                            onNavigateToHistorico = { navController.navigate("historico") },
                            onNavigateToConsumo = { navController.navigate("consumo") },
                            onNavigateToCopia = { navController.navigate("copia") }
                        )
                    }
                    composable("produccion") {
                        PantallaProduccion(
                            viewModel = produccionViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("ajustes") {
                        PantallaAjustes(
                            viewModel = ajustesViewModel,
                            produccionViewModel = produccionViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("historico") {
                        PantallaHistorico(
                            viewModel = historicoViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("consumo") {
                        PantallaConsumo(
                            viewModel = consumoViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("copia") {
                        PantallaCopia(
                            viewModel = copiaViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
