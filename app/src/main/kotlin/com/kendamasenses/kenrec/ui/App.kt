package com.kendamasenses.kenrec.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/** Two screens: record, then trim. A clip goes record -> trim -> gallery -> back to record. */
@Composable
fun KenRecApp() {
    var trimPath by rememberSaveable { mutableStateOf<String?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Senses.BgDeep).safeDrawingPadding()) {
        val path = trimPath
        if (path == null) {
            RecordScreen(banner = banner, onRecorded = { banner = null; trimPath = it })
        } else {
            TrimScreen(
                rawPath = path,
                onSaved = { banner = "Saved to your gallery (album Ken Rec)"; trimPath = null },
                onDiscard = { trimPath = null },
            )
        }
    }
}
