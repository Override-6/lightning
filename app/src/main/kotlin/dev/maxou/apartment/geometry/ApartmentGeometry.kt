package dev.maxou.apartment.geometry

/**
 * Procedural apartment geometry.
 *
 * Coordinate system (right-hand, Y-up):
 *   Living room : X −5..+5,  Z  0..+8,  Y 0..3
 *   Bedroom     : X −5..+5,  Z −8..0,   Y 0..3
 *   Shared wall at Z=0 with doorway: X −0.8..+0.8, Y 0..2.2
 *
 * All geometry uses CCW front-face winding (GL default).
 */
object ApartmentGeometry {

    // ── Colors ───────────────────────────────────────────────────────────────
    private val C_FLOOR    = f(0.65f, 0.42f, 0.24f)   // warm wood
    private val C_CEILING  = f(0.95f, 0.95f, 0.93f)   // off-white
    private val C_WALL_LR  = f(0.88f, 0.82f, 0.72f)   // living-room beige
    private val C_WALL_BR  = f(0.72f, 0.80f, 0.70f)   // bedroom sage-green
    private val C_SOFA     = f(0.22f, 0.28f, 0.52f)   // navy blue
    private val C_WOOD     = f(0.60f, 0.40f, 0.20f)   // furniture wood
    private val C_DARK     = f(0.15f, 0.15f, 0.16f)   // TV / dark
    private val C_BED      = f(0.55f, 0.40f, 0.26f)   // bed-frame wood
    private val C_MATTRESS = f(0.90f, 0.90f, 0.85f)   // white/cream
    private val C_WARDROBE = f(0.72f, 0.64f, 0.50f)   // wardrobe beige
    private val C_LAMP     = f(0.95f, 0.90f, 0.72f)   // lamp shade warm
    private val C_GLASS    = f(0.55f, 0.72f, 0.88f)   // window glass blue
    private val C_FRAME    = f(0.90f, 0.90f, 0.90f)   // window frame white
    private val C_DOORFRM  = f(0.62f, 0.44f, 0.22f)   // door frame wood

    private fun f(r: Float, g: Float, b: Float) = floatArrayOf(r, g, b)

    // ── Public API ───────────────────────────────────────────────────────────

