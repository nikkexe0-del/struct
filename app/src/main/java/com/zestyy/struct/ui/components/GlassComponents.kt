package com.zestyy.struct.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.os.Build
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import com.zestyy.struct.ui.theme.LocalGlassPalette

/**
 * "Liquid Glass" acrylic surface. True cross-content backdrop blur (sampling whatever renders
 * BEHIND this composable) isn't exposed by stock Jetpack Compose — that needs either a
 * RenderNode/backdrop capture trick or a library like chrisbanes/haze. This gives the frosted-glass
 * *look* (translucent gradient fill + soft self-blur on the surface + hairline border + top
 * specular highlight) which reads correctly against the app's dark backgrounds and photo/map
 * content sitting under it. Swap the Box background for a `Modifier.hazeChild(...)` later if you
 * add the Haze library for genuine backdrop sampling.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    tint: Color = LocalGlassPalette.current.surfaceDefault,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(tint, tint.copy(alpha = (tint.alpha * 0.85f).coerceIn(0f, 1f)))
                )
            )
            .border(1.dp, Brush.verticalGradient(listOf(palette.highlight, palette.border)), shape)
            .thenBlurBehind()
    ) {
        content()
    }
}

/** Applies a subtle self-blur so glass edges feel soft, only on API 31+ where RenderEffect exists. */
private fun Modifier.thenBlurBehind(): Modifier = this.then(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = AndroidRenderEffect
                .createBlurEffect(0.6f, 0.6f, Shader.TileMode.CLAMP)
                .asComposeRenderEffect()
            clip = true
        }
    } else Modifier
)

/**
 * A glass pill button with a fluid press animation — scales down and dims slightly on press,
 * springs back on release. Used for primary tracking controls (start/pause/stop/lap).
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalGlassPalette.current.surfaceDefault,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glassButtonScale"
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = (tint.alpha * 0.85f).coerceIn(0f, 1f)))))
            .border(1.dp, Brush.verticalGradient(listOf(palette.highlight, palette.border)), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        content()
    }
}

/** Bottom nav / stat bar with the same glass treatment, meant to float above map content. */
@Composable
fun GlassBar(
    modifier: Modifier = Modifier,
    tint: Color = LocalGlassPalette.current.surfaceDefault,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCard(modifier = modifier, cornerRadius = 28.dp, tint = tint, content = content)
}
