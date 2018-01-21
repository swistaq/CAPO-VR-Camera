package com.agh.panda.threed.gl;


import android.content.Context;
import android.graphics.*;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class VideoTextureRenderer extends TextureSurfaceRenderer implements SurfaceTexture.OnFrameAvailableListener {
    //black&white
    private static final String blackAndWhiteFragmentShaderCode = "#extension GL_OES_EGL_image_external : require   " +
            "precision mediump float;                                                                               " +
            "uniform samplerExternalOES texture;                                                                    " +
            "varying vec2 v_TexCoordinate;                                                                          " +
            "void main() {                                                                                          " +
            "    vec4 color = texture2D(texture, v_TexCoordinate);                                                  " +
            "    float colorR = (color.r + color.g + color.b) / 3.0;                                                " +
            "    float colorG = (color.r + color.g + color.b) / 3.0;                                                " +
            "    float colorB = (color.r + color.g + color.b) / 3.0;                                                " +
            "    gl_FragColor = vec4(colorR, colorG, colorB, color.a);                                              " +
            "}                                                                                                      ";

    //clean shader
    private static final String cleanFragmentShaderCode = "#extension GL_OES_EGL_image_external : require\n     " +
            "precision mediump float;                                                                           " +
            "uniform samplerExternalOES texture;                                                                " +
            "varying vec2 v_TexCoordinate;                                                                      " +
            "void main () {                                                                                     " +
            "    vec4 color = texture2D(texture, v_TexCoordinate);                                              " +
            "    gl_FragColor = color;                                                                          " +
            "}                                                                                                  ";

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //clean shader
    private static final String cleanVertexShaderCode = "#version 300 es\n              " +
            "in vec4 vPosition;                                                         " +
            "in vec4 vTexCoordinate;                                                    " +
            "uniform mat4 textureTransform;                                             " +
            "out vec2 texFrg;                                                           " +
            "void main() {                                                              " +
            "   texFrg = (textureTransform * vTexCoordinate).xy;                        " +
            "   gl_Position = vPosition;                                                " +
            "}                                                                          ";

    // wybulone na prostokątnym obrazie, po czym docięte do kwadrata po wybuleniu, powiedzmy że działa
    private static final String parallaxFragmentShaderCode = "#version 300 es\n                     " +
            "#extension GL_OES_EGL_image_external_essl3 : require\n                                 " +
            "in highp vec2 texFrg;                                                                  " +
            "out highp vec4 frgCol;                                                                 " +
            "uniform highp samplerExternalOES textura;                                              " +
            "void main(void){                                                                       " +
            "    highp vec4 col = vec4(0.0, 0.0, 0.0, 0.0); /* base colour */                       " +
            "    highp float alpha = 0.3; /* lens parameter */                                      " +
            "    highp vec2 p1 = vec2(2.0 * texFrg - 1.0);                                          " +
            "    p1 = vec2(p1[0]*2.0, p1[1]);                                                       " +
            "    highp vec2 p2 = p1 / (1.0 - alpha * length(p1));                                   " +
            "    p2 = vec2(p2[0]/2.0, p2[1]);                                                       " +
            "    p2 = (p2 + 1.0) * 0.5;                                                             " +
            "    if (all(lessThanEqual(p2, vec2(0.75, 1.0)))                                        " +
            "       && all(greaterThanEqual(p2, vec2(0.25, 0.0)))){                                 " +
            "        col = texture(textura, p2);                                                    " +
            "    }                                                                                  " +
            "    p2 = p2 * 2.1;"+
            "    frgCol = col;                                                                      " +
            "}                                                                                      ";

    private static final String meshParallaxFragmentShaderCode = "#version 300 es\n                 " +
            "#extension GL_OES_EGL_image_external : require\n                                       " +
            "in highp vec2 texFrg;                                                                  " +
            "out highp vec4 frgCol;                                                                 " +
            "uniform highp samplerExternalOES textura;                                              " +
            "void main(void) {                                                                      " +
            "    highp vec4 col = vec4(0.0, 0.0, 0.0, 1.0); /* base colour */                       " +
            "    if (all(greaterThanEqual(texFrg, vec2(0.0))) &&                                    " +
            "        all(lessThanEqual(texFrg, vec2(1.0)))) {                                       " +
            "        col = texture(textura, texFrg);                                                " +
            "    }                                                                                  " +
            "    frgCol = col;                                                                      " +
            "}                                                                                      ";

    private static final String vertexShaderCode = cleanVertexShaderCode;
    private static final String fragmentShaderCode = parallaxFragmentShaderCode;

    private static float squareSize = 1.0f;
    private static float squareCoords[] = {-squareSize, squareSize, 0.0f,   // top left
            -squareSize, -squareSize, 0.0f,   // bottom left
            squareSize, -squareSize, 0.0f,   // bottom right
            squareSize, squareSize, 0.0f}; // top right

    private static short drawOrder[] = {0, 1, 2, 0, 2, 3};

    private Context ctx;

    // Texture to be shown in backgrund
    private FloatBuffer textureBuffer;
    private float textureCoords[] = {0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f};
    private int[] textures = new int[1];

    private int vertexShaderHandle;
    private int fragmentShaderHandle;
    private int shaderProgram;
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private SurfaceTexture videoTexture;
    private float[] videoTextureTransform;
    private boolean frameAvailable = false;

    private int videoWidth;
    private int videoHeight;
    private boolean adjustViewport = false;

    public VideoTextureRenderer(Context context, SurfaceTexture texture, int width, int height) {
        super(texture, width, height);
        this.ctx = context;
        videoTextureTransform = new float[16];
    }

    private void loadShaders() {
        vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShaderHandle, vertexShaderCode);
        GLES20.glCompileShader(vertexShaderHandle);
        checkGlError("Vertex shader compile");

        fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShaderHandle, fragmentShaderCode);
        GLES20.glCompileShader(fragmentShaderHandle);
        checkGlError("Pixel shader compile");

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShaderHandle);
        GLES20.glAttachShader(shaderProgram, fragmentShaderHandle);
        GLES20.glLinkProgram(shaderProgram);
        checkGlError("Shader program compile");

        int[] status = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(shaderProgram);
            Log.e("Shader", "Vertex Shader log: " + GLES20.glGetShaderInfoLog(vertexShaderHandle));
            Log.e("Shader", "Fragment Shader log: " + GLES20.glGetShaderInfoLog(fragmentShaderHandle));
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

    }


    private void setupVertexBuffer() {
        // Draw list buffer
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // Initialize the texture holder
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);
    }


    private void setupTexture(Context context) {
        ByteBuffer texturebb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        texturebb.order(ByteOrder.nativeOrder());

        textureBuffer = texturebb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);

        // Generate the actual texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        checkGlError("Texture bind");

        videoTexture = new SurfaceTexture(textures[0]);
        videoTexture.setOnFrameAvailableListener(this);
    }

    @Override
    protected boolean draw() {
        synchronized (this) {
            if (frameAvailable) {
                videoTexture.updateTexImage();
                videoTexture.getTransformMatrix(videoTextureTransform);
                frameAvailable = false;
            } else {
                return false;
            }

        }

        if (adjustViewport)
            adjustViewport();

        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Draw texture
        GLES20.glUseProgram(shaderProgram);
        int textureParamHandle = GLES20.glGetUniformLocation(shaderProgram, "textura");
        int textureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinate");
        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int textureTranformHandle = GLES20.glGetUniformLocation(shaderProgram, "textureTransform");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 4 * 3, vertexBuffer);


        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(textureParamHandle, 0);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 4, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, videoTextureTransform, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);

        return true;
    }

    private void adjustViewport() {
        float surfaceAspect = height / (float) width;
        float videoAspect = videoHeight / (float) videoWidth;

        if (surfaceAspect > videoAspect) {
            float heightRatio = height / (float) videoHeight;
            int newWidth = (int) (width * heightRatio);
            int xOffset = (newWidth - width) / 2;
            GLES20.glViewport(-xOffset, 0, newWidth, height);
        } else {
            float widthRatio = width / (float) videoWidth;
            int newHeight = (int) (height * widthRatio);
            int yOffset = (newHeight - height) / 2;
            GLES20.glViewport(0, -yOffset, width, newHeight);
        }

        adjustViewport = false;
    }

    @Override
    protected void initGLComponents() {
        setupVertexBuffer();
        setupTexture(ctx);
        loadShaders();
    }

    @Override
    protected void deinitGLComponents() {
        GLES20.glDeleteTextures(1, textures, 0);
        GLES20.glDeleteProgram(shaderProgram);
        videoTexture.release();
        videoTexture.setOnFrameAvailableListener(null);
    }

    public void setVideoSize(int width, int height) {
        this.videoWidth = width;
        this.videoHeight = height;
        adjustViewport = true;
    }

    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }
    }

    public SurfaceTexture getVideoTexture() {
        return videoTexture;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            frameAvailable = true;
        }
    }
}
