package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NebulaObsidian
import com.example.ui.theme.NebulaOutline
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.NebulaSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.SapphireBlue
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextSecondary

fun nebulaBackgroundBrush(): Brush = Brush.verticalGradient(
    colors = listOf(
        NebulaObsidian,
        Color(0xFF0C1024),
        Color(0xFF10132C),
        NebulaObsidian
    )
)

fun primaryAccentGradient(): Brush = Brush.linearGradient(
    colors = listOf(ElectricViolet, SapphireBlue, NeonCyan)
)

@Composable
fun NebulaCard(
    modifier: Modifier = Modifier,
    highlightBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (highlightBorder) {
        ElectricViolet.copy(alpha = 0.65f)
    } else {
        NebulaOutline.copy(alpha = 0.7f)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = NebulaSurfaceElevated.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(accentColor.copy(alpha = 0.16f))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor
        )
    }
}

@Composable
fun BabyVoiceOrbMicButton(
    isAssistantEnabled: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "main_mic_button"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_scale"
    )
    val ringScale by animateFloatAsState(
        targetValue = if (isListening) 1.10f + (rmsLevel * 0.35f) else if (isSpeaking) 1.08f else idlePulse,
        animationSpec = spring(dampingRatio = 0.65f),
        label = "ring_scale"
    )

    val outerGlowColor = when {
        isListening -> NeonCyan
        isSpeaking -> EmeraldSuccess
        isAssistantEnabled -> ElectricViolet
        else -> SapphireBlue.copy(alpha = 0.55f)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(196.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                outerGlowColor.copy(alpha = if (isListening) 0.42f else 0.24f),
                                DeepViolet.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = (size.minDimension / 2f) * ringScale
                        )
                    )
                }
        ) {
            // Outer animated halo ring
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .border(
                        width = if (isListening) 2.5.dp else 1.5.dp,
                        brush = Brush.sweepGradient(
                            listOf(ElectricViolet, NeonCyan, SapphireBlue, ElectricViolet)
                        ),
                        shape = CircleShape
                    )
            )

            // Main clickable orb button
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(NebulaSurface)
                    .border(
                        width = 2.dp,
                        brush = primaryAccentGradient(),
                        shape = CircleShape
                    )
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.cd_mic_button),
                        onClick = onClick
                    )
                    .testTag(testTag),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_baby_orb),
                    contentDescription = stringResource(R.string.cd_baby_orb),
                    contentScale = ContentScale.Crop,
                    alpha = if (isAssistantEnabled || isListening) 0.85f else 0.45f,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.28f),
                                    NebulaObsidian.copy(alpha = 0.65f)
                                )
                            )
                        )
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when {
                            isListening -> Icons.Rounded.Stop
                            isSpeaking -> Icons.Rounded.GraphicEq
                            else -> Icons.Rounded.Mic
                        },
                        contentDescription = stringResource(R.string.cd_mic_button),
                        tint = if (isListening) RoseDanger else Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            isListening -> "TAP TO STOP"
                            isSpeaking -> "SPEAKING"
                            else -> "TAP TO SPEAK"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftLavender
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceWaveformMeter(
    isActive: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 12
    val transition = rememberInfiniteTransition(label = "wave_bars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val weight = if (isActive) {
                val wave = kotlin.math.abs(kotlin.math.sin((phase + i * 0.55f).toDouble())).toFloat()
                (0.22f + (rmsLevel.coerceAtLeast(0.2f) * wave * 0.78f)).coerceIn(0.18f, 1f)
            } else {
                0.18f
            }
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height((26 * weight).dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isActive) primaryAccentGradient()
                        else Brush.verticalGradient(listOf(TextSecondary.copy(alpha = 0.25f), NebulaOutline))
                    )
            )
        }
    }
}
