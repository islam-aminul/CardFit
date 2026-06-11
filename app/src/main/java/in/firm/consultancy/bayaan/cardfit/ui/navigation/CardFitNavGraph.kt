package `in`.firm.consultancy.bayaan.cardfit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.TaskViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.CardTypeScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.ConfigureScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.HomeScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.LicensesScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.NameScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PhotoEditScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PhotoExportScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PhotoSourceScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PreviewScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.ScanScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.SettingsScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.TaskDetailScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.TaskListScreen

/** Route constants for the screen graph (CLAUDE.md sections 11 + Phase 13). */
object Routes {
    const val HOME = "home"
    // Document (ID-card) flow.
    const val CARD_TYPE = "card_type"
    const val SCAN = "scan"
    const val CONFIGURE = "configure"
    const val NAME = "name"
    const val PREVIEW = "preview"
    // Photo flow. Size selection now lives on the edit screen (no standalone size step).
    const val PHOTO_SOURCE = "photo_source"
    const val PHOTO_EDIT = "photo_edit"
    const val PHOTO_EXPORT = "photo_export"
    // Task flow (Phase 14). Adding an entry reuses the document/photo flows under task-scoped routes.
    const val TASK_LIST = "task_list"
    const val TASK_DETAIL = "task_detail"
    const val TASK_CARD_TYPE = "task_card_type"
    const val TASK_SCAN = "task_scan"
    const val TASK_PHOTO_SOURCE = "task_photo_source"
    const val TASK_PHOTO_EDIT = "task_photo_edit"
    const val SETTINGS = "settings"
    const val LICENSES = "licenses"
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
    taskViewModel: TaskViewModel = viewModel(),
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onChooseDocument = { navController.navigate(Routes.CARD_TYPE) },
                onChoosePhoto = { navController.navigate(Routes.PHOTO_SOURCE) },
                onChooseTasks = { navController.navigate(Routes.TASK_LIST) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        // --- document flow ---
        composable(Routes.CARD_TYPE) {
            CardTypeScreen(
                viewModel = appViewModel,
                onNext = { navController.navigate(Routes.SCAN) },
                // Dynamic: returns to Home (the entry that launched this) via the back stack.
                onBack = { navController.popBackStack() },
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

        // --- task flow ---
        composable(Routes.TASK_LIST) {
            TaskListScreen(
                viewModel = taskViewModel,
                onOpenTask = { navController.navigate(Routes.TASK_DETAIL) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TASK_DETAIL) {
            TaskDetailScreen(
                viewModel = taskViewModel,
                onAddDocument = { navController.navigate(Routes.TASK_CARD_TYPE) },
                onAddPhoto = { navController.navigate(Routes.TASK_PHOTO_SOURCE) },
                onBack = { navController.popBackStack(Routes.TASK_LIST, inclusive = false) },
            )
        }

        // Task: add a scanned document (reuses the card-type + scan screens).
        composable(Routes.TASK_CARD_TYPE) {
            CardTypeScreen(
                viewModel = appViewModel,
                onNext = { navController.navigate(Routes.TASK_SCAN) },
                // Dynamic: returns to the active Task detail via the back stack.
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TASK_SCAN) {
            val scope = rememberCoroutineScope()
            ScanScreen(
                viewModel = appViewModel,
                onNext = {
                    val s = appViewModel.state.value
                    val session = s.session
                    scope.launch {
                        if (session != null) taskViewModel.addDocumentEntry(session, s.sizeOverride)
                        appViewModel.reset()
                        navController.popBackStack(Routes.TASK_DETAIL, inclusive = false)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        // Task: add an edited photo (reuses the photo source/edit screens; size is chosen on edit).
        composable(Routes.TASK_PHOTO_SOURCE) {
            PhotoSourceScreen(
                viewModel = photoViewModel,
                onPicked = { navController.navigate(Routes.TASK_PHOTO_EDIT) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TASK_PHOTO_EDIT) {
            val scope = rememberCoroutineScope()
            PhotoEditScreen(
                viewModel = photoViewModel,
                onNext = {
                    val size = photoViewModel.state.value.resolvedSize
                    scope.launch {
                        val uri = photoViewModel.produceEditedImage()
                        if (uri != null && size != null) {
                            taskViewModel.addPhotoEntry(uri, size.widthMm, size.heightMm)
                        }
                        photoViewModel.reset()
                        navController.popBackStack(Routes.TASK_DETAIL, inclusive = false)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenLicenses = { navController.navigate(Routes.LICENSES) },
            )
        }
        composable(Routes.LICENSES) {
            LicensesScreen(onBack = { navController.popBackStack() })
        }
    }
}
