package `in`.firm.consultancy.bayaan.cardfit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import `in`.firm.consultancy.bayaan.cardfit.ui.navigation.CardFitNavGraph
import `in`.firm.consultancy.bayaan.cardfit.ui.navigation.Routes
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.CardFitTheme

class MainActivity : ComponentActivity() {

    // Route requested by a long-press launcher shortcut (null for a normal launch); read reactively
    // so a warm-start shortcut tap (onNewIntent) re-navigates.
    private var shortcutRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        shortcutRoute = routeForIntent(intent)
        setContent {
            CardFitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CardFitNavGraph(
                        shortcutRoute = shortcutRoute,
                        onShortcutHandled = { shortcutRoute = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutRoute = routeForIntent(intent)
    }

    private fun routeForIntent(intent: Intent?): String? = when (intent?.action) {
        ACTION_SCAN -> Routes.CARD_TYPE
        ACTION_PHOTO -> Routes.PHOTO_SOURCE
        ACTION_TASK -> Routes.TASK_LIST
        else -> null
    }

    companion object {
        const val ACTION_SCAN = "in.firm.consultancy.bayaan.cardfit.SHORTCUT_SCAN"
        const val ACTION_PHOTO = "in.firm.consultancy.bayaan.cardfit.SHORTCUT_PHOTO"
        const val ACTION_TASK = "in.firm.consultancy.bayaan.cardfit.SHORTCUT_TASK"
    }
}
