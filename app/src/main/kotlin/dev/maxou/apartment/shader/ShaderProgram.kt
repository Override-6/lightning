package dev.maxou.apartment.shader

import android.opengl.GLES20
import android.util.Log

class ShaderProgram(vertSrc: String, fragSrc: String) {

    val id: Int

    init {
        val vert = compile(GLES20.GL_VERTEX_SHADER, vertSrc)
        val frag = compile(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        id = GLES20.glCreateProgram()
        GLES20.glAttachShader(id, vert)
        GLES20.glAttachShader(id, frag)
        GLES20.glLinkProgram(id)
        val status = IntArray(1)
        GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("Shader", "Link error: ${GLES20.glGetProgramInfoLog(id)}")
        }
        GLES20.glDeleteShader(vert)
        GLES20.glDeleteShader(frag)
    }

    private fun compile(type: Int, src: String): Int {
        val id = GLES20.glCreateShader(type)
        GLES20.glShaderSource(id, src)
        GLES20.glCompileShader(id)
        val status = IntArray(1)
        GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("Shader", "Compile error (type=$type): ${GLES20.glGetShaderInfoLog(id)}")
        }
        return id
    }

    fun use() = GLES20.glUseProgram(id)
    fun attrib(name: String) = GLES20.glGetAttribLocation(id, name)
    fun uniform(name: String) = GLES20.glGetUniformLocation(id, name)
}
