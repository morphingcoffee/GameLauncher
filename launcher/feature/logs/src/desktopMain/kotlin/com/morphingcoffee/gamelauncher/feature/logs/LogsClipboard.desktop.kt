package com.morphingcoffee.gamelauncher.feature.logs

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual fun copyTextToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}
