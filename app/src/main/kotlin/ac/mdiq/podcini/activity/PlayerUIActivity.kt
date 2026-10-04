package ac.mdiq.podcini.activity

import ac.mdiq.podcini.config.AppConfig.initialize
import ac.mdiq.podcini.playback.theatres
import ac.mdiq.podcini.sourcing.AppGatewayRegistry
import ac.mdiq.podcini.ui.compose.AppThemes
import ac.mdiq.podcini.ui.compose.PodciniTheme
import ac.mdiq.podcini.ui.compose.textColor
import ac.mdiq.podcini.ui.screens.AVPlayerVM
import ac.mdiq.podcini.ui.screens.ControlUI
import ac.mdiq.podcini.ui.screens.ProgressBar
import ac.mdiq.podcini.utils.Logd
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

private const val TAG = "PlayerUIActivity"
class PlayerUIActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialize()
        AppGatewayRegistry.ensureSourceClients()

        setContent {
            PodciniTheme(AppThemes.BLACK) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(onDismissRequest = { finish() }, dragHandle = null, sheetGesturesEnabled = false, shape = MaterialTheme.shapes.small, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                    val vm: AVPlayerVM = viewModel(key = "0", factory = viewModelFactory { initializer { AVPlayerVM(playerId = 0) } })
                    DisposableEffect(vm) {
                        vm.start()
                        onDispose { vm.stop() }
                    }
                    val player_ by theatres[0].mPlayerFlow.collectAsStateWithLifecycle()
                    val player = player_ ?: return@ModalBottomSheet
                    val curMedia by player.curMediaFlow.collectAsStateWithLifecycle()
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).border(1.dp, MaterialTheme.colorScheme.tertiary).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))) {
                        Column {
                            Text(curMedia?.title ?: "No title", maxLines = 1, color = textColor, style = MaterialTheme.typography.bodyMedium)
                            ProgressBar(vm)
                            ControlUI(vm)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logd(TAG) { "onDestroy called" }
    }
}