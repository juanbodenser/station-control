package com.jboden.fuegos.utils

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

fun NavController.navegarAtras() {
    val currentEntry = currentBackStackEntry
    if (currentEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        val route = currentEntry.destination.route
        if (route != "recuento" && previousBackStackEntry != null) {
            popBackStack()
        }
    }
}

fun NavController.navegarA(ruta: String) {
    val currentEntry = currentBackStackEntry
    if (currentEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(ruta) {
            launchSingleTop = true
        }
    }
}
