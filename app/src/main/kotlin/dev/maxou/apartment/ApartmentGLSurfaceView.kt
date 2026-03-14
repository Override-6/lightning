package dev.maxou.apartment

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class ApartmentGLSurfaceView(
    context: Context,
    private val touch: TouchController,
    val renderer: ApartmentRenderer
) : GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        touch.onTouchEvent(event, width)
        return true
    }
}
