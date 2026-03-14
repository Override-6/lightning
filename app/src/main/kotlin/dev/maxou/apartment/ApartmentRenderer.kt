package dev.maxou.apartment

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import dev.maxou.apartment.geometry.ApartmentGeometry
import dev.maxou.apartment.geometry.Mesh
import dev.maxou.apartment.shader.ShaderProgram
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ApartmentRenderer(private val touch: TouchController) : GLSurfaceView.Renderer {

    val camera = Camera()

    private lateinit var shader: ShaderProgram
    private val meshes = mutableListOf<Mesh>()

    private var uMVP    = 0
    private var uColor  = 0
    private var uLight0 = 0
    private var uLight1 = 0
    private var uAmb    = 0
    private var aPos    = 0
    private var aNorm   = 0

    private val projMatrix = FloatArray(16)
    private val vpMatrix   = FloatArray(16)
    private var aspect     = 1f

    private var lastNanos = 0L

    // ── Shaders ───────────────────────────────────────────────────────────────

    private val VERT = """
        attribute vec4 aPosition;
        attribute vec3 aNormal;
        uniform mat4 uMVP;
        varying vec3 vWorldPos;
        varying vec3 vNormal;
        void main() {
            vWorldPos = aPosition.xyz;
            vNormal   = aNormal;
            gl_Position = uMVP * aPosition;
        }
    """.trimIndent()

    private val FRAG = """
        precision mediump float;
        uniform vec3  uColor;
        uniform vec3  uLight0;
        uniform vec3  uLight1;
        uniform float uAmbient;
        varying vec3 vWorldPos;
        varying vec3 vNormal;

        float pointLight(vec3 lp) {
            vec3  dir  = lp - vWorldPos;
            float d    = length(dir);
            float att  = 1.0 / (1.0 + 0.12 * d + 0.018 * d * d);
            return max(dot(normalize(vNormal), normalize(dir)), 0.0) * att;
        }

        void main() {
            float lit = uAmbient + pointLight(uLight0) + pointLight(uLight1);
            gl_FragColor = vec4(uColor * min(lit, 1.4), 1.0);
        }
    """.trimIndent()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.08f, 0.08f, 0.10f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        shader = ShaderProgram(VERT, FRAG)
        uMVP    = shader.uniform("uMVP")
        uColor  = shader.uniform("uColor")
        uLight0 = shader.uniform("uLight0")
        uLight1 = shader.uniform("uLight1")
        uAmb    = shader.uniform("uAmbient")
        aPos    = shader.attrib("aPosition")
        aNorm   = shader.attrib("aNormal")

        ApartmentGeometry.build().forEach { (data, color) ->
            meshes.add(Mesh(data, color))
        }

        lastNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspect = width.toFloat() / height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt  = ((now - lastNanos) / 1_000_000_000f).coerceAtMost(0.1f)
        lastNanos = now

        val (ldx, ldy) = touch.consumeLookDelta()
        camera.update(dt, touch.moveX, touch.moveY, ldx, ldy)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.perspectiveM(projMatrix, 0, 75f, aspect, 0.05f, 50f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, camera.getViewMatrix(), 0)

        shader.use()
        GLES20.glUniformMatrix4fv(uMVP, 1, false, vpMatrix, 0)
        GLES20.glUniform3f(uLight0,  0f, 2.95f,  4f)   // living-room ceiling
        GLES20.glUniform3f(uLight1,  0f, 2.95f, -4f)   // bedroom ceiling
        GLES20.glUniform1f(uAmb, 0.22f)

        meshes.forEach { it.draw(shader, uColor, aPos, aNorm) }
    }
}
