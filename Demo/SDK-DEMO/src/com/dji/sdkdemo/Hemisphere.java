package com.dji.sdkdemo;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.dji.sdkdemo.util.RawResourceReader;
import com.dji.sdkdemo.util.ShaderHelper;
import com.dji.sdkdemo.util.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static java.lang.Math.sin;
import static java.lang.StrictMath.cos;

/**
 * Created by leegross on 11/9/15.
 */
public class Hemisphere {

    private final int mTextureDataHandle;
    /** OpenGL handles to our program uniforms. */
    private int mvpMatrixUniform;
    private int mvpMatrixProjectorUniform;

    /** OpenGL handles to our program attributes. */
    private int positionAttribute;
    private int normalAttribute;
    private int colorAttribute;
    private int textureAttribute;

    /** Identifiers for our uniforms and attributes inside the shaders. */
    private static final String MVP_MATRIX_UNIFORM = "u_MVPMatrix";
    private static final String MVP_PROJECTOR_MATRIX_UNIFORM = "u_MVPMatrix_projector";

    private static final String POSITION_ATTRIBUTE = "a_Position";
    private static final String NORMAL_ATTRIBUTE = "a_Normal";
    private static final String COLOR_ATTRIBUTE = "a_Color";
    private static final String TEXTURE_ATTRIBUTE = "a_Texture";

    /** This is a handle to our cube shading program. */
    private int program;

    /** Additional constants. */
    private static final int POSITION_DATA_SIZE_IN_ELEMENTS = 3;
    private static final int NORMAL_DATA_SIZE_IN_ELEMENTS = 3;
    private static final int COLOR_DATA_SIZE_IN_ELEMENTS = 4;
    private static final int TEXTURE_DATA_SIZE_IN_ELEMENTS = 2;

    static final int SIZE_PER_SIDE = 20;
    final int yLength = SIZE_PER_SIDE;
    final int xLength = SIZE_PER_SIDE;

    private static final int BYTES_PER_FLOAT = 4;
    private static final int BYTES_PER_SHORT = 2;

    private static final int STRIDE = (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURE_DATA_SIZE_IN_ELEMENTS)
            * BYTES_PER_FLOAT;

    final int[] vbo = new int[1];
    final int[] ibo = new int[1];

    float radius = Constants.SPHERE_RADIUS;

    int indexCount;

