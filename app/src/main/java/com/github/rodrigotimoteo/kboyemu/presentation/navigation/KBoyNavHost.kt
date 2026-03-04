package com.github.rodrigotimoteo.kboyemu.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.rodrigotimoteo.kboyemu.presentation.emulator.EmulatorScreen
import com.github.rodrigotimoteo.kboyemu.presentation.home.HomeScreen
import com.github.rodrigotimoteo.kboyemu.presentation.settings.SettingsScreen

/**
 * Top-level navigation graph for the app. Defines transitions between the home (ROM picker),
 * emulator (game play), and settings screens.
 *
 * @param navController controller that drives navigation between destinations
 * @param modifier modifier applied to the [NavHost]
 *
 * @author rodrigotimoteo
 */
@Composable
fun KBoyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.path,
        modifier = modifier,
    ) {
        composable(Route.Home.path) {
            HomeScreen(
                onRomLoaded = {
                    navController.navigate(Route.Emulator.path) {
                        popUpTo(Route.Home.path) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Emulator.path) {
            EmulatorScreen(
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.path)
                },
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onChangeRom = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Emulator.path) { inclusive = true }
                    }
                },
            )
        }
    }
}


