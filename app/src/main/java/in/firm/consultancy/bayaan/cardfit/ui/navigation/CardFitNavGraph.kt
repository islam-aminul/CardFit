package `in`.firm.consultancy.bayaan.cardfit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.CardTypeScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.ConfigureScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.NameScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PreviewScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.ScanScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.SettingsScreen

/** Route constants for the screen graph (CLAUDE.md section 11). */
object Routes {
    const val CARD_TYPE = "card_type"
    const val SCAN = "scan"
    const val CONFIGURE = "configure"
    const val NAME = "name"
    const val PREVIEW = "preview"
    const val SETTINGS = "settings"
}

/**
 * The end-to-end navigation graph. The [AppViewModel] is obtained once here and shared by every
 * destination (activity-scoped), so the ScanSession survives navigation between steps.
 */
@Composable
fun CardFitNavGraph(
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = viewModel(),
) {
    NavHost(navController = navController, startDestination = Routes.CARD_TYPE) {
        composable(Routes.CARD_TYPE) {
            CardTypeScreen(
                viewModel = appViewModel,
                onNext = { navController.navigate(Routes.SCAN) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                viewModel = appViewModel,
                onNext = { navController.navigate(Routes.CONFIGURE) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.CONFIGURE) {
            ConfigureScreen(
                viewModel = appViewModel,
                onNext = { navController.navigate(Routes.NAME) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.NAME) {
            NameScreen(
                viewModel = appViewModel,
                onNext = { navController.navigate(Routes.PREVIEW) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PREVIEW) {
            PreviewScreen(
                viewModel = appViewModel,
                onBack = { navController.popBackStack() },
                onEditConfig = {
                    // Return to Configure to re-export from the same ScanSession (no re-scan).
                    navController.popBackStack(Routes.CONFIGURE, inclusive = false)
                },
                onStartFresh = {
                    // Back to the start (card-type) as a clean slate; the session is already reset.
                    navController.popBackStack(Routes.CARD_TYPE, inclusive = false)
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
