package dev.maxou.apartment

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/**
 * Transparent overlay that draws:
 *  – Joystick visual (left half) when active
 *  – Centre crosshair
 *
 * isClickable = false so touches fall through to the GLSurfaceView beneath.
 */
class OverlayView(context: Context, private val touch: TouchController) : View(context) {

    init {
        isClickable  = false
        isFocusable  = false
        setWillNotDraw(false)
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        color       = Color.argb(110, 255, 255, 255)
        strokeWidth = 3f
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(80, 255, 255, 255)
    }
    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        color       = Color.argb(190, 255, 255, 255)
        strokeWidth = 2f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(190, 255, 255, 255)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width  / 2f
        val cy = height / 2f

        // Crosshair
        val arm = 18f
        canvas.drawLine(cx - arm, cy, cx + arm, cy, crossPaint)
        canvas.drawLine(cx, cy - arm, cx, cy + arm, crossPaint)
        canvas.drawCircle(cx, cy, 2.5f, dotPaint)

        // Joystick
        val js = touch.getJoystickState()
        if (js.active) {
            canvas.drawCircle(js.anchorX, js.anchorY, js.radius, ringPaint)
            canvas.drawCircle(js.thumbX,  js.thumbY,  js.radius * 0.38f, thumbPaint)
        }

        // Redraw every frame (tied to display VSYNC)
        postInvalidateOnAnimation()
    }
}
