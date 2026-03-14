package dev.maxou.apartment

import android.content.Context
import android.view.MotionEvent
import dev.maxou.apartment.scene.SceneBuilder
import io.github.sceneview.SceneView
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position

/**
 * Main 3D view: wraps SceneView (Filament PBR renderer), manages first-person
 * camera via FPCamera + touch, builds the scene geometry via SceneBuilder.
 */
class ApartmentSceneView(
    context: Context,
    private val touch: TouchController
) : SceneView(
    context = context,
    cameraManipulator = null   // disable default orbit-camera gesture
) {

    private val fpCam = FPCamera()
    private var lastFrameNanos = 0L

    init {
        SceneBuilder.build(this)

        onFrame = { frameTimeNanos ->
            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameTimeNanos
            } else {
                val dt = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.1f)
                lastFrameNanos = frameTimeNanos

                val (ldx, ldy) = touch.consumeLookDelta()
                fpCam.update(dt, touch.moveX, touch.moveY, ldx, ldy)

                val (dx, dy, dz) = fpCam.lookDir()
                cameraNode.lookAt(
                    eye    = Position(fpCam.posX, fpCam.posY, fpCam.posZ),
                    center = Position(
                        (fpCam.posX + dx).toFloat(),
                        (fpCam.posY + dy).toFloat(),
                        (fpCam.posZ + dz).toFloat()
                    ),
                    up = Direction(y = 1f)
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        touch.onTouchEvent(event, width)
        return true   // fully consume; no orbit-camera fallthrough
    }
}
