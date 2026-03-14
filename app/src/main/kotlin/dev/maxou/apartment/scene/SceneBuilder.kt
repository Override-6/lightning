package dev.maxou.apartment.scene

import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.View as FilamentView
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Color
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.SphereNode

/**
 * Builds the entire 3D apartment scene: rooms, furniture, lights, post-processing.
 *
 * Coordinate system (Y-up):
 *   Living room : X −5..+5, Z  0..+8, Y 0..3
 *   Bedroom     : X −5..+5, Z −8.. 0, Y 0..3
 *   Shared wall at Z=0, doorway at X −0.8..+0.8, Y 0..2.2
 */
object SceneBuilder {

    fun build(sv: SceneView) {
        setupPostProcessing(sv)
        setupAmbientLight(sv)
        addCeilingLights(sv)
        buildRooms(sv)
        buildSharedWall(sv)
        buildLivingRoomFurniture(sv)
        buildBedroomFurniture(sv)
    }

    // ── Post-processing ───────────────────────────────────────────────────────

    private fun setupPostProcessing(sv: SceneView) {
        sv.view.apply {
            // SSAO — subtle contact shadows
            ambientOcclusionOptions = ambientOcclusionOptions.apply {
                enabled  = true
                radius   = 0.4f
                power    = 1.2f
                bias     = 0.003f
            }
            // Bloom — light glow
            bloomOptions = bloomOptions.apply {
                enabled  = true
                strength = 0.25f
                levels   = 6
            }
            // Temporal AA
            temporalAntiAliasingOptions = temporalAntiAliasingOptions.apply {
                enabled = true
            }
        }
    }

    // ── Ambient environment (flat warm IBL without skybox) ────────────────────

    private fun setupAmbientLight(sv: SceneView) {
        // Simple L0 spherical harmonics: warm neutral ambient
        val sh = floatArrayOf(0.38f, 0.34f, 0.28f)   // R, G, B constant band
        val ibl = IndirectLight.Builder()
            .irradiance(1, sh)
            .intensity(3_000f)
            .build(sv.engine)
        sv.indirectLight = ibl
    }

    // ── Ceiling lights ─────────────────────────────────────────────────────────

    private fun addCeilingLights(sv: SceneView) {
        // Living-room warm point light
        pointLight(sv, 0f, 2.9f, 4f,  intensity = 1_200f, r = 1.0f, g = 0.92f, b = 0.78f, falloff = 12f)
        // Bedroom warm point light
        pointLight(sv, 0f, 2.9f, -4f, intensity = 900f, r = 1.0f, g = 0.92f, b = 0.78f, falloff = 12f)
        // Cool soft fill (moonlight through windows)
        LightNode(sv.engine, LightManager.Type.DIRECTIONAL) {
            intensity(150f)
            color(0.72f, 0.80f, 1.0f)
            direction(0.4f, -1f, 0.3f)
            castShadows(true)
        }.also { sv.addChildNode(it) }

        // Set camera exposure for indoor (ISO 800, f/2, 1/60s)
        sv.engine.getCameraComponent(sv.cameraNode.entity)
            ?.setExposure(4f, 1f / 60f, 200f)
    }

    private fun pointLight(sv: SceneView, x: Float, y: Float, z: Float,
                           intensity: Float, r: Float, g: Float, b: Float, falloff: Float) {
        LightNode(sv.engine, LightManager.Type.POINT) {
            intensity(intensity)
            color(r, g, b)
            falloff(falloff)
            castShadows(false)
        }.also {
            it.worldPosition = Position(x, y, z)
            sv.addChildNode(it)
        }
    }

    // ── Room geometry ──────────────────────────────────────────────────────────