    Hemisphere(Context context) {
        mTextureDataHandle = TextureHelper.loadTexture(context, R.raw.colorful_grid);

        try {

            float[] heightMapVertexData = createVertexData();

            short[] heightMapIndexData = createIndexData();

            indexCount = heightMapIndexData.length;

            final FloatBuffer heightMapVertexDataBuffer = ByteBuffer
                    .allocateDirect(heightMapVertexData.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            heightMapVertexDataBuffer.put(heightMapVertexData).position(0);

            final ShortBuffer heightMapIndexDataBuffer = ByteBuffer
                    .allocateDirect(heightMapIndexData.length * BYTES_PER_SHORT).order(ByteOrder.nativeOrder())
                    .asShortBuffer();
            heightMapIndexDataBuffer.put(heightMapIndexData).position(0);

            GLES20.glGenBuffers(1, vbo, 0);
            GLES20.glGenBuffers(1, ibo, 0);

            if (vbo[0] > 0 && ibo[0] > 0) {
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, heightMapVertexDataBuffer.capacity() * BYTES_PER_FLOAT,
                        heightMapVertexDataBuffer, GLES20.GL_STATIC_DRAW);

                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0]);
                GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, heightMapIndexDataBuffer.capacity()
                        * BYTES_PER_SHORT, heightMapIndexDataBuffer, GLES20.GL_STATIC_DRAW);

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
                GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
            } else {
                Log.e("hemisphere", "glBuffers error");
            }
        } catch (Throwable t) {
            Log.w("heightMapAPP", t);
        }

        setupShaders(context);
    }

    private short[] createIndexData() {
        // Now build the index data
        final int numHemisphereVertices = (yLength-1) * (2 * xLength * 3);
        final int numCircleVertices = 3 * 2 * (xLength);

        final short[] heightMapIndexData = new short[numHemisphereVertices + numCircleVertices];

        int offset = 0;

        // add semi circle vertices to index array
        for (int y = 0; y < yLength-1; y++) {
            for (int x = 0; x < xLength; x++) {
                heightMapIndexData[offset++] = (short) ((y + 1) * yLength + x);
                heightMapIndexData[offset++] = (short) (y * yLength + x);
                heightMapIndexData[offset++] = (short) (y * yLength + x + 1);
            }
            for (int x = 0; x < xLength; x++) {
                heightMapIndexData[offset++] = (short) (y * yLength + x + 1);
                heightMapIndexData[offset++] = (short) ((y + 1) * yLength + x + 1);
                heightMapIndexData[offset++] = (short) ((y + 1) * yLength + x);
            }
        }

//        add circle vertices to index array
        for (int x = 0; x < xLength-1; x++) {
            heightMapIndexData[offset++] = (short) xLength * yLength;
            heightMapIndexData[offset++] = (short) (x + 1 + (xLength-1) * yLength );
            heightMapIndexData[offset++] = (short) (x + (xLength-1) * yLength);

            heightMapIndexData[offset++] = (short) xLength * yLength;
            heightMapIndexData[offset++] = (short) (x + 1);
            heightMapIndexData[offset++] = (short) (x);
        }
        return heightMapIndexData;
    }

    public float[] createVertexData() {
        final int floatsPerVertex = POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS
                + COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURE_DATA_SIZE_IN_ELEMENTS;
        int sphere_data_length = xLength * yLength * floatsPerVertex;
        int circle_data_length = floatsPerVertex; //add origin

        final float[] heightMapVertexData = new float[sphere_data_length + circle_data_length];

        int offset = 0;

        // First, build sphere the data for the vertex buffer
        for (int y = 0; y < yLength; y++) {
            for (int x = 0; x < xLength; x++) {
                final float xRatio = x / (float) (xLength-1);

                // Build our heightmap from the top down, so that our triangles are counter-clockwise.
                final float yRatio = y / (float) (yLength*2-2);

                // Position
                heightMapVertexData[offset++] = radius * (float) (sin(Math.PI * xRatio) * cos(2 * Math.PI * yRatio));
                heightMapVertexData[offset++] = radius * (float) (sin(Math.PI * xRatio) * sin(2 * Math.PI * yRatio));
                heightMapVertexData[offset++] = radius * (float) cos(Math.PI * xRatio);

                // normal vector
                heightMapVertexData[offset++] = (float) -(sin(Math.PI * xRatio) * cos(2 * Math.PI * yRatio));
                heightMapVertexData[offset++] = (float) -(sin(Math.PI * xRatio) * sin(2 * Math.PI * yRatio));
                heightMapVertexData[offset++] = (float) -cos(Math.PI * xRatio);

                // Add some fancy colors.
                heightMapVertexData[offset++] = xRatio;
                heightMapVertexData[offset++] = yRatio;
                heightMapVertexData[offset++] = 1f;
                heightMapVertexData[offset++] = 1f;

                // add texture coordinates
                heightMapVertexData[offset++] = 1f;
                heightMapVertexData[offset++] = 1f;
            }
        }

        // add vertex for origin of circle
        heightMapVertexData[offset++] = 0;
        heightMapVertexData[offset++] = 0f;
        heightMapVertexData[offset++] = 0;

        // normal vector
        heightMapVertexData[offset++] = 0;
        heightMapVertexData[offset++] = 0;
        heightMapVertexData[offset++] = 1;

        // Add some fancy colors.
        heightMapVertexData[offset++] = 1f;
        heightMapVertexData[offset++] = 0f;
        heightMapVertexData[offset++] = 1f;
        heightMapVertexData[offset++] = 1f;

        // add texture coordinates
        heightMapVertexData[offset++] = 0f;
        heightMapVertexData[offset++] = 0f;

        return heightMapVertexData;
    }

    private void setupShaders(Context context) {
        final String vertexShader = RawResourceReader.readTextFileFromRawResource(context,
                R.raw.per_pixel_vertex_shader);
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(context,
                R.raw.per_pixel_fragment_shader);

        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        program = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[] {
                POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURE_ATTRIBUTE });
    }

    void render(float[] mvpMatrix, float[] mvpProjectorMatrix) {

        setProgramHandles(mvpMatrix, mvpProjectorMatrix);

        if (vbo[0] > 0 && ibo[0] > 0) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);

            bindAttributes();

            // Draw
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0]);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
    }

    private void setProgramHandles(float[] mvpMatrix, float[] mvpProjectorMatrix) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set our per-vertex program.
        GLES20.glUseProgram(program);

        // Set program handles for cube drawing.
        mvpMatrixUniform = GLES20.glGetUniformLocation(program, MVP_MATRIX_UNIFORM);
        mvpMatrixProjectorUniform = GLES20.glGetUniformLocation(program, MVP_PROJECTOR_MATRIX_UNIFORM);

        positionAttribute = GLES20.glGetAttribLocation(program, POSITION_ATTRIBUTE);
        normalAttribute = GLES20.glGetAttribLocation(program, NORMAL_ATTRIBUTE);
        colorAttribute = GLES20.glGetAttribLocation(program, COLOR_ATTRIBUTE);
        textureAttribute = GLES20.glGetAttribLocation(program, TEXTURE_ATTRIBUTE);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixProjectorUniform, 1, false, mvpProjectorMatrix, 0);
    }

    private void bindAttributes(){
        // Bind Attributes
        GLES20.glVertexAttribPointer(positionAttribute, POSITION_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                STRIDE, 0);
        GLES20.glEnableVertexAttribArray(positionAttribute);

        GLES20.glVertexAttribPointer(normalAttribute, NORMAL_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                STRIDE, POSITION_DATA_SIZE_IN_ELEMENTS * BYTES_PER_FLOAT);
        GLES20.glEnableVertexAttribArray(normalAttribute);

        GLES20.glVertexAttribPointer(colorAttribute, COLOR_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                STRIDE, (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
        GLES20.glEnableVertexAttribArray(colorAttribute);

        GLES20.glVertexAttribPointer(textureAttribute, TEXTURE_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
                STRIDE, (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
        GLES20.glEnableVertexAttribArray(textureAttribute);

        /* This will be used to pass in the texture. */
        int mTextureUniformHandle = GLES20.glGetUniformLocation(program, "v_Texture");

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
        if (Constants.USE_CAMERA_STREAM){
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureDataHandle);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
        }
    }

    public int getTextureHandle() {
        return mTextureDataHandle;
    }
}