    /** Returns a list of (MeshData, color) pairs ready to upload as Mesh objects. */
    fun build(): List<Pair<MeshData, FloatArray>> {
        val result = mutableListOf<Pair<MeshData, FloatArray>>()

        fun add(color: FloatArray, block: MeshBuilder.() -> Unit) {
            val mb = MeshBuilder()
            mb.block()
            if (!mb.isEmpty()) result.add(mb.build() to color)
        }

        // ── Floors ───────────────────────────────────────────────────────────
        add(C_FLOOR) {
            horizontal(-5f, 0f, 0f, 5f, 8f, normalUp = true)   // living room
            horizontal(-5f, 0f, -8f, 5f, 0f, normalUp = true)  // bedroom
        }

        // ── Ceilings ─────────────────────────────────────────────────────────
        add(C_CEILING) {
            horizontal(-5f, 3f, 0f, 5f, 8f, normalUp = false)
            horizontal(-5f, 3f, -8f, 5f, 0f, normalUp = false)
        }

        // ── Living-room walls ────────────────────────────────────────────────
        add(C_WALL_LR) {
            // North wall (z=8, normal −Z, seen from inside living room)
            wallZ(-5f, 0f, 8f, 5f, 3f, normalPlusZ = false)
            // East wall (x=+5, normal −X)
            wallX(5f, 0f, 0f, 3f, 8f, normalPlusX = false)
            // West wall (x=−5, normal +X)
            wallX(-5f, 0f, 0f, 3f, 8f, normalPlusX = true)
            // South / shared wall seen from living room (z=0, normal +Z)
            //   Left segment
            wallZ(-5f, 0f, 0f, -0.8f, 3f, normalPlusZ = true)
            //   Right segment
            wallZ(0.8f, 0f, 0f, 5f, 3f, normalPlusZ = true)
            //   Above-doorway segment
            wallZ(-0.8f, 2.2f, 0f, 0.8f, 3f, normalPlusZ = true)
        }

        // ── Bedroom walls ────────────────────────────────────────────────────
        add(C_WALL_BR) {
            // South wall (z=−8, normal +Z)
            wallZ(-5f, 0f, -8f, 5f, 3f, normalPlusZ = true)
            // East wall
            wallX(5f, 0f, -8f, 3f, 0f, normalPlusX = false)
            // West wall
            wallX(-5f, 0f, -8f, 3f, 0f, normalPlusX = true)
            // North / shared wall seen from bedroom (z=0, normal −Z)
            wallZ(-5f, 0f, 0f, -0.8f, 3f, normalPlusZ = false)
            wallZ(0.8f, 0f, 0f, 5f, 3f, normalPlusZ = false)
            wallZ(-0.8f, 2.2f, 0f, 0.8f, 3f, normalPlusZ = false)
        }

        // ── Door frame ───────────────────────────────────────────────────────
        add(C_DOORFRM) {
            box(-1.0f, 0f, -0.12f, -0.8f, 2.35f, 0.12f)   // left post
            box( 0.8f, 0f, -0.12f,  1.0f, 2.35f, 0.12f)   // right post
            box(-1.0f, 2.2f, -0.12f, 1.0f, 2.4f, 0.12f)   // top bar
        }

        // ── Living-room windows (east wall) ──────────────────────────────────
        add(C_FRAME) {
            box(4.85f, 0.85f, 2.0f, 5.0f, 2.3f, 2.15f)   // left sill
            box(4.85f, 0.85f, 5.85f, 5.0f, 2.3f, 6.0f)   // right sill
            box(4.85f, 2.15f, 2.0f, 5.0f, 2.3f, 6.0f)    // top
            box(4.85f, 0.85f, 2.0f, 5.0f, 1.0f, 6.0f)    // bottom sill
        }
        add(C_GLASS) { box(4.9f, 1.0f, 2.15f, 5.0f, 2.15f, 5.85f) }

        // ── Bedroom windows (west wall) ───────────────────────────────────────
        add(C_FRAME) {
            box(-5.0f, 0.85f, -6.0f, -4.85f, 2.3f, -5.85f)
            box(-5.0f, 0.85f, -2.15f, -4.85f, 2.3f, -2.0f)
            box(-5.0f, 2.15f, -6.0f, -4.85f, 2.3f, -2.0f)
            box(-5.0f, 0.85f, -6.0f, -4.85f, 1.0f, -2.0f)
        }
        add(C_GLASS) { box(-5.0f, 1.0f, -5.85f, -4.9f, 2.15f, -2.15f) }

        // ── Living-room furniture ─────────────────────────────────────────────
        // Sofa (against south wall, facing TV)
        add(C_SOFA) {
            box(-4.5f, 0f, 0.4f, 0.5f, 0.45f, 2.0f)       // seat
            box(-4.5f, 0.45f, 0.4f, 0.5f, 1.0f, 0.9f)     // back
            box(-4.5f, 0f, 0.4f, -3.9f, 0.75f, 2.0f)      // left arm
            box(-0.1f, 0f, 0.4f,  0.5f, 0.75f, 2.0f)      // right arm
        }
        // Coffee table
        add(C_WOOD) {
            box(-3.0f, 0f, 3.0f, 1.0f, 0.42f, 5.5f)       // top slab
            box(-2.9f, 0f, 3.1f, -2.6f, 0.40f, 3.4f)      // leg FL
            box( 0.7f, 0f, 3.1f,  0.9f, 0.40f, 3.4f)      // leg FR
            box(-2.9f, 0f, 5.1f, -2.6f, 0.40f, 5.4f)      // leg BL
            box( 0.7f, 0f, 5.1f,  0.9f, 0.40f, 5.4f)      // leg BR
        }
        // TV stand + TV
        add(C_DARK) {
            box(-4.0f, 0f, 7.4f, 4.0f, 0.55f, 7.9f)       // stand
            box(-3.7f, 0.55f, 7.5f, 3.7f, 1.85f, 7.7f)    // TV bezel
        }
        add(f(0.03f, 0.03f, 0.03f)) {
            box(-3.5f, 0.60f, 7.52f, 3.5f, 1.80f, 7.65f)  // screen
        }
        // Bookshelf
        add(C_WOOD) {
            box(-4.9f, 0f, 5.0f, -4.1f, 2.2f, 7.9f)
            box(-4.9f, 0.5f, 5.0f, -4.1f, 0.55f, 7.9f)    // shelf 1
            box(-4.9f, 1.1f, 5.0f, -4.1f, 1.15f, 7.9f)    // shelf 2
            box(-4.9f, 1.7f, 5.0f, -4.1f, 1.75f, 7.9f)    // shelf 3
        }
        // Floor lamp (corner)
        add(f(0.30f, 0.30f, 0.30f)) {
            box( 3.6f, 0f, 7.0f,  3.8f, 1.6f, 7.2f)       // pole
        }
        add(C_LAMP) {
            box( 3.3f, 1.6f, 6.9f,  4.1f, 2.1f, 7.5f)     // shade
        }

        // ── Bedroom furniture ──────────────────────────────────────────────────
        // Bed frame + mattress
        add(C_BED) {
            box(-4.0f, 0f, -7.6f, 1.0f, 0.32f, -2.0f)     // frame
            box(-4.0f, 0.32f, -7.6f, 1.0f, 1.15f, -7.3f)  // headboard
        }
        add(C_MATTRESS) {
            box(-3.8f, 0.32f, -7.4f, 0.8f, 0.56f, -2.2f)  // mattress
            box(-3.6f, 0.56f, -7.2f, -1.9f, 0.82f, -6.5f) // pillow L
            box(-0.5f, 0.56f, -7.2f,  0.6f, 0.82f, -6.5f) // pillow R
            // bedcover raised bump
            box(-3.75f, 0.56f, -6.5f, 0.75f, 0.70f, -2.5f)
        }
        // Wardrobe
        add(C_WARDROBE) {
            box(1.5f, 0f, -7.9f, 4.9f, 2.4f, -5.0f)
            box(1.5f, 0f, -7.88f, 3.2f, 2.38f, -7.82f)    // left door
            box(3.2f, 0f, -7.88f, 4.9f, 2.38f, -7.82f)    // right door
            box(3.15f, 0.9f, -7.9f, 3.25f, 1.1f, -7.7f)   // handles
        }
        // Nightstand + lamp
        add(C_BED) {
            box(1.5f, 0f, -4.8f, 2.8f, 0.55f, -3.5f)
        }
        add(f(0.85f, 0.85f, 0.85f)) {
            box(1.85f, 0.55f, -4.45f, 2.35f, 0.68f, -3.95f) // lamp base
        }
        add(C_LAMP) {
            box(1.60f, 0.68f, -4.65f, 2.60f, 1.18f, -3.75f)  // lamp shade
        }
        // Bedroom rug
        add(f(0.50f, 0.38f, 0.55f)) {
            box(-3.8f, 0.01f, -4.8f, 1.0f, 0.015f, -2.2f)  // rug (thin slab)
        }

        return result
    }
}
