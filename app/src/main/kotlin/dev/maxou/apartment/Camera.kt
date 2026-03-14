package dev.maxou.apartment

import android.opengl.Matrix
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class Camera {

    var posX = 0f
    var posY = 1.65f   // eye height
    var posZ = 5f      // start in centre of living room

    var yaw   = 180f   // degrees: 0=facing −Z, 180=facing +Z (toward TV)
    var pitch = -5f    // degrees: slightly downward

    val moveSpeed = 4.0f   // m/s

    private val viewMatrix = FloatArray(16)

    /** Apply movement and look input. [dt] in seconds. [mx]/[my] are joystick −1..+1 axes. */
    fun update(dt: Float, mx: Float, my: Float, lookDx: Float, lookDy: Float) {
        // Look
        yaw   += lookDx * 0.18f
        pitch  = (pitch - lookDy * 0.18f).coerceIn(-80f, 80f)

        // Movement – forward & strafe in XZ plane
        if (mx != 0f || my != 0f) {
            val yRad = Math.toRadians(yaw.toDouble())
            val fwdX = sin(yRad).toFloat()
            val fwdZ = -cos(yRad).toFloat()
            val rgtX = cos(yRad).toFloat()
            val rgtZ = sin(yRad).toFloat()

            val newX = posX + (fwdX * my + rgtX * mx) * moveSpeed * dt
            val newZ = posZ + (fwdZ * my + rgtZ * mx) * moveSpeed * dt

            posX = newX.coerceIn(-4.7f, 4.7f)
            posZ = applyDoorwayCollision(posZ, newZ, posX)
        }
    }

    /** Prevent walking through the shared wall at Z=0 except through the doorway. */
    private fun applyDoorwayCollision(oldZ: Float, newZ: Float, x: Float): Float {
        val inDoorway = abs(x) <= 0.75f
        return when {
            oldZ >= 0f && newZ < 0f && !inDoorway -> 0.05f   // blocked entering bedroom
            oldZ <= 0f && newZ > 0f && !inDoorway -> -0.05f  // blocked entering living room
            else -> newZ.coerceIn(-7.7f, 7.7f)
        }
    }

    fun getViewMatrix(): FloatArray {
        val yRad = Math.toRadians(yaw.toDouble())
        val pRad = Math.toRadians(pitch.toDouble())
        val dirX = (sin(yRad) * cos(pRad)).toFloat()
        val dirY = sin(pRad).toFloat()
        val dirZ = (-cos(yRad) * cos(pRad)).toFloat()
        Matrix.setLookAtM(
            viewMatrix, 0,
            posX, posY, posZ,
            posX + dirX, posY + dirY, posZ + dirZ,
            0f, 1f, 0f
        )
        return viewMatrix
    }
}
