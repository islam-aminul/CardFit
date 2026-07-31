package `in`.firm.consultancy.bayaan.cardfit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import `in`.firm.consultancy.bayaan.cardfit.domain.model.FitMode
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.TaskViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.CardTypeScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.ConfigureScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.DocumentEditScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.DocumentPagesScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.HomeScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.LicensesScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.NameScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PhotoEditScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PhotoExportScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PhotoSourceScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.PreviewScreen
import `in`.firm.consultancy.bayaan.cardfit.ui.screens.ReceiptWidthScreen
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
    // Standalone document (full-page / receipt) capture flow.
    const val DOC_PAGES = "doc_pages"
    const val DOC_EDIT = "doc_edit" // + "/{index}"
    const val RECEIPT_WIDTH = "receipt_width" // + "/{index}"
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
    shortcutRoute: String? = null,
    onShortcutHandled: () -> Unit = {},
) {
    // A long-press launcher shortcut deep-links into a flow: jump there from Home (so Back returns Home).
    LaunchedEffect(shortcutRoute) {
        val route = shortcutRoute ?: return@LaunchedEffect
        navController.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
        onShortcutHandled()
    }

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
                onDocumentSelected = { navController.navigate(Routes.DOC_PAGES) },
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

        // Standalone document capture: multi-page loop, per-page edit, receipt width.
        composable(Routes.DOC_PAGES) {
            DocumentPagesScreen(
                viewModel = appViewModel,
                allowMultiPage = true,
                onNext = { navController.navigate(Routes.CONFIGURE) },
                onEditPage = { i -> navController.navigate("${Routes.DOC_EDIT}/$i") },
                onPickReceiptWidth = { i -> navController.navigate("${Routes.RECEIPT_WIDTH}/$i") },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "${Routes.DOC_EDIT}/{index}",
            arguments = listOf(navArgument("index") { type = NavType.IntType }),
        ) { entry ->
            DocumentEditScreen(
                viewModel = appViewModel,
                pageIndex = entry.arguments?.getInt("index") ?: 0,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            "${Routes.RECEIPT_WIDTH}/{index}",
            arguments = listOf(navArgument("index") { type = NavType.IntType }),
        ) { entry ->
            ReceiptWidthScreen(
                viewModel = appViewModel,
                pageIndex = entry.arguments?.getInt("index") ?: 0,
                onDone = { navController.popBackStack() },
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
                onNewScan = { navController.popBackStack(Routes.CARD_TYPE, inclusive = false) },
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
                onNewPhoto = { navController.popBackStack(Routes.PHOTO_SOURCE, inclusive = false) },
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

        // Application set: add a scanned document (reuses the card-type + scan/document screens).
        composable(Routes.TASK_CARD_TYPE) {
            CardTypeScreen(
                viewModel = appViewModel,
                onNext = { navController.navigate(Routes.TASK_SCAN) },
                onDocumentSelected = { navController.navigate(Routes.TASK_SCAN) },
                // Dynamic: returns to the active set detail via the back stack.
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TASK_SCAN) {
            val scope = rememberCoroutineScope()
            // A set holds single documents: capture exactly one page (no "Add") for document types,
            // and use the existing card scanner for card types.
            val addToSet: () -> Unit = {
                val s = appViewModel.state.value
                val session = s.session
                scope.launch {
                    if (session != null) taskViewModel.addDocumentEntry(session, s.sizeOverride)
                    appViewModel.reset()
                    navController.popBackStack(Routes.TASK_DETAIL, inclusive = false)
                }
            }
            // Collected, not read off .value: reading the StateFlow directly during composition
            // doesn't subscribe, so this branch would keep the stale card type after a change.
            val appState by appViewModel.state.collectAsStateWithLifecycle()
            val isDocument = appState.session?.cardType?.fitMode != FitMode.ACTUAL_SIZE
            if (isDocument) {
                DocumentPagesScreen(
                    viewModel = appViewModel,
                    allowMultiPage = false,
                    onNext = addToSet,
                    onEditPage = { i -> navController.navigate("${Routes.DOC_EDIT}/$i") },
                    onPickReceiptWidth = { i -> navController.navigate("${Routes.RECEIPT_WIDTH}/$i") },
                    onBack = { navController.popBackStack() },
                )
            } else {
                ScanScreen(
                    viewModel = appViewModel,
                    onNext = addToSet,
                    onBack = { navController.popBackStack() },
                )
            }
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
