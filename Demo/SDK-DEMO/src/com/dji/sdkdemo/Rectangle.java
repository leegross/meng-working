package com.dji.sdkdemo;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.dji.sdkdemo.util.RawResourceReader;
import com.dji.sdkdemo.util.TextureHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by leegross on 9/14/15.
 */
public class Rectangle {

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private final int mProgram;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float rect_z = 7f;
    static float rect_x = (float) (rect_z * Math.tan(StrictMath.toRadians(Constants.HORIZONTAL_FOV/2)));
    static float rect_y = rect_x / Constants.ASPECT_RATIO;
    static float rectangleCoords[] = {
            -rect_x,  rect_y, rect_z,   // top left
            -rect_x, -rect_y, rect_z,   // bottom left
            rect_x, -rect_y, rect_z,   // bottom right
            rect_x,  rect_y, rect_z }; // top right

    private FloatBuffer uvVertexBuffer;
    static final int UV_COORDS_PER_VERTEX = 2;
    static float rectangleUVCoords[] = {
            1f, 0f,
            1f, 1f,
            0f, 1f,
            0f, 0f};

    private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    private Context mContext;

    /** This is a handle to our texture data. */
    private int mTextureDataHandle;

    public Rectangle(Context context) {
        Log.d("myApp", "rectangle initialized");
        mContext = context;

        mTextureDataHandle = TextureHelper.loadTexture(mContext, R.raw.sample_img);


        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                getVertexShader());
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                getFragmentShader());

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram();

        // add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);

        // add the fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);

        // creates OpenGL ES program executables
        GLES20.glLinkProgram(mProgram);

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                rectangleCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(rectangleCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer uvcoords_buffer = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                rectangleCoords.length * 4);
        uvcoords_buffer.order(ByteOrder.nativeOrder());
        uvVertexBuffer = uvcoords_buffer.asFloatBuffer();
        uvVertexBuffer.put(rectangleUVCoords);
        uvVertexBuffer.position(0);
    }

    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);



        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");

        // Enable a handle to the rectangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the rectangle coordinate data
        int vertexStride = COORDS_PER_VERTEX * 4;
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        //-----
        // get handle to vertex shader's vPosition member
        int mTextCoorHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");

        // Enable a handle to the rectangle vertices
        GLES20.glEnableVertexAttribArray(mTextCoorHandle);

        // Prepare the rectangle coordinate data
        int uvVertexStride = UV_COORDS_PER_VERTEX * 4;
        GLES20.glVertexAttribPointer(mTextCoorHandle, UV_COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                uvVertexStride, uvVertexBuffer);

        /* This will be used to pass in the texture. */
        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // Bind the texture to this unit.
         GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureDataHandle);
        //------

        // get handle to shape's transformation matrix
        int mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the square
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    protected String getVertexShader()
    {
        return RawResourceReader.readTextFileFromRawResource(mContext, R.raw.per_pixel_vertex_shader);
    }

    protected String getFragmentShader()
    {
        return RawResourceReader.readTextFileFromRawResource(mContext, R.raw.per_pixel_fragment_shader);
    }

    public int getTextureHandle() {
        return mTextureDataHandle;
    }
}