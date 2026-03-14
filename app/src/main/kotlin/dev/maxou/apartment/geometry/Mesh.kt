package dev.maxou.apartment.geometry

import android.opengl.GLES20
import dev.maxou.apartment.shader.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * GPU-side mesh: one VBO (interleaved pos+normal) + one IBO.
 * Color is a uniform set before each draw call, not per-vertex.
 *
 * Vertex layout (stride = 24 bytes):
 *   offset  0: position  (3 × float)
 *   offset 12: normal    (3 × float)
 */
class Mesh(data: MeshData, val color: FloatArray) {

    private val vboId: Int
    private val iboId: Int
    private val indexCount: Int = data.indexCount

    init {
        val ids = IntArray(2)
        GLES20.glGenBuffers(2, ids, 0)
        vboId = ids[0]
        iboId = ids[1]

        val vb = ByteBuffer.allocateDirect(data.vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .also { it.put(data.vertices); it.position(0) }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.vertices.size * 4, vb, GLES20.GL_STATIC_DRAW)

        val ib = ByteBuffer.allocateDirect(data.indices.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
            .also { it.put(data.indices); it.position(0) }
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iboId)
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, data.indices.size * 2, ib, GLES20.GL_STATIC_DRAW)
    }

    fun draw(shader: ShaderProgram, uColorLoc: Int, aPosLoc: Int, aNormLoc: Int) {
        GLES20.glUniform3fv(uColorLoc, 1, color, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        val stride = 24 // 6 floats × 4 bytes

        GLES20.glEnableVertexAttribArray(aPosLoc)
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, 0)

        GLES20.glEnableVertexAttribArray(aNormLoc)
        GLES20.glVertexAttribPointer(aNormLoc, 3, GLES20.GL_FLOAT, false, stride, 12)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iboId)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0)

        GLES20.glDisableVertexAttribArray(aPosLoc)
        GLES20.glDisableVertexAttribArray(aNormLoc)
    }
}
