package dev.maxou.apartment

import android.view.MotionEvent
import kotlin.math.hypot
import kotlin.math.min

class TouchController {

    @Volatile var moveX: Float = 0f
    @Volatile var moveY: Float = 0f

    private val lookLock = Any()
    private var lookDxAccum = 0f
    private var lookDyAccum = 0f

    fun addLookDelta(dx: Float, dy: Float) = synchronized(lookLock) {
        lookDxAccum += dx; lookDyAccum += dy
    }

    fun consumeLookDelta(): Pair<Float, Float> = synchronized(lookLock) {
        val r = lookDxAccum to lookDyAccum
        lookDxAccum = 0f; lookDyAccum = 0f
        r
    }

    data class JoystickState(
        val active: Boolean = false,
        val anchorX: Float = 0f, val anchorY: Float = 0f,
        val thumbX:  Float = 0f, val thumbY:  Float = 0f,
        val radius:  Float = 130f
    )

    private val jsLock = Any()
    private var jsState = JoystickState()
    fun getJoystickState(): JoystickState = synchronized(jsLock) { jsState }

    private var joyPointerId  = -1
    private var lookPointerId = -1
    private var lookLastX = 0f
    private var lookLastY = 0f
    private val JS_RADIUS = 130f
    private val DEAD_ZONE = 0.08f

    fun onTouchEvent(event: MotionEvent, viewWidth: Int) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val ex  = event.getX(idx)
                val ey  = event.getY(idx)
                if (ex < viewWidth / 2f && joyPointerId == -1) {
                    joyPointerId = pid
                    synchronized(jsLock) { jsState = JoystickState(true, ex, ey, ex, ey, JS_RADIUS) }
                    moveX = 0f; moveY = 0f
                } else if (ex >= viewWidth / 2f && lookPointerId == -1) {
                    lookPointerId = pid; lookLastX = ex; lookLastY = ey
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    val ex  = event.getX(i)
                    val ey  = event.getY(i)
                    when (pid) {
                        joyPointerId  -> updateJoystick(ex, ey)
                        lookPointerId -> {
                            addLookDelta(ex - lookLastX, ey - lookLastY)
                            lookLastX = ex; lookLastY = ey
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pid = event.getPointerId(event.actionIndex)
                if (pid == joyPointerId)  { joyPointerId  = -1; moveX = 0f; moveY = 0f; synchronized(jsLock) { jsState = JoystickState() } }
                if (pid == lookPointerId) { lookPointerId = -1 }
            }
            MotionEvent.ACTION_CANCEL -> {
                joyPointerId = -1; lookPointerId = -1; moveX = 0f; moveY = 0f
                synchronized(jsLock) { jsState = JoystickState() }
            }
        }
    }

    private fun updateJoystick(ex: Float, ey: Float) {
        val s   = synchronized(jsLock) { jsState }
        val dx  = ex - s.anchorX; val dy = ey - s.anchorY
        val dist = hypot(dx, dy)
        val r   = if (dist > 0) min(dist, JS_RADIUS) / dist else 0f
        val tx  = s.anchorX + dx * r; val ty = s.anchorY + dy * r
        synchronized(jsLock) { jsState = s.copy(thumbX = tx, thumbY = ty) }
        val nx = (tx - s.anchorX) / JS_RADIUS
        val ny = (ty - s.anchorY) / JS_RADIUS
        moveX = if (kotlin.math.abs(nx) > DEAD_ZONE) nx else 0f
        moveY = if (kotlin.math.abs(ny) > DEAD_ZONE) -ny else 0f
    }
}
