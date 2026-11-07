package com.kendamasenses.kenrec.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kendamasenses.kenrec.AppSettings
import com.kendamasenses.kenrec.RESTART_CHOICES
import com.kendamasenses.kenrec.VideoQuality
import com.kendamasenses.kenrec.approxGigabytes

private fun gb(value: Double) = "%.1f GB".format(value)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { AppSettings(context) }
    var minutes by remember { mutableIntStateOf(store.restartMinutes) }
    var quality by remember { mutableStateOf(store.quality) }
    BackHandler(onBack = onBack)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Logo()
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            SectionTitle("Settings")
        }

        Spacer(Modifier.height(24.dp))
        Heading("Restart a recording after")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RESTART_CHOICES.forEach { m ->
                PillButton(
                    text = "$m min",
                    onClick = { minutes = m; store.restartMinutes = m },
                    style = if (m == minutes) PillStyle.Primary else PillStyle.Outlined,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Note("Then the old recording is thrown away and a new one starts, so the phone never fills up. A Restart now button shows in the last 90 seconds.")

        Spacer(Modifier.height(24.dp))
        Heading("Video quality")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            VideoQuality.entries.forEach { q ->
                QualityCard(
                    quality = q,
                    selected = q == quality,
                    onClick = { quality = q; store.quality = q },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Note(
            "A full recording before a restart ($minutes min) is about ${gb(approxGigabytes(quality, minutes * 60L))}. " +
                "Sizes are rough and change with what the camera sees and with your phone. Changes apply to the next recording.",
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, color = Senses.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Note(text: String) {
    Text(text, color = Senses.InkMute, fontSize = 12.sp, lineHeight = 18.sp)
}

@Composable
private fun QualityCard(quality: VideoQuality, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Senses.Mint.copy(alpha = 0.12f) else Senses.Surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) Senses.Mint else Senses.Border, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(quality.title, color = Senses.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(quality.detail, color = Senses.InkMute, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text("About ${gb(approxGigabytes(quality, 3600))} per hour", color = Senses.Ink, fontSize = 13.sp)
    }
}