    private fun buildRooms(sv: SceneView) {
        val ml = sv.materialLoader

        // Shared materials
        val woodFloor = ml.pbr(0.62f, 0.40f, 0.22f, rough = 0.78f)
        val ceiling   = ml.pbr(0.95f, 0.95f, 0.93f, rough = 0.97f)
        val wallLR    = ml.pbr(0.88f, 0.82f, 0.72f, rough = 0.96f)  // living-room beige
        val wallBR    = ml.pbr(0.70f, 0.79f, 0.68f, rough = 0.96f)  // bedroom sage-green

        // Floor (both rooms)
        sv.cube(woodFloor, 10f, 0.02f, 16f, 0f, -0.01f, 0f)
        // Ceiling
        sv.cube(ceiling, 10f, 0.02f, 16f, 0f, 3.01f, 0f)

        // Living-room walls
        sv.cube(wallLR, 10.2f, 3f, 0.12f, 0f, 1.5f, 8.06f)          // north Z=8
        sv.cube(wallLR, 0.12f, 3f, 8.12f,  5.06f, 1.5f, 4f)          // east  X=5 LR
        sv.cube(wallLR, 0.12f, 3f, 8.12f, -5.06f, 1.5f, 4f)          // west  X=-5 LR
        // Window opening on east wall of living room (fake glass inset)
        val glass = ml.pbr(0.55f, 0.72f, 0.88f, rough = 0.05f, metal = 0f, refl = 0.85f)
        sv.cube(glass, 0.04f, 1.3f, 3.8f, 5.02f, 1.55f, 4f)
        val winFrame = ml.pbr(0.90f, 0.90f, 0.90f, rough = 0.6f)
        sv.cube(winFrame, 0.1f, 0.1f, 4.0f, 5.05f, 0.95f, 4f)         // sill bottom
        sv.cube(winFrame, 0.1f, 0.1f, 4.0f, 5.05f, 2.22f, 4f)         // sill top
        sv.cube(winFrame, 0.1f, 1.3f, 0.1f, 5.05f, 1.55f, 2.05f)      // left side
        sv.cube(winFrame, 0.1f, 1.3f, 0.1f, 5.05f, 1.55f, 5.95f)      // right side

        // Bedroom walls
        sv.cube(wallBR, 10.2f, 3f, 0.12f, 0f, 1.5f, -8.06f)          // south Z=-8
        sv.cube(wallBR, 0.12f, 3f, 8.12f,  5.06f, 1.5f, -4f)          // east  X=5 BR
        sv.cube(wallBR, 0.12f, 3f, 8.12f, -5.06f, 1.5f, -4f)          // west  X=-5 BR
        // Bedroom window on west wall
        sv.cube(glass, 0.04f, 1.3f, 3.8f, -5.02f, 1.55f, -4f)
        sv.cube(winFrame, 0.1f, 0.1f, 4.0f, -5.05f, 0.95f, -4f)
        sv.cube(winFrame, 0.1f, 0.1f, 4.0f, -5.05f, 2.22f, -4f)
        sv.cube(winFrame, 0.1f, 1.3f, 0.1f, -5.05f, 1.55f, -2.05f)
        sv.cube(winFrame, 0.1f, 1.3f, 0.1f, -5.05f, 1.55f, -5.95f)
    }

    // ── Shared wall with doorway ───────────────────────────────────────────────

    private fun buildSharedWall(sv: SceneView) {
        val ml = sv.materialLoader
        // Two different paints on each side
        val wallLR = ml.pbr(0.88f, 0.82f, 0.72f, rough = 0.96f)
        val wallBR = ml.pbr(0.70f, 0.79f, 0.68f, rough = 0.96f)
        val door   = ml.pbr(0.62f, 0.44f, 0.22f, rough = 0.55f)  // wood frame

        // Wall segments — living-room side (material = wallLR, facing +Z) and
        // bedroom side (material = wallBR, facing -Z) are separate CubeNodes
        // offset ±0.055 so both are visible from their respective rooms
        listOf(
            Triple(-2.9f, 1.5f, 3f),     // left of door (x=-5 to -0.8)
            Triple( 2.9f, 1.5f, 3f),     // right of door
            Triple( 0.0f, 2.6f, 0.8f),   // above door (y=2.2 to 3)
        ).forEachIndexed { i, (px, py, sz) ->
            val sx = if (i < 2) 4.2f else 1.6f
            val sy = if (i < 2) 3.0f else 0.8f
            val ox = if (i == 0) -2.9f else if (i == 1) 2.9f else 0.0f
            sv.cube(wallLR, sx, sy, 0.1f, ox, py, 0.05f)
            sv.cube(wallBR, sx, sy, 0.1f, ox, py, -0.05f)
        }

        // Door frame
        sv.cube(door, 0.18f, 2.35f, 0.18f, -0.9f, 1.175f, 0f)   // left post
        sv.cube(door, 0.18f, 2.35f, 0.18f,  0.9f, 1.175f, 0f)   // right post
        sv.cube(door, 1.98f, 0.18f, 0.18f,  0.0f, 2.31f,   0f)  // top bar
    }

