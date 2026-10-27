package com.griaai.app.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.griaai.app.core.theme.LocalGriaColors

/**
 * Minimalist Quiet Luxury Logo for NEXUS AI.
 *
 * An architectural convergence lens and diamond core representing local intelligence
 * connecting human thought with private machine inference.
 */
@Composable
fun GriaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showContainer: Boolean = true
) {
    val colors = LocalGriaColors.current
    val containerShape = RoundedCornerShape(size * 0.28f)

    if (showContainer) {
        Surface(
            modifier = modifier
                .size(size)
                .clip(containerShape)
                .border(0.75.dp, colors.borderSubtle, containerShape),
            color = colors.surfaceElevated,
            shape = containerShape
        ) {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                GriaLogoSymbol(
                    size = size * 0.62f,
                    primaryColor = colors.accent,
                    secondaryColor = colors.textPrimary,
                    coreColor = colors.surfaceElevated
                )
            }
        }
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            GriaLogoSymbol(
                size = size,
                primaryColor = colors.accent,
                secondaryColor = colors.textPrimary,
                coreColor = colors.background
            )
        }
    }
}

@Composable
fun GriaLogoSymbol(
    size: Dp,
    primaryColor: Color,
    secondaryColor: Color,
    coreColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Four orbital micro-nodes
        val orbitRadius = w * 0.44f
        val nodeRadius = w * 0.026f
        val orbitColor = primaryColor.copy(alpha = 0.55f)

        drawCircle(orbitColor, nodeRadius, Offset(cx, cy - orbitRadius))
        drawCircle(orbitColor, nodeRadius, Offset(cx + orbitRadius, cy))
        drawCircle(orbitColor, nodeRadius, Offset(cx, cy + orbitRadius))
        drawCircle(orbitColor, nodeRadius, Offset(cx - orbitRadius, cy))

        // 2. Converging Astrolabe Nexus Lens
        val lensReach = w * 0.38f
        val curveTension = w * 0.12f

        val lensPath = Path().apply {
            moveTo(cx, cy - lensReach)
            cubicTo(cx, cy - curveTension, cx + curveTension, cy, cx + lensReach, cy)
            cubicTo(cx + curveTension, cy, cx, cy + curveTension, cx, cy + lensReach)
            cubicTo(cx, cy + curveTension, cx - curveTension, cy, cx - lensReach, cy)
            cubicTo(cx - curveTension, cy, cx, cy - curveTension, cx, cy - lensReach)
            close()
        }
        drawPath(lensPath, color = secondaryColor, style = Fill)

        // 3. Central Convergence Diamond in Accent (Midnight Blue)
        val diamondRadius = w * 0.18f
        val diamondPath = Path().apply {
            moveTo(cx, cy - diamondRadius)
            lineTo(cx + diamondRadius, cy)
            lineTo(cx, cy + diamondRadius)
            lineTo(cx - diamondRadius, cy)
            close()
        }
        drawPath(diamondPath, color = primaryColor, style = Fill)

        // 4. Central Aperture Pin Dot
        drawCircle(coreColor, w * 0.045f, Offset(cx, cy))
    }
}
