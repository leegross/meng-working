package com.dji.sdkdemo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import static com.dji.sdkdemo.Constants.*;
import static java.lang.Math.pow;
import static java.lang.Math.toDegrees;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.sqrt;

/**
 * Created by leegross on 9/14/15.
 */
class MyGLSurfaceView extends GLSurfaceView {

    private final MyGLRenderer mRenderer;
    private DroneWrapper mDroneWrapper;

    float H_CENTER = SURFACE__HORIZONTAL_CENTER;
    float V_CENTER = SURFACE_VERTICAL_CENTER;

    private int minGimbalPitchAngle;
    private int maxGimbalPitchAngle;

    private boolean isGestureInProgress;

    float mPrevDelta = 0;

    float scale = 100.0f;
    private float prevX;
    private float prevY;
    private float gestStartX1;
    private float gestStartY1;
    private float gestStartX2;
    private float gestStartY2;
    private float prevX1;
    private float prevY1;
    private float prevX2;
    private float prevY2;

    private String current_gest = null;
    private int move_count;

    public MyGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);

        scale = 100.0f;
        isGestureInProgress = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        isGestureInProgress = true;
        float x = e.getX();
        float y = e.getY();

        //handle multi touch event
        if (e.getPointerCount() > 1) {
            float p1x = e.getX(0);
            float p1y = e.getY(0);
            float p2x = e.getX(1);
            float p2y = e.getY(1);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    Log.d("myAppTouch", "         two finger down");
                    gestStartX1 = p1x;
                    gestStartY1 = p1y;
                    gestStartX2 = p2x;
                    gestStartY2 = p2y;
                    current_gest = null;
                    move_count = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    // add delay until we decide which gesture the user indicated
                    move_count += 1;
                    if (move_count < 10){
                        break;
                    }

                    // if we don't have a gesture, decide which gesture
                    if (current_gest == null){
                        float rotation_angle = computeRotationAngle(p1x, p1y, p2x, p2y, gestStartX1, gestStartY1, gestStartX2, gestStartY2);
                        String gesture = getGesture(rotation_angle);
                        current_gest = gesture;
                    }

                    // act based on gesture
                    if (current_gest.equals("Zoom")) {
                        float delta = (float) sqrt(pow(p1x - p2x, 2) + pow(p1y - p2y, 2));
                        float zoom_scale = (delta - mPrevDelta) / scale;
                        float midx = (p1x + p2x) / 2.0f;
                        float midy = (p1y + p2y) / 2.0f;
                        mRenderer.moveBasedOnCameraZoom(zoom_scale, midx, midy);
                        current_gest = "Zoom";
                    } else if (current_gest.equals("Two Finger Rotation")){
                        float rotation_angle = computeRotationAngle(p1x, p1y, p2x, p2y, prevX1, prevY1, prevX2, prevY2);
                        float[] rotationPt = computeRotationPoint(p1x, p1y, p2x, p2y);
                        float rx = rotationPt[0];
                        float ry = rotationPt[1];
                        mRenderer.moveBasedOnTwoFingerRotation(rx, ry,rotation_angle);
                        current_gest = "Two Finger Rotation";
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    Log.d("myAppTouch", "         two finger up");
                    float[] cameraTranslationV = mRenderer.getCameraTranslation();
                    float x_translate = cameraTranslationV[0];
                    float y_translate = cameraTranslationV[1];
                    float z_translate = cameraTranslationV[2];
                    float heading = mRenderer.getPhiCamera();
                    mDroneWrapper.setNewGPSCoordinates(x_translate, y_translate, z_translate, heading);

                    if (e.getActionIndex() == 0){
                        prevX = p2x;
                        prevY = p2y;
                    } else{
                        prevX = p1x;
                        prevY = p1y;
                    }

                    break;
            }

            prevX1 = p1x;
            prevY1 = p1y;
            prevX2 = p2x;
            prevY2 = p2y;
            mPrevDelta = (float) sqrt(pow(p1x - p2x, 2.0f) + pow(p1y - p2y, 2.0f));

        }
        // handle single touch event
        else {
            switch (e.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    mRenderer.updateCameraRotation(prevX, prevY, x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    mDroneWrapper.setYawAngle(mRenderer.getPhiCamera());
                    mDroneWrapper.setGimbalPitch((int) mRenderer.getThetaCamera());
                    isGestureInProgress = false;
                    Log.d("myAppTouch", "         one finger up");

                    break;
            }

            prevX = x;
            prevY = y;
        }

        return true;
    }

    private String getGesture(float rotation_angle){
        if (abs(rotation_angle) < 5) {
            return "Zoom";
        } else {
            return "Two Finger Rotation";
        }

    }

    private float computeRotationAngle(float x1, float y1, float x2, float y2, float prevx1, float prevy1, float prevx2, float prevy2){
        // compute rotation angle
        float p1_move = (float) sqrt(pow(x1- prevx1, 2) + pow(y1- prevy1, 2));
        float p2_move = (float) sqrt(pow(x2- prevx2, 2) + pow(y2- prevy2, 2));

        float rotation_angle;
        if (p1_move < p2_move){
            float fixed_x = (x1+ prevx1)/2.0f;
            float fixed_y = (y1+ prevy1)/2.0f;

            float angle_start = (float) atan2(fixed_y - prevy2, fixed_x - prevx2);
            float angle_end = (float) atan2(fixed_y - y2, fixed_x - x2);
            rotation_angle = (float) toDegrees(angle_end - angle_start);
        } else {
            float fixed_x = (x2 + prevx2)/2.0f;
            float fixed_y = (y2 + prevy2)/2.0f;

            float angle_start = (float) atan2(fixed_y - prevy1, fixed_x - prevx1);
            float angle_end = (float) atan2(fixed_y - y1, fixed_x - x1);
            rotation_angle = (float) toDegrees(angle_end - angle_start);
        }

        if (rotation_angle < -180){
            rotation_angle += 360;
        }

        return rotation_angle;
    }

    private float[] computeRotationPoint(float x1, float y1, float x2, float y2){
        float p1_move = (float) sqrt(pow(x1- gestStartX1, 2) + pow(y1- gestStartY1, 2));
        float p2_move = (float) sqrt(pow(x2- gestStartX2, 2) + pow(y2- gestStartY2, 2));

        float fixed_x;
        float fixed_y;
        if (p1_move < p2_move){
            fixed_x = gestStartX1;
            fixed_y = gestStartY1;
        } else {
            fixed_x = gestStartX2;
            fixed_y = gestStartY2;
        }

        return new float[]{fixed_x, fixed_y};
    }

    // called by drone wrapper when we receive new orientation update
    public void onDroneLocationAndOrientationUpdate() {
        float currentGimbalPitch = mDroneWrapper.getCurrentGimbalPitch();
        float currentYaw = mDroneWrapper.getCurrentYaw();
        float x = mDroneWrapper.getCurrentLongitudeInMeters();
        float y = mDroneWrapper.getCurrentAltitude();
        float z = mDroneWrapper.getCurrentLatitudeInMeters();

        mRenderer.setProjectorRotationAngles(currentGimbalPitch, currentYaw);
        mRenderer.setProjectorTranslationV(x, y, z);

        if (mDroneWrapper.droneUpdatesAreInitialized() && !mRenderer.isCameraPhiInitailized()) {
            mRenderer.setInitialCameraPhi(currentYaw);
        }
        if (mDroneWrapper.droneUpdatesAreInitialized() && !mRenderer.isCameraThetaInitialized()) {
            mRenderer.setInitialCameraTheta(currentGimbalPitch);
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

    public void updateZoomScale(float new_scale) {
        scale = new_scale;
    }

    public boolean isGestureInProgress() {
        return isGestureInProgress;
    }
}