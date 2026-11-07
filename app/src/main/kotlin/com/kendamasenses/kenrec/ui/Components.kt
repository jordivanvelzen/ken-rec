package com.kendamasenses.kenrec.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kendamasenses.kenrec.R

enum class PillStyle { Primary, Outlined, Danger }

/** The storefront's button: a pill, mint when it is the main action. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(percent = 50)
    val (bg, fg) = when (style) {
        PillStyle.Primary -> Senses.Mint to Senses.MintInk
        PillStyle.Outlined -> androidx.compose.ui.graphics.Color.Transparent to Senses.Ink
        PillStyle.Danger -> Senses.Coral to Senses.Ink
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(bg)
            .then(if (style == PillStyle.Outlined) Modifier.border(1.dp, Senses.Border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.02.sp)
    }
}

/** A round 44dp button with an icon. */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Senses.BgDeep.copy(alpha = 0.55f))
            .border(1.dp, Senses.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Senses.Ink)
    }
}

/** A round 44dp button with a short text, for the nudge controls. */
@Composable
fun RoundTextButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Senses.Surface)
            .border(1.dp, Senses.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Senses.Ink, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Logo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_horizontal),
        contentDescription = "Kendama Senses",
        modifier = modifier.height(26.dp),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Senses.Ink,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.fillMaxWidth(),
    )
}
