package com.dji.sdkdemo;

import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.Gimbal.DJIGimbalRotation;
import dji.sdk.api.GroundStation.DJIGroundStationFlyingInfo;
import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.interfaces.DJIExecuteResultCallback;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;
import dji.sdk.interfaces.DJIGroundStationFlyingInfoCallBack;
import dji.sdk.interfaces.DJIMcuUpdateStateCallBack;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Created by leegross on 11/3/15.
 */
public class DroneWrapper {

    // current drone parameters
    private float currentAltitude;
    private double currentLatitude;
    private double currentLongitude;
    private double currentGimbalPitch;
    private float currentYaw;

    // drone home points
    private double homeLocationLatitude;
    private double homeLocationLongitude;
    private boolean getHomePointFlag;

    private DJIGroundStationTask mTask;
    private setResultToToastCallback toastCallback;
    private MyGLSurfaceView mGLView;
    private uiCallback mUiCallback;

    public DroneWrapper(MyGLSurfaceView surfaceView) {
        mGLView = surfaceView;

        initMainControllerState();

        mTask = new DJIGroundStationTask();

        initFlyingInfo();

        DJIDrone.getDjiGroundStation().setYawControlMode(DJIGroundStationTypeDef.DJINavigationFlightControlYawControlMode.Navigation_Flight_Control_Yaw_Control_Angle);
    }

    private void initFlyingInfo(){
        DJIDrone.getDjiGroundStation().setGroundStationFlyingInfoCallBack(new DJIGroundStationFlyingInfoCallBack() {

            @Override
            public void onResult(DJIGroundStationFlyingInfo mInfo)
            {
                final StringBuffer sb = new StringBuffer();

                currentAltitude = mInfo.altitude;
                currentLatitude = mInfo.droneLocationLatitude;
                currentLongitude = mInfo.droneLocationLongitude;
                sb.append("Altitude = ").append(currentAltitude).append("\n");
                sb.append("Latitude = ").append(currentLatitude).append("\n");
                sb.append("Longitude = ").append(currentLongitude).append("\n");
                sb.append("Yaw = ").append(mInfo.yaw).append("\n");
                currentAltitude = mInfo.altitude;
                currentYaw = mInfo.yaw;

                mGLView.onDroneOrientationUpdate();

                if (mUiCallback != null) {
                    mUiCallback.UICallback("ground_station_text_view", sb.toString());
                }
            }
        });
    }

    private void initMainControllerState(){
        DJIMcuUpdateStateCallBack mMcuUpdateStateCallBack = new DJIMcuUpdateStateCallBack() {

            @Override
            public void onResult(DJIMainControllerSystemState state) {
                homeLocationLatitude = state.droneLocationLatitude;
                homeLocationLongitude = state.droneLocationLongitude;

                if (homeLocationLatitude != -1 && homeLocationLongitude != -1 && homeLocationLatitude != 0 && homeLocationLongitude != 0) {
                    getHomePointFlag = true;
                } else {
                    getHomePointFlag = false;
                }
            }

        };

        DJIDrone.getDjiMainController().setMcuUpdateStateCallBack(mMcuUpdateStateCallBack);
    }

    private boolean checkGetHomePoint(){
        if(!getHomePointFlag){
            toastCallback.ToastResult("home point is not set");
        }
        return getHomePointFlag;
    }

    public interface setResultToToastCallback {
        void ToastResult(String result);
    }
    public void setToastCallback(setResultToToastCallback cb){
        toastCallback = cb;
    }

    public interface uiCallback {
        void UICallback(String type, String result);
    }
    public void setUICallback(uiCallback cb){
        mUiCallback = cb;
    }

