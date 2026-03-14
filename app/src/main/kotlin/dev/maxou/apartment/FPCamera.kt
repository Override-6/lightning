package dev.maxou.apartment

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * First-person camera: position + yaw/pitch.
 * Computes look direction; the Filament camera is updated externally via lookAt().
 */
class FPCamera {

    var posX = 0f
    var posY = 1.65f   // eye height
    var posZ = 5f      // start in living room

    var yaw   = 180f   // degrees — 180 = facing +Z (toward TV on north wall)
    var pitch = -5f    // degrees — slightly downward

    val moveSpeed = 4.0f  // m/s

    fun update(dt: Float, mx: Float, my: Float, lookDx: Float, lookDy: Float) {
        yaw   += lookDx * 0.18f
        pitch  = (pitch - lookDy * 0.18f).coerceIn(-80f, 80f)

        if (mx != 0f || my != 0f) {
            val yr   = Math.toRadians(yaw.toDouble())
            val fwdX =  sin(yr).toFloat()
            val fwdZ = -cos(yr).toFloat()
            val rgtX =  cos(yr).toFloat()
            val rgtZ =  sin(yr).toFloat()

            val newX = posX + (fwdX * my + rgtX * mx) * moveSpeed * dt
            val newZ = posZ + (fwdZ * my + rgtZ * mx) * moveSpeed * dt

            posX = newX.coerceIn(-4.7f, 4.7f)
            posZ = applyDoorCollision(posZ, newZ)
        }
    }

    /** Block passage through the shared wall at Z=0 unless inside the doorway. */
    private fun applyDoorCollision(oldZ: Float, newZ: Float): Float {
        val inDoor = abs(posX) <= 0.75f
        return when {
            oldZ >= 0f && newZ < 0f && !inDoor -> 0.05f
            oldZ <= 0f && newZ > 0f && !inDoor -> -0.05f
            else -> newZ.coerceIn(-7.7f, 7.7f)
        }
    }

    /** Direction the camera is looking (unit vector). */
    fun lookDir(): Triple<Double, Double, Double> {
        val yr = Math.toRadians(yaw.toDouble())
        val pr = Math.toRadians(pitch.toDouble())
        return Triple(
            sin(yr) * cos(pr),
            sin(pr),
            -cos(yr) * cos(pr)
        )
    }
}
