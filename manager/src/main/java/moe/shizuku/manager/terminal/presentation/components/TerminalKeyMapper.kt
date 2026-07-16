package moe.shizuku.manager.terminal.presentation.components

import com.termux.terminal.TerminalSession

object TerminalKeyMapper {
    fun handleKey(key: String, session: TerminalSession?, onCtrlToggle: () -> Unit, onAltToggle: () -> Unit) {
        if (session == null) return
        when (key) {
            "ESC" -> session.write(byteArrayOf(27), 0, 1)
            "TAB" -> session.write(byteArrayOf(9), 0, 1)
            "LEFT" -> session.write(byteArrayOf(27, 91, 68), 0, 3)
            "RIGHT" -> session.write(byteArrayOf(27, 91, 67), 0, 3)
            "UP" -> session.write(byteArrayOf(27, 91, 65), 0, 3)
            "DOWN" -> session.write(byteArrayOf(27, 91, 66), 0, 3)
            "HOME" -> session.write(byteArrayOf(27, 91, 72), 0, 3)
            "END" -> session.write(byteArrayOf(27, 91, 70), 0, 3)
            "BKSP" -> session.write(byteArrayOf(127), 0, 1)
            "ENTER" -> session.write(byteArrayOf(13), 0, 1)
            "CTRL" -> onCtrlToggle()
            "ALT" -> onAltToggle()
            else -> {
                val bytes = key.toByteArray()
                session.write(bytes, 0, bytes.size)
            }
        }
    }
}
