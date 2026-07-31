package com.pgdevhouse.dailyrebuild

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val RebuildNavy = Color(0xFF0B2B4B)
val RebuildBlue = Color(0xFF1769AA)
val RebuildTeal = Color(0xFF0F9D96)
val RebuildGreen = Color(0xFF5AB552)
val RebuildSky = Color(0xFFEAF5FB)
val RebuildMint = Color(0xFFE8F7F3)
val RebuildAmber = Color(0xFFF5B842)
val RebuildDanger = Color(0xFFB3261E)

private val DailyRebuildLightColors = lightColorScheme(
    primary = RebuildBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EAF8),
    onPrimaryContainer = RebuildNavy,
    secondary = RebuildTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F2EE),
    onSecondaryContainer = Color(0xFF053A38),
    tertiary = RebuildGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F4DC),
    onTertiaryContainer = Color(0xFF173C16),
    background = Color(0xFFF5F8FB),
    onBackground = Color(0xFF16212B),
    surface = Color.White,
    onSurface = Color(0xFF16212B),
    surfaceVariant = Color(0xFFE8EEF3),
    onSurfaceVariant = Color(0xFF4B5B68),
    outline = Color(0xFF83939F),
    outlineVariant = Color(0xFFD5DEE5),
    error = RebuildDanger,
    onError = Color.White
)

private val DailyRebuildDarkColors = darkColorScheme(
    primary = Color(0xFF8CC9F2),
    onPrimary = Color(0xFF003353),
    primaryContainer = Color(0xFF074E79),
    onPrimaryContainer = Color(0xFFD6EAF8),
    secondary = Color(0xFF78D5CD),
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF00504B),
    onSecondaryContainer = Color(0xFFD7F2EE),
    tertiary = Color(0xFFA2D99B),
    onTertiary = Color(0xFF10370E),
    tertiaryContainer = Color(0xFF2C5428),
    onTertiaryContainer = Color(0xFFE0F4DC),
    background = Color(0xFF0F171D),
    onBackground = Color(0xFFE6EEF4),
    surface = Color(0xFF162129),
    onSurface = Color(0xFFE6EEF4),
    surfaceVariant = Color(0xFF26343E),
    onSurfaceVariant = Color(0xFFB9C8D3),
    outline = Color(0xFF8A9AA6),
    outlineVariant = Color(0xFF344650),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)


private val DailyRebuildShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val DailyRebuildTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    titleLarge = Typography().titleLarge.copy(
        fontSize = 21.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = Typography().titleMedium.copy(
        fontSize = 17.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = Typography().labelLarge.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
)

@Composable
fun DailyRebuildAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            DailyRebuildDarkColors
        } else {
            DailyRebuildLightColors
        },
        typography = DailyRebuildTypography,
        shapes = DailyRebuildShapes,
        content = content
    )
}

@Composable
fun RebuildSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(if (subtitle == null) 28.dp else 44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor)
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )

                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                trailing?.invoke()
            }

            content()
        }
    }
}

@Composable
fun RebuildMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = color,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun RebuildProgressRing(
    progress: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    progressColor: Color = MaterialTheme.colorScheme.secondary
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(size)
        ) {
            val strokeWidth = 10.dp.toPx()
            val inset = strokeWidth / 2f

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    width = this.size.width - strokeWidth,
                    height = this.size.height - strokeWidth
                ),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    width = this.size.width - strokeWidth,
                    height = this.size.height - strokeWidth
                ),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        Text(
            text = centerText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RebuildPrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun RebuildSecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun RebuildStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 11.dp,
                vertical = 6.dp
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RebuildInsetPanel(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}