    public void takeOff(){
        DJIDrone.getDjiGroundStation().oneKeyFly(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult groundStationResult) {
                String ResultsString = "take off return code =" + groundStationResult.toString();
                toastCallback.ToastResult(ResultsString);
            }
        });
    }

    public void openGs(final double latitude, final double longitude, final float altitude, final short heading){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "openGS return code =" + result.toString();
                toastCallback.ToastResult(ResultsString);
                addWaypoint(latitude, longitude, altitude, heading);
            }

        });
    }

    public void openGs(){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "openGS return code =" + result.toString();
                toastCallback.ToastResult(ResultsString);
            }

        });
    }

    private void addWaypoint(double latitude, double longitude, float altitude, short heading){
        mTask.RemoveAllWaypoint();
        DJIGroundStationWaypoint mWayPoint1 = new DJIGroundStationWaypoint(latitude, longitude);
        mWayPoint1.action.actionRepeat = 1;
        mWayPoint1.altitude = (currentAltitude+altitude)/2;
        mWayPoint1.heading = 0;
        mWayPoint1.actionTimeout = 10;
        mWayPoint1.turnMode = 1;
        mWayPoint1.dampingDistance = 1.5f;
        mWayPoint1.hasAction = true;

        mWayPoint1.addAction(DJIGroundStationTypeDef.GroundStationOnWayPointAction.Way_Point_Action_Gimbal_Pitch, 0);


        mTask.addWaypoint(mWayPoint1);

        DJIGroundStationWaypoint mWayPoint2 = new DJIGroundStationWaypoint(latitude, longitude);
        mWayPoint2.action.actionRepeat = 1;
        mWayPoint2.altitude = altitude;
        mWayPoint2.heading = 0;
        mWayPoint2.actionTimeout = 10;
        mWayPoint2.turnMode = 1;
        mWayPoint2.dampingDistance = 1.5f;
        mWayPoint2.hasAction = true;

        mWayPoint2.addAction(DJIGroundStationTypeDef.GroundStationOnWayPointAction.Way_Point_Action_Craft_Yaw, heading);


        mTask.addWaypoint(mWayPoint2);

        mTask.finishAction = DJIGroundStationTypeDef.DJIGroundStationFinishAction.None;
        mTask.movingMode = DJIGroundStationTypeDef.DJIGroundStationMovingMode.GSHeadingUsingWaypointHeading;
        mTask.pathMode = DJIGroundStationTypeDef.DJIGroundStationPathMode.Point_To_Point;
        mTask.wayPointCount = mTask.getAllWaypoint().size();

        uploadWaypoint();
    }

    private void uploadWaypoint(){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().uploadGroundStationTask(mTask, new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "upload waypoint return code =" + result.toString();
                toastCallback.ToastResult(ResultsString);
                startTask();
            }
        });

    }

    private void startTask(){
        if(!checkGetHomePoint()) return;
        DJIDrone.getDjiGroundStation().startGroundStationTask(new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "start task return code =" + result.toString();
                toastCallback.ToastResult(ResultsString);
            }
        });
    }

    public void closeGs(){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().closeGroundStation(new DJIGroundStationExecuteCallBack(){

            @Override
            public void onResult(DJIGroundStationTypeDef.GroundStationResult result) {
                String ResultsString = "close gs return code =" + result.toString();
                toastCallback.ToastResult(ResultsString);
            }

        });
    }

    public void setGimbalPitch(int angle){
        // clip gimbal pitch between -90 and 0
        angle = (int) max(getGimbalMinPitchAngle(), angle);
        angle = (int) min(getGimbalMaxPitchAngle(), angle);
        final int finalAngle = angle;
        new Thread()
        {
            public void run()
            {
                DJIGimbalRotation mPitch = new DJIGimbalRotation(true, false,true, finalAngle);

                DJIDrone.getDjiGimbal().updateGimbalAttitude(mPitch,null,null);

            }
        }.start();
    }

    // sets the angle the drone is facing relative to north
    public void setYawAngle(float angle){
        angle = (angle + 180)%360 - 180;
        if (angle < -180){
            angle += 360;
        } else if(angle > 180){
            angle -= 360;
        }
        final float finalAngle = angle;
        openGs();
        new Thread()
        {
            public void run()
            {
                DJIDrone.getDjiGroundStation().sendFlightControlData(finalAngle, 0, 0, 0, new DJIExecuteResultCallback() {
                    @Override
                    public void onResult(DJIError djiError) {}
                });

            }
        }.start();
    }

    public void resume(){

        DJIDrone.getDjiMainController().startUpdateTimer(100);
        DJIDrone.getDjiGroundStation().startUpdateTimer(1000);
    }

    public void pause(){

        DJIDrone.getDjiMainController().stopUpdateTimer();
        DJIDrone.getDjiGroundStation().stopUpdateTimer();
    }

    public float getCurrentAltitude(){
        return currentAltitude;
    }

    public double getCurrentLatitude(){
        return currentLatitude;
    }

    public double getCurrentLongitude(){
        return currentLongitude;
    }

    public double getHomeLocationLatitude(){
        return homeLocationLatitude;
    }

    public double getHomeLocationLongitude(){
        return homeLocationLongitude;
    }

    public double getCurrentGimbalPitch(){
        return currentGimbalPitch;
    }

    public double getGimbalMinPitchAngle(){
        return -90;//DJIDrone.getDjiGimbal().getGimbalPitchMinAngle();
    }

    public double getGimbalMaxPitchAngle(){
        return 0;//DJIDrone.getDjiGimbal().getGimbalPitchMaxAngle();
    }

    public void setCurrentGimbalPitch(double pitch){
        currentGimbalPitch = pitch;
        mGLView.onDroneOrientationUpdate();
    }

    public float getCurrentYaw(){
        return currentYaw;
    }
}
