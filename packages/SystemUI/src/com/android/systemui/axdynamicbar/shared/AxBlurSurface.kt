package com.android.systemui.axdynamicbar.shared

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.axion.blur.AxBlurBackgroundRenderer
import com.android.axion.blur.AxBlurColors

internal val LocalBlurAlpha = compositionLocalOf { 1f }

internal class AxBlurHost(context: Context) : View(context) {
    private val blur = AxBlurBackgroundRenderer(this)
    private val overlayColor = AxBlurColors.surfaceLightTint(context)

    private val bgDrawable: GradientDrawable = GradientDrawable().also { it.setColor(0x00000000) }

    private var fallbackColor = 0
    private var blurAlpha = 1f

    fun configure(cornerRadiusPx: Float, fallback: Int, alpha: Float) {
        if (bgDrawable.cornerRadius != cornerRadiusPx) {
            bgDrawable.cornerRadius = cornerRadiusPx
            invalidate()
        }
        if (fallbackColor != fallback) {
            fallbackColor = fallback
            invalidate()
        }
        if (blurAlpha != alpha) {
            blurAlpha = alpha
            this.alpha = alpha
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        blur.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        blur.onDetachedFromWindow()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        blur.onVisibilityAggregated(isVisible)
    }

    override fun verifyDrawable(who: Drawable): Boolean =
        blur.verifyDrawable(who) || super.verifyDrawable(who)

    override fun draw(canvas: Canvas) {
        if (blurAlpha <= 0f || width <= 0 || height <= 0) return
        bgDrawable.setBounds(0, 0, width, height)
        if (!blur.drawBackgroundWithOverlayColor(canvas, bgDrawable, overlayColor)) {
            bgDrawable.setColor(fallbackColor)
            bgDrawable.draw(canvas)
            bgDrawable.setColor(0x00000000)
        }
    }
}

@Composable
internal fun AxBlurBackdrop(
    cornerRadius: Dp,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
    alpha: Float = LocalBlurAlpha.current,
) {
    val radiusPx = with(LocalDensity.current) { cornerRadius.toPx() }
    val fallback = fallbackColor.toArgb()
    AndroidView(
        factory = { ctx -> AxBlurHost(ctx).also { it.configure(radiusPx, fallback, alpha) } },
        update = { it.configure(radiusPx, fallback, alpha) },
        modifier = modifier,
    )
}
