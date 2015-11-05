package com.dji.sdkdemo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;

import static java.lang.Math.atan;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.StrictMath.toDegrees;

/**
 * Created by leegross on 9/14/15.
 */
class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;
    private final float TOUCH_SCALE_FACTOR = .011f;//180f/320;
    private float mPreviousX;
    private float mPreviousY;
    public float radius = 7;
    float startX;
    float startY;
    private DroneWrapper mDroneWrapper;
    private double mPreviousGimbalPitch;
    private int minGimbalPitchAngle;
    private int maxGimbalPitchAngle;
    private float mPreviousYaw = -1000;

    public MyGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                float thetaX = (float) toDegrees(atan(dx * TOUCH_SCALE_FACTOR / radius));
                float thetaY = (float) toDegrees(atan(-dy * TOUCH_SCALE_FACTOR / radius));
                thetaY = (float) min(maxGimbalPitchAngle, mDroneWrapper.getCurrentGimbalPitch() - thetaY);
                thetaY = (float) (thetaY - mDroneWrapper.getCurrentGimbalPitch());
                thetaY = (float) max(minGimbalPitchAngle, mDroneWrapper.getCurrentGimbalPitch() - thetaY);
                thetaY = (float) (thetaY - mDroneWrapper.getCurrentGimbalPitch());

                rotateCameraByTheta(thetaX, thetaY);
                break;
            case MotionEvent.ACTION_DOWN:
                startX = x;
                startY = y;
                break;
            case MotionEvent.ACTION_UP:
                float endX = e.getX();
                float endY = e.getY();
                thetaX = (float) Math.toDegrees(atan((endX - startX) * TOUCH_SCALE_FACTOR / radius));
                thetaY = (float) Math.toDegrees(atan((endY - startY) * TOUCH_SCALE_FACTOR / radius));

                setYawAngle(thetaX);
                setGimbalPitch(thetaY);
                startX = 0;
                startY = 0;
                break;
        }

        mPreviousX = x;
        mPreviousY = y;

        return false;
    }

    private void setGimbalPitch(float thetaY){
        double currentGimbalPitch = mDroneWrapper.getCurrentGimbalPitch();
        int newPitchAngle = (int) (currentGimbalPitch + thetaY);
        newPitchAngle = max(minGimbalPitchAngle, newPitchAngle);
        newPitchAngle = min(maxGimbalPitchAngle, newPitchAngle);
        mDroneWrapper.setGimbalPitch(newPitchAngle);
    }

    private void setYawAngle(float thetaX){
        float currentYaw = mDroneWrapper.getCurrentYaw();
        float newYawAngle = currentYaw + thetaX;
        newYawAngle = (newYawAngle + 180)%360 - 180;
        mDroneWrapper.setYawAngle(newYawAngle);
    }

    private void rotateCameraByTheta(float thetaX, float thetaY){
        float[] mRotationMatrixInXDirection = new float[16];
        float[] mRotationMatrixInYDirection = new float[16];
        float[] mRotationMatrix = new float[16];
        float[] newCenter = new float[4];

        float[] upVector = mRenderer.getUpVector();
        float[] sideVector = mRenderer.getSideVector();

        Matrix.setRotateM(mRotationMatrixInXDirection, 0, thetaX, upVector[0], upVector[1], upVector[2]);
        Matrix.setRotateM(mRotationMatrixInYDirection, 0, thetaY, sideVector[0], sideVector[1], sideVector[2]);

        float[] centerVector = mRenderer.getCenterVector();
        Matrix.multiplyMM(mRotationMatrix, 0, mRotationMatrixInXDirection, 0, mRotationMatrixInYDirection, 0);
        Matrix.multiplyMV(newCenter, 0, mRotationMatrix, 0, centerVector, 0);

        mRenderer.setCenter(newCenter);
        requestRender();
    }

    public void setDroneWrapper(DroneWrapper droneWrapper) {
        mDroneWrapper = droneWrapper;
        minGimbalPitchAngle = (int) mDroneWrapper.getGimbalMinPitchAngle();
        maxGimbalPitchAngle = (int) mDroneWrapper.getGimbalMaxPitchAngle();
    }

    public MyGLRenderer getRenderer() {
        return mRenderer;
    }

    // called by drone wrapper when we receive new orientation update
    public void onDroneOrientationUpdate() {
        double currentGimbalPitch = mDroneWrapper.getCurrentGimbalPitch();
        float thetaY = (float) -(currentGimbalPitch - mPreviousGimbalPitch);
        mPreviousGimbalPitch = currentGimbalPitch;

        float currentYaw = mDroneWrapper.getCurrentYaw();
        // if this is the first time, don't rotate rectangle
        if (mPreviousYaw == -1000){
            mPreviousYaw = currentYaw;
        }
        float thetaX = currentYaw - mPreviousYaw;
        rotateRectangleByTheta(thetaX, thetaY);
        mPreviousYaw = currentYaw;
    }

    private void rotateRectangleByTheta(float thetaX, float thetaY) {
        float[] mRotationMatrixInXDirection = new float[16];
        float[] mRotationMatrixInYDirection = new float[16];
        float[] mNewRectangleRotationMatrix = new float[16];

        float[] mRotationMatrix = new float[16];

        float[] upVector = mRenderer.getUpVector();
        float[] sideVector = mRenderer.getSideVector();

        if (upVector == null || sideVector == null) {
            return;
        }

        Matrix.setRotateM(mRotationMatrixInXDirection, 0, thetaX, upVector[0], upVector[1], upVector[2]);
        Matrix.setRotateM(mRotationMatrixInYDirection, 0, thetaY, sideVector[0], sideVector[1], sideVector[2]);

        float[] rectangleRotationMatrix = mRenderer.getRectangleRotationMatrix();
        Matrix.multiplyMM(mRotationMatrix, 0, mRotationMatrixInXDirection, 0, mRotationMatrixInYDirection, 0);
        Matrix.multiplyMM(mNewRectangleRotationMatrix, 0, mRotationMatrix, 0, rectangleRotationMatrix, 0);

        mRenderer.setRectangleRotationMatrix(mNewRectangleRotationMatrix);
        requestRender();
    }
}