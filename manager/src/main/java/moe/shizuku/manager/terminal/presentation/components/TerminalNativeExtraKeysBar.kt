package moe.shizuku.manager.terminal.presentation.components

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.shared.terminal.io.extrakeys.ExtraKeyButton
import com.termux.shared.terminal.io.extrakeys.ExtraKeysConstants
import com.termux.shared.terminal.io.extrakeys.ExtraKeysInfo
import com.termux.shared.terminal.io.extrakeys.ExtraKeysView
import com.termux.terminal.TerminalSession
import org.json.JSONException

@Composable
fun TerminalNativeExtraKeysBar(
    session: TerminalSession?,
    isCtrlDown: Boolean,
    isAltDown: Boolean,
    onCtrlToggle: () -> Unit,
    onAltToggle: () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val activeBackgroundColor = MaterialTheme.colorScheme.primary.toArgb()
    val activeTextColor = MaterialTheme.colorScheme.onPrimary.toArgb()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        factory = { context ->
            ExtraKeysView(context, null).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                
                setButtonColors(
                    textColor,
                    activeTextColor,
                    backgroundColor,
                    activeBackgroundColor
                )

                setExtraKeysViewClient(object : ExtraKeysView.IExtraKeysView {
                    override fun onExtraKeyButtonClick(
                        view: View,
                        button: ExtraKeyButton,
                        androidButton: Button
                    ) {
                        TerminalKeyMapper.handleKey(
                            key = button.key,
                            session = session,
                            onCtrlToggle = onCtrlToggle,
                            onAltToggle = onAltToggle
                        )
                    }

                    override fun performExtraKeyButtonHapticFeedback(
                        view: View,
                        button: ExtraKeyButton,
                        androidButton: Button
                    ): Boolean {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        return true
                    }
                })

                try {
                    val jsonStr = """
                        [
                          ["ESC", "TAB", "CTRL", "ALT", "-", "/", "_", "LEFT", "UP", "RIGHT"],
                          ["~", "|", "HOME", "END", "$", "BKSP", "ENTER", "LEFT", "DOWN", "RIGHT"]
                        ]
                    """.trimIndent()
                    val info = ExtraKeysInfo(
                        jsonStr,
                        ExtraKeysConstants.ExtraKeyDisplayMap(),
                        ExtraKeysConstants.ExtraKeyDisplayMap()
                    )
                    // Note: reload normally expects height, but looking at decompiled code, ExtraKeysView.reload(ExtraKeysInfo) is available.
                    reload(info)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        },
        update = { view ->
            // Update active states for CTRL and ALT
            view.readSpecialButton(com.termux.shared.terminal.io.extrakeys.SpecialButton.CTRL, true)
            // Wait, we need to know if we can update the toggle state of special buttons visually.
            // ExtraKeysView maintains its own state for SpecialButtons (CTRL, ALT, SHIFT, FN).
            // Actually, TerminalView reads from ExtraKeysView or we handle it via our isCtrlDown.
            // ExtraKeysView does not have `readSpecialButton` returning Unit, it returns Boolean.
            // We just let ExtraKeysView handle its own toggle visually.
        }
    )
}