    // ── Living-room furniture ──────────────────────────────────────────────────

    private fun buildLivingRoomFurniture(sv: SceneView) {
        val ml = sv.materialLoader

        val sofa     = ml.pbr(0.18f, 0.24f, 0.48f, rough = 0.98f)          // navy fabric
        val sofaDark = ml.pbr(0.14f, 0.18f, 0.38f, rough = 0.98f)
        val wood     = ml.pbr(0.55f, 0.36f, 0.16f, rough = 0.60f)
        val tvDark   = ml.pbr(0.12f, 0.12f, 0.13f, rough = 0.50f, metal = 0.45f)
        val screen   = ml.pbr(0.02f, 0.02f, 0.02f, rough = 0.15f, metal = 0.6f)
        val screenGlow = ml.pbr(0.08f, 0.14f, 0.22f, rough = 0.15f)         // faint blue glow
        val shelf    = ml.pbr(0.58f, 0.38f, 0.18f, rough = 0.65f)
        val lampMetal= ml.pbr(0.72f, 0.58f, 0.22f, rough = 0.30f, metal = 0.85f) // brass
        val lampShade= ml.pbr(0.95f, 0.90f, 0.75f, rough = 0.85f)
        val plantPot = ml.pbr(0.50f, 0.32f, 0.22f, rough = 0.80f)
        val plant    = ml.pbr(0.20f, 0.50f, 0.20f, rough = 0.95f)
        val rug      = ml.pbr(0.55f, 0.38f, 0.50f, rough = 1.0f)

        // Sofa — seat + back + arms + 3 cushions
        sv.cube(sofa, 5.2f, 0.45f, 1.6f,  -2.0f, 0.225f, 1.2f)         // seat
        sv.cube(sofaDark, 5.2f, 0.6f, 0.5f, -2.0f, 0.75f,  0.55f)      // backrest
        sv.cube(sofaDark, 0.5f, 0.7f, 1.6f, -4.6f, 0.35f,  1.2f)       // left arm
        sv.cube(sofaDark, 0.5f, 0.7f, 1.6f,  0.6f, 0.35f,  1.2f)       // right arm
        // Cushions (3 slightly raised boxes)
        val cushColor = ml.pbr(0.22f, 0.28f, 0.56f, rough = 1.0f)
        sv.cube(cushColor, 1.5f, 0.18f, 1.3f, -3.7f, 0.54f, 1.2f)
        sv.cube(cushColor, 1.5f, 0.18f, 1.3f, -2.0f, 0.54f, 1.2f)
        sv.cube(cushColor, 1.5f, 0.18f, 1.3f, -0.3f, 0.54f, 1.2f)

        // Coffee table — round top + 4 cylinder legs
        sv.cube(wood, 3.5f, 0.06f, 2.0f, -1.5f, 0.42f, 4.5f)          // top slab
        listOf(-2.8f to 3.7f, -0.2f to 3.7f, -2.8f to 5.3f, -0.2f to 5.3f).forEach { (lx, lz) ->
            sv.cylinder(wood, radius = 0.05f, height = 0.40f, x = lx, y = 0.20f, z = lz)
        }

        // TV unit + TV
        sv.cube(tvDark, 8.0f, 0.50f, 0.55f, 0f, 0.25f, 7.75f)          // unit
        sv.cube(tvDark, 7.4f, 0.06f, 0.45f, 0f, 0.53f, 7.75f)          // lip
        sv.cube(tvDark, 7.0f, 1.40f, 0.12f, 0f, 1.20f, 7.80f)          // TV bezel
        sv.cube(screen,     6.6f, 1.22f, 0.06f, 0f, 1.20f, 7.82f)      // screen black
        sv.cube(screenGlow, 6.4f, 1.10f, 0.04f, 0f, 1.20f, 7.83f)      // glow layer
        // TV legs (2 cylinders)
        sv.cylinder(tvDark, 0.06f, 0.08f, -1.5f, 0.54f, 7.73f)
        sv.cylinder(tvDark, 0.06f, 0.08f,  1.5f, 0.54f, 7.73f)

        // Bookshelf (west wall)
        sv.cube(shelf, 0.70f, 2.20f, 2.80f, -4.65f, 1.10f, 6.50f)     // carcass
        for (yh in listOf(0.55f, 1.10f, 1.65f)) {
            sv.cube(wood, 0.70f, 0.04f, 2.80f, -4.65f, yh, 6.50f)      // shelf boards
        }

        // Floor lamp (corner near sofa)
        sv.cylinder(lampMetal, 0.03f, 1.55f, 1.0f, 0.775f, 1.4f)      // pole
        sv.sphere(lampMetal,   0.06f, 1.0f, 1.57f, 1.4f)               // top ball
        sv.cylinder(lampShade, 0.22f, 0.30f, 1.0f, 1.50f, 1.4f)       // shade body

        // Small plant (corner by TV)
        sv.cylinder(plantPot, 0.14f, 0.22f, 3.5f, 0.11f, 7.0f)        // pot
        sv.sphere(plant, 0.25f, 3.5f, 0.58f, 7.0f)                     // foliage ball

        // Rug under coffee table
        sv.cube(rug, 4.5f, 0.018f, 3.0f, -1.5f, 0.009f, 4.5f)
    }

    // ── Bedroom furniture ──────────────────────────────────────────────────────

    private fun buildBedroomFurniture(sv: SceneView) {
        val ml = sv.materialLoader

        val bedFrame = ml.pbr(0.50f, 0.36f, 0.20f, rough = 0.60f)
        val mattress = ml.pbr(0.92f, 0.90f, 0.85f, rough = 0.90f)
        val pillow   = ml.pbr(0.95f, 0.92f, 0.84f, rough = 0.88f)
        val duvet    = ml.pbr(0.88f, 0.85f, 0.78f, rough = 0.92f)
        val wardrobe = ml.pbr(0.80f, 0.72f, 0.56f, rough = 0.62f)
        val wardMetal= ml.pbr(0.60f, 0.60f, 0.60f, rough = 0.30f, metal = 0.8f)
        val nightStd = ml.pbr(0.52f, 0.38f, 0.20f, rough = 0.65f)
        val lampShade= ml.pbr(0.95f, 0.90f, 0.75f, rough = 0.85f)
        val lampMetal= ml.pbr(0.75f, 0.60f, 0.24f, rough = 0.28f, metal = 0.85f)
        val plant    = ml.pbr(0.22f, 0.52f, 0.22f, rough = 0.95f)
        val pot      = ml.pbr(0.48f, 0.30f, 0.20f, rough = 0.80f)
        val rug      = ml.pbr(0.42f, 0.36f, 0.52f, rough = 1.0f)

        // Bed frame
        sv.cube(bedFrame, 5.2f, 0.30f, 5.6f, -1.5f, 0.15f, -4.8f)     // base
        sv.cube(bedFrame, 5.2f, 1.10f, 0.20f, -1.5f, 0.85f, -7.5f)    // headboard
        sv.cube(bedFrame, 5.2f, 0.40f, 0.20f, -1.5f, 0.50f, -2.0f)    // footboard
        // Bed legs (4 cylinders)
        listOf(-4.0f to -7.4f, 1.0f to -7.4f, -4.0f to -2.1f, 1.0f to -2.1f).forEach { (lx, lz) ->
            sv.cylinder(bedFrame, 0.06f, 0.14f, lx, 0.07f, lz)
        }
        // Mattress + duvet + pillows
        sv.cube(mattress, 4.8f, 0.24f, 5.0f, -1.5f, 0.42f, -4.8f)
        sv.cube(duvet,    4.7f, 0.14f, 3.6f, -1.5f, 0.67f, -4.0f)     // duvet (partial)
        sv.sphere(pillow, 0.26f, -3.0f, 0.74f, -7.1f)                  // pillow L
        sv.sphere(pillow, 0.26f,  0.0f, 0.74f, -7.1f)                  // pillow R

        // Wardrobe (east wall)
        sv.cube(wardrobe, 3.5f, 2.40f, 0.60f, 3.25f, 1.20f, -6.0f)   // body
        // Wardrobe doors (2 panels with slight bevel)
        sv.cube(wardrobe, 1.65f, 2.30f, 0.06f, 2.35f, 1.20f, -5.73f)
        sv.cube(wardrobe, 1.65f, 2.30f, 0.06f, 4.15f, 1.20f, -5.73f)
        // Door handles
        sv.cylinder(wardMetal, 0.02f, 0.22f, 2.90f, 1.20f, -5.70f)
        sv.cylinder(wardMetal, 0.02f, 0.22f, 3.70f, 1.20f, -5.70f)
        // Mirror on door
        val mirror = ml.pbr(0.72f, 0.78f, 0.80f, rough = 0.04f, metal = 0f, refl = 0.95f)
        sv.cube(mirror, 1.0f, 1.4f, 0.03f, 2.35f, 1.50f, -5.71f)

        // Nightstand + bedside lamp
        sv.cube(nightStd, 0.70f, 0.52f, 0.65f, 2.0f, 0.26f, -3.8f)
        sv.cylinder(lampMetal, 0.025f, 0.36f, 2.0f, 0.70f, -3.8f)     // lamp stem
        sv.sphere(lampMetal,   0.04f,  2.0f, 1.08f, -3.8f)             // lamp knob
        sv.cylinder(lampShade, 0.16f, 0.22f, 2.0f, 0.96f, -3.8f)      // shade

        // Bedroom plant
        sv.cylinder(pot,   0.12f, 0.20f, -4.0f, 0.10f, -1.6f)
        sv.sphere(plant,   0.22f, -4.0f, 0.50f, -1.6f)

        // Bedroom rug
        sv.cube(rug, 4.0f, 0.018f, 3.2f, -1.5f, 0.009f, -4.5f)
    }

