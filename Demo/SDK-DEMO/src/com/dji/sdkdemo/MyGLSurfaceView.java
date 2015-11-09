package com.dji.sdkdemo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;

import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.atan2;
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
    private float mPreviousYaw;
    private final float NOT_INITIALIZED = -1000.0f;
    private float[] mRectangleCenter;
    float accumulatedThetaY = 0;
    float accumulatedCameraY = 0;

    public MyGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        mPreviousYaw = NOT_INITIALIZED;
        mPreviousGimbalPitch = NOT_INITIALIZED;
        mRectangleCenter = new float[]{0, 0, 7, 0};
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

                float dx = (x - mPreviousX) *  TOUCH_SCALE_FACTOR;
                float dy = y - mPreviousY;
//                float temp = abs(mPreviousX - Constants.TABLET_CENTER_X) * TOUCH_SCALE_FACTOR/radius;
//                float theta = (float) acos(temp);
//                float delta = (float) (dx/cos(Math.PI/2-theta));

//                float thetaX = (float) toDegrees(atan(delta / radius));
                float thetaX = (float) toDegrees(atan(dx/radius));

                float thetaY = (float) toDegrees(atan(-dy * TOUCH_SCALE_FACTOR/ radius));

                thetaY = clipPitchAngle(thetaY);

                rotateCameraByTheta(0, thetaY);
                accumulatedThetaY -= thetaY;
                break;
            case MotionEvent.ACTION_UP:

                // compute yaw angle
                float[] currentCenterVector = mRenderer.getCenterVector();
                float theta1x = (float) atan2(mRectangleCenter[2], mRectangleCenter[0]);
                float theta2x = (float) atan2(currentCenterVector[2], currentCenterVector[0]);
                thetaX = (float) toDegrees(theta1x - theta2x);
//                setYawAngle(thetaX);

                // compute pitch angle
                float theta1y = (float) atan2(mRectangleCenter[2], mRectangleCenter[1]);
                float theta2y = (float) atan2(currentCenterVector[2], currentCenterVector[1]);
                thetaY = (float) toDegrees(theta1y - theta2y);
                if (abs(thetaY) > 90) {
                    thetaY = 0;
                }
                setGimbalPitch(thetaY);

                accumulatedThetaY = 0;

                break;
        }

        mPreviousX = x;
        mPreviousY = y;

        return true;
    }

    private float clipPitchAngle(float thetaY){
        // Y is limited to [-90, 0] so don't move past those
        if (mDroneWrapper.getCurrentGimbalPitch() + accumulatedThetaY > maxGimbalPitchAngle &&
                mDroneWrapper.getCurrentGimbalPitch() + accumulatedThetaY - thetaY > maxGimbalPitchAngle) {
            thetaY = (float) 0;
        } else if (mDroneWrapper.getCurrentGimbalPitch() + accumulatedThetaY < minGimbalPitchAngle &&
                mDroneWrapper.getCurrentGimbalPitch() + accumulatedThetaY - thetaY < minGimbalPitchAngle) {
            thetaY = (float) 0;
        } else if (mDroneWrapper.getCurrentGimbalPitch() + accumulatedThetaY - thetaY > maxGimbalPitchAngle) {
            thetaY = (float) (mDroneWrapper.getCurrentGimbalPitch() - maxGimbalPitchAngle);
        } else if (mDroneWrapper.getCurrentGimbalPitch() + accumulatedThetaY - thetaY < minGimbalPitchAngle) {
            thetaY = (float) (mDroneWrapper.getCurrentGimbalPitch() - minGimbalPitchAngle);
        }
        return thetaY;
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
        float newYawAngle = currentYaw - thetaX;
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

        correctForFlippingBug(thetaY);

        Matrix.setRotateM(mRotationMatrixInXDirection, 0, thetaX, upVector[0], upVector[1], upVector[2]);
        Matrix.setRotateM(mRotationMatrixInYDirection, 0, thetaY, sideVector[0], sideVector[1], sideVector[2]);

        float[] centerVector = mRenderer.getCenterVector();
        Matrix.multiplyMM(mRotationMatrix, 0, mRotationMatrixInXDirection, 0, mRotationMatrixInYDirection, 0);
        Matrix.multiplyMV(newCenter, 0, mRotationMatrix, 0, centerVector, 0);


//        // update up and side vectors
//        float[] newUpVector = new float[4];
//        float[] newSideVector = new float[4];
//        Matrix.multiplyMV(newUpVector, 0, mRotationMatrixInXDirection, 0, upVector, 0);
//        Matrix.multiplyMV(newSideVector, 0, mRotationMatrixInYDirection, 0, sideVector, 0);
//
//        mRenderer.setSideVector(newSideVector);
//        mRenderer.setUpVector(newUpVector);

        mRenderer.setCenter(newCenter);
        requestRender();
    }

    // when the camera rotates past 90 degrees, up vector flips for some reason (couldn't figure it out)
    // so we want to flip it back whenever we pass 90 degrees.
    private void correctForFlippingBug(float thetaY){
        float[] upVector = mRenderer.getUpVector();

        accumulatedCameraY += thetaY;
        if (accumulatedCameraY > 90) {
            float[] newUpVector = {0, -1, 0, 0};
            mRenderer.setUpVector(newUpVector);
        }
        if (upVector[0] == 0 && upVector[1] == -1 && upVector[2] == 0 && upVector[3] == 0 && accumulatedCameraY < 90) {
            float[] newUpVector = {0, 1, 0, 0};
            mRenderer.setUpVector(newUpVector);
        }
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
        float currentYaw = mDroneWrapper.getCurrentYaw();

        if (mPreviousYaw == NOT_INITIALIZED &&
            mDroneWrapper.getCurrentLatitude() != 0 &&
            mDroneWrapper.getCurrentLongitude() != 0
        ){
            mPreviousYaw = currentYaw;
        }

        if (mPreviousGimbalPitch == NOT_INITIALIZED &&
            mDroneWrapper.getCurrentLatitude() != 0 &&
            mDroneWrapper.getCurrentLongitude() != 0
        ){
            mPreviousGimbalPitch = currentGimbalPitch;
        }

        float thetaX = mPreviousYaw - currentYaw;
        float thetaY = (float) -(currentGimbalPitch - mPreviousGimbalPitch);
        rotateRectangleByTheta(0, thetaY);

        // update previous angles
        if (mPreviousYaw != NOT_INITIALIZED) {
            mPreviousYaw = currentYaw;
        }
        if (mPreviousGimbalPitch != NOT_INITIALIZED) {
            mPreviousGimbalPitch = currentGimbalPitch;
        }
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

        float[] tempVector = new float[4];
        Matrix.multiplyMV(tempVector, 0, mRotationMatrix, 0, mRectangleCenter, 0);
        mRectangleCenter = tempVector;

        mRenderer.setRectangleRotationMatrix(mNewRectangleRotationMatrix);
        requestRender();
    }
}