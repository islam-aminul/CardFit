package `in`.firm.consultancy.bayaan.cardfit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.CardTypeScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.ConfigureScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.HomeScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.NameScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PhotoEditScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PhotoExportScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PhotoSizeScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PhotoSourceScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PreviewScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.ScanScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.SettingsScreen

/** Route constants for the screen graph (CLAUDE.md sections 11 + Phase 13). */
object Routes {
    const val HOME = "home"
    // Document (ID-card) flow.
    const val CARD_TYPE = "card_type"
    const val SCAN = "scan"
    const val CONFIGURE = "configure"
    const val NAME = "name"
    const val PREVIEW = "preview"
    // Photo flow.
    const val PHOTO_SOURCE = "photo_source"
    const val PHOTO_EDIT = "photo_edit"
    const val PHOTO_SIZE = "photo_size"
    const val PHOTO_EXPORT = "photo_export"
    const val SETTINGS = "settings"
}

/**
 * The end-to-end navigation graph. [AppViewModel] (document flow) and [PhotoViewModel] (photo flow)
 * are each obtained once here and shared by their destinations (activity-scoped), so each flow's
 * session survives navigation between its steps.
 */
@Composable
fun CardFitNavGraph(
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = viewModel(),
    photoViewModel: PhotoViewModel = viewModel(),
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onChooseDocument = { navController.navigate(Routes.CARD_TYPE) },
                onChoosePhoto = { navController.navigate(Routes.PHOTO_SOURCE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        // --- document flow ---
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
                onEditConfig = { navController.popBackStack(Routes.CONFIGURE, inclusive = false) },
                onStartFresh = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }

        // --- photo flow ---
        composable(Routes.PHOTO_SOURCE) {
            PhotoSourceScreen(
                viewModel = photoViewModel,
                onPicked = { navController.navigate(Routes.PHOTO_EDIT) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PHOTO_EDIT) {
            PhotoEditScreen(
                viewModel = photoViewModel,
                onNext = { navController.navigate(Routes.PHOTO_SIZE) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PHOTO_SIZE) {
            PhotoSizeScreen(
                viewModel = photoViewModel,
                onNext = { navController.navigate(Routes.PHOTO_EXPORT) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PHOTO_EXPORT) {
            PhotoExportScreen(
                viewModel = photoViewModel,
                onBack = { navController.popBackStack() },
                onStartFresh = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