    // ── Node helpers ───────────────────────────────────────────────────────────

    private fun SceneView.cube(
        mat: com.google.android.filament.MaterialInstance,
        sx: Float, sy: Float, sz: Float,
        px: Float, py: Float, pz: Float
    ) = CubeNode(engine, Size(sx, sy, sz), materialInstance = mat)
        .also { it.worldPosition = Position(px, py, pz); addChildNode(it) }

    private fun SceneView.cylinder(
        mat: com.google.android.filament.MaterialInstance,
        radius: Float, height: Float,
        x: Float, y: Float, z: Float,
        sides: Int = 24
    ) = CylinderNode(engine, radius, height, materialInstance = mat)
        .also { it.worldPosition = Position(x, y, z); addChildNode(it) }

    private fun SceneView.sphere(
        mat: com.google.android.filament.MaterialInstance,
        radius: Float,
        x: Float, y: Float, z: Float
    ) = SphereNode(engine, radius, materialInstance = mat)
        .also { it.worldPosition = Position(x, y, z); addChildNode(it) }

    // ── Material shorthand ─────────────────────────────────────────────────────

    private fun MaterialLoader.pbr(
        r: Float, g: Float, b: Float,
        rough: Float,
        metal: Float = 0f,
        refl: Float  = 0.5f
    ) = createColorInstance(Color(r, g, b, 1f), metal, rough, refl)
}
