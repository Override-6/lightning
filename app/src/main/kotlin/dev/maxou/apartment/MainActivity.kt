package dev.maxou.apartment

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class MainActivity : Activity() {

    private lateinit var glView: ApartmentGLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full-screen immersive
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )

        val touch    = TouchController()
        val renderer = ApartmentRenderer(touch)
        glView       = ApartmentGLSurfaceView(this, touch, renderer)
        val overlay  = OverlayView(this, touch)

        val frame = FrameLayout(this)
        val lp    = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        frame.addView(glView,   lp)
        frame.addView(overlay,  lp)
        setContentView(frame)
    }

    override fun onResume() { super.onResume(); glView.onResume() }
    override fun onPause()  { super.onPause();  glView.onPause()  }
}
