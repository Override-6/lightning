package dev.maxou.apartment.geometry

/**
 * Accumulates interleaved vertex data (pos xyz + normal xyz = 6 floats/vertex)
 * and short indices. Produces geometry for one draw call.
 *
 * Winding convention: CCW = front face (GL default).
 * All quads must be wound CCW when viewed from the side the normal points toward.
 */
class MeshBuilder {

    private val verts = mutableListOf<Float>()   // 6 floats per vertex
    private val idx   = mutableListOf<Short>()
    private var vcnt  = 0

    /** Add one vertex (position + normal). */
    private fun v(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
        verts.add(x); verts.add(y); verts.add(z)
        verts.add(nx); verts.add(ny); verts.add(nz)
        vcnt++
    }

    /**
     * Add a quad as 2 triangles: (v0,v1,v2) and (v0,v2,v3).
     * Caller is responsible for CCW winding from the normal's direction.
     */
    fun quad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float,  ny: Float,  nz: Float
    ) {
        val base = vcnt.toShort()
        v(x0, y0, z0, nx, ny, nz)
        v(x1, y1, z1, nx, ny, nz)
        v(x2, y2, z2, nx, ny, nz)
        v(x3, y3, z3, nx, ny, nz)
        idx.add(base);                  idx.add((base + 1).toShort()); idx.add((base + 2).toShort())
        idx.add(base);                  idx.add((base + 2).toShort()); idx.add((base + 3).toShort())
    }

    // ── Convenience helpers ──────────────────────────────────────────────────

    /** Floor/ceiling-like horizontal quad at a fixed Y, spanning xMin..xMax × zMin..zMax.
     *  [normalUp=true] → normal +Y (floor visible from above).
     *  [normalUp=false] → normal -Y (ceiling visible from below). */
    fun horizontal(xMin: Float, y: Float, zMin: Float, xMax: Float, zMax: Float, normalUp: Boolean) {
        if (normalUp) {
            // CCW from above: (xMin,zMin)→(xMin,zMax)→(xMax,zMax)→(xMax,zMin)
            quad(xMin, y, zMin,  xMin, y, zMax,  xMax, y, zMax,  xMax, y, zMin,  0f, 1f, 0f)
        } else {
            // CCW from below: (xMin,zMin)→(xMax,zMin)→(xMax,zMax)→(xMin,zMax)
            quad(xMin, y, zMin,  xMax, y, zMin,  xMax, y, zMax,  xMin, y, zMax,  0f, -1f, 0f)
        }
    }

    /** Vertical quad on a constant-Z plane, spanning xMin..xMax × yMin..yMax.
     *  [normalPlusZ=true] → normal +Z (face visible from the +Z side).
     *  [normalPlusZ=false] → normal -Z. */
    fun wallZ(xMin: Float, yMin: Float, z: Float, xMax: Float, yMax: Float, normalPlusZ: Boolean) {
        if (normalPlusZ) {
            // CCW from +Z side: bottom-left→bottom-right→top-right→top-left
            quad(xMin, yMin, z,  xMax, yMin, z,  xMax, yMax, z,  xMin, yMax, z,  0f, 0f, 1f)
        } else {
            // CCW from -Z side
            quad(xMin, yMax, z,  xMax, yMax, z,  xMax, yMin, z,  xMin, yMin, z,  0f, 0f, -1f)
        }
    }

    /** Vertical quad on a constant-X plane, spanning zMin..zMax × yMin..yMax.
     *  [normalPlusX=true] → normal +X.
     *  [normalPlusX=false] → normal -X. */
    fun wallX(x: Float, yMin: Float, zMin: Float, yMax: Float, zMax: Float, normalPlusX: Boolean) {
        if (normalPlusX) {
            // CCW from +X: (x,yMin,zMax)→(x,yMin,zMin)→(x,yMax,zMin)→(x,yMax,zMax)
            quad(x, yMin, zMax,  x, yMin, zMin,  x, yMax, zMin,  x, yMax, zMax,  1f, 0f, 0f)
        } else {
            // CCW from -X: (x,yMin,zMin)→(x,yMin,zMax)→(x,yMax,zMax)→(x,yMax,zMin)
            quad(x, yMin, zMin,  x, yMin, zMax,  x, yMax, zMax,  x, yMax, zMin,  -1f, 0f, 0f)
        }
    }

    /** Add all 6 faces of an axis-aligned box. */
    fun box(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float) {
        horizontal(x0, y1, z0, x1, z1, normalUp = true)     // top    +Y
        horizontal(x0, y0, z0, x1, z1, normalUp = false)    // bottom -Y
        wallZ(x0, y0, z1, x1, y1, normalPlusZ = true)       // front  +Z
        wallZ(x0, y0, z0, x1, y1, normalPlusZ = false)      // back   -Z
        wallX(x1, y0, z0, y1, z1, normalPlusX = false)      // right  -X (outward from box)
        wallX(x0, y0, z0, y1, z1, normalPlusX = true)       // left   +X (outward from box)
    }

    fun build(): MeshData = MeshData(verts.toFloatArray(), idx.toShortArray())
    fun isEmpty(): Boolean = vcnt == 0
}

data class MeshData(val vertices: FloatArray, val indices: ShortArray) {
    val vertexCount: Int get() = vertices.size / 6
    val indexCount: Int get() = indices.size
}
