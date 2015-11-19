package com.dji.sdkdemo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import static java.lang.Math.atan2;
import static java.lang.StrictMath.tan;
import static java.lang.StrictMath.toDegrees;
import static java.lang.StrictMath.toRadians;

/**
 * Created by leegross on 9/14/15.
 */
class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;
    private final float TOUCH_SCALE_FACTOR = (float) (tan(toRadians(Constants.HORIZONTAL_FOV/2)) * Constants.FRUST_NEAR /Constants.SURFACE__HORIZONTAL_CENTER);//.011f;//180f/320;
    private float mPreviousX;
    private float mPreviousY;
    private DroneWrapper mDroneWrapper;
    private double mPreviousGimbalPitch;
    private int minGimbalPitchAngle;
    private int maxGimbalPitchAngle;
    private float mPreviousYaw;
    private final float NOT_INITIALIZED = -1000.0f;
    float accumulatedThetaY = 0;
    float accumulatedCameraY = 0;
    float H_CENTER = Constants.SURFACE__HORIZONTAL_CENTER;
    float V_CENTER = Constants.SURFACE_VERTICAL_CENTER;

    public MyGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        mPreviousYaw = NOT_INITIALIZED;
        mPreviousGimbalPitch = NOT_INITIALIZED;
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

                float thetaX1 = (float) atan2((x - H_CENTER) * TOUCH_SCALE_FACTOR, Constants.FRUST_NEAR);
                float thetaX2 = (float) atan2((H_CENTER - mPreviousX) * TOUCH_SCALE_FACTOR, Constants.FRUST_NEAR);
                float thetaX = (float) toDegrees(thetaX1 + thetaX2);

                float thetaY1 = (float) atan2((V_CENTER - y) * TOUCH_SCALE_FACTOR, Constants.FRUST_NEAR);
                float thetaY2 = (float) atan2((mPreviousY - V_CENTER) * TOUCH_SCALE_FACTOR, Constants.FRUST_NEAR);
                float thetaY = (float) toDegrees(thetaY1 + thetaY2);

                thetaY = clipPitchAngle(thetaY);

                mRenderer.updateCameraRotationAngles(thetaY, thetaX);
                break;
            case MotionEvent.ACTION_UP:

                mDroneWrapper.setYawAngle(mRenderer.getPhiCamera());
                mDroneWrapper.setGimbalPitch((int) mRenderer.getThetaCamera());

                accumulatedThetaY = 0;
                break;
        }

        mPreviousX = x;
        mPreviousY = y;

        return true;
    }

    private float clipPitchAngle(float thetaY){
        float currentGimbalPitch = (float) mDroneWrapper.getCurrentGimbalPitch();
        // Y is limited to [-90, 0] so don't move past those
        if (currentGimbalPitch + accumulatedThetaY > maxGimbalPitchAngle &&
                currentGimbalPitch + accumulatedThetaY - thetaY > maxGimbalPitchAngle) {
            thetaY = (float) 0;
        } else if (currentGimbalPitch + accumulatedThetaY < minGimbalPitchAngle &&
                currentGimbalPitch + accumulatedThetaY - thetaY < minGimbalPitchAngle) {
            thetaY = (float) 0;
        } else if (currentGimbalPitch + accumulatedThetaY - thetaY > maxGimbalPitchAngle) {
            thetaY = currentGimbalPitch + accumulatedThetaY - maxGimbalPitchAngle;
        } else if (currentGimbalPitch + accumulatedThetaY - thetaY < minGimbalPitchAngle) {
            thetaY = currentGimbalPitch + accumulatedThetaY - minGimbalPitchAngle;
        }

        accumulatedThetaY -= thetaY;
        return thetaY;
    }

    // called by drone wrapper when we receive new orientation update
    public void onDroneOrientationUpdate() {
        float currentGimbalPitch = (float) mDroneWrapper.getCurrentGimbalPitch();
        float currentYaw = mDroneWrapper.getCurrentYaw();

        mRenderer.setProjectorRotationAngles(currentGimbalPitch, currentYaw);
    }

    public void setDroneWrapper(DroneWrapper droneWrapper) {
        mDroneWrapper = droneWrapper;
        minGimbalPitchAngle = (int) mDroneWrapper.getGimbalMinPitchAngle();
        maxGimbalPitchAngle = (int) mDroneWrapper.getGimbalMaxPitchAngle();
    }

    public MyGLRenderer getRenderer() {
        return mRenderer;
    }
}