package com.dji.sdkdemo;

import android.util.Log;

import dji.sdk.api.Camera.DJICameraSettingsTypeDef;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIError;
import dji.sdk.api.Gimbal.DJIGimbalAttitude;
import dji.sdk.api.Gimbal.DJIGimbalRotation;
import dji.sdk.api.GroundStation.DJIGroundStationFlyingInfo;
import dji.sdk.api.GroundStation.DJIGroundStationTask;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef;
import dji.sdk.api.GroundStation.DJIGroundStationWaypoint;
import dji.sdk.api.GroundStation.DJIGroundStationTypeDef;
import dji.sdk.api.MainController.DJIMainControllerSystemState;
import dji.sdk.interfaces.DJIExecuteResultCallback;
import dji.sdk.interfaces.DJIGimbalUpdateAttitudeCallBack;
import dji.sdk.interfaces.DJIGroundStationExecuteCallBack;
import dji.sdk.interfaces.DJIGroundStationFlyingInfoCallBack;
import dji.sdk.interfaces.DJIMcuUpdateStateCallBack;

import static com.dji.sdkdemo.Constants.*;
import static dji.sdk.api.GroundStation.DJIGroundStationTypeDef.*;
import static dji.sdk.api.GroundStation.DJIGroundStationTypeDef.DJINavigationFlightControlYawControlMode.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.tan;
import static java.lang.Math.toDegrees;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.sqrt;
import static java.lang.StrictMath.toRadians;

/**
 * Created by leegross on 11/3/15.
 */
// handles most of the DJI controls
public class DroneWrapper {

    // current drone parameters
    private float currentAltitude;
    private double currentLatitude;
    private double currentLongitude;
    private float currentGimbalPitch;
    private float currentYaw;
    private String currentFlightMode;

    // drone home points
    private double homeLocationLatitude;
    private double homeLocationLongitude;
    private boolean getHomePointFlag;

    private DJIGroundStationTask mTask;
    private setResultToToastCallback toastCallback;
    private MyGLSurfaceView mGLView;

    public DroneWrapper(MyGLSurfaceView surfaceView) {
        mGLView = surfaceView;

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        initMainControllerState();

        mTask = new DJIGroundStationTask();

        initFlyingInfo();

        initGimbalInfo();

        DJIDrone.getDjiGroundStation().setYawControlMode(Navigation_Flight_Control_Yaw_Control_Angle);

        //DJIDrone.getDjiCamera().setCameraMode(DJICameraSettingsTypeDef.CameraMode.Camera_Camera_Mode, null); //need to test this line
    }

    private void initFlyingInfo(){

        DJIDrone.getDjiGroundStation().setGroundStationFlyingInfoCallBack(new DJIGroundStationFlyingInfoCallBack() {

            @Override
            public void onResult(DJIGroundStationFlyingInfo mInfo) {
                currentAltitude = mInfo.altitude;
                currentLatitude = (float) mInfo.droneLocationLatitude;
                currentLongitude = (float) mInfo.droneLocationLongitude;
                currentYaw = mInfo.yaw;
                currentFlightMode = mInfo.flightMode.name();

                mGLView.onDroneLocationAndOrientationUpdate();
            }
        });
    }

    private void initGimbalInfo(){
        DJIGimbalUpdateAttitudeCallBack mGimbalUpdateAttitudeCallBack = new DJIGimbalUpdateAttitudeCallBack() {

            //DroneWrapper dw = droneWrapper;

            @Override
            public void onResult(final DJIGimbalAttitude attitude) {
                currentGimbalPitch = (float) attitude.pitch;
                mGLView.onDroneLocationAndOrientationUpdate();
            }

        };

        DJIDrone.getDjiGimbal().setGimbalUpdateAttitudeCallBack(mGimbalUpdateAttitudeCallBack);
    }

    private void initMainControllerState(){
        DJIMcuUpdateStateCallBack mMcuUpdateStateCallBack = new DJIMcuUpdateStateCallBack() {

            @Override
            public void onResult(DJIMainControllerSystemState state) {
                homeLocationLatitude = (float) state.homeLocationLatitude;
                homeLocationLongitude = (float) state.homeLocationLongitude;

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

    public boolean isExecutingMission() {
        if (//currentFlightMode != GroundStationFlightMode.GS_Mode_Joystick &&
            currentFlightMode != GroundStationFlightMode.GS_Mode_Waypoint.name()) {
            return false;
        }
        return true;
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

    public void takeOff(){
        DJIDrone.getDjiGroundStation().oneKeyFly(new DJIGroundStationExecuteCallBack() {
            @Override
            public void onResult(GroundStationResult groundStationResult) {
                String ResultsString = "take off return code =" + groundStationResult.toString();
//                toastCallback.ToastResult(ResultsString);
            }
        });
    }

    public void openGs(final double latitude, final double longitude, final float altitude, final short heading){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(GroundStationResult result) {
                String ResultsString = "openGS return code =" + result.toString();
//                toastCallback.ToastResult(ResultsString);
                addWaypoint(latitude, longitude, altitude, heading);
            }

        });
    }

    public void openGs(){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().openGroundStation(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(GroundStationResult result) {
                String ResultsString = "openGS return code =" + result.toString();
                toastCallback.ToastResult(ResultsString);
            }

        });
    }

    private void addWaypoint(double latitude, double longitude, float altitude, short heading){
        mTask.RemoveAllWaypoint();

        DJIGroundStationWaypoint mWayPoint1 = createWaypoint(currentLatitude, currentLongitude, currentAltitude, heading);
        mTask.addWaypoint(mWayPoint1);


        DJIGroundStationWaypoint mWayPoint2 = createWaypoint(latitude, longitude, altitude, heading);
        mTask.addWaypoint(mWayPoint2);

        setWaypointTaskSettings();

        uploadWaypoint();
    }

    private DJIGroundStationWaypoint createWaypoint(double latitude, double longitude, float altitude, float heading) {
        DJIGroundStationWaypoint waypoint = new DJIGroundStationWaypoint(latitude, longitude);
        waypoint.action.actionRepeat = 1;
        waypoint.altitude = altitude;
        waypoint.heading = (short) heading;
        waypoint.actionTimeout = 10;
        waypoint.turnMode = 1;
        waypoint.dampingDistance = 1.5f;
        waypoint.hasAction = false;

//        waypoint.addAction(DJIGroundStationTypeDef.GroundStationOnWayPointAction.Way_Point_Action_Craft_Yaw, (int) heading);

        return waypoint;
    }

    private void setWaypointTaskSettings(){
        mTask.finishAction = DJIGroundStationFinishAction.None;
//        mTask.movingMode = DJIGroundStationMovingMode.GSHeadingUsingInitialDirection;
        mTask.movingMode = DJIGroundStationMovingMode.GSHeadingUsingWaypointHeading;
        mTask.pathMode = DJIGroundStationPathMode.Point_To_Point;
        mTask.wayPointCount = mTask.getAllWaypoint().size();
    }

    private void uploadWaypoint(){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().uploadGroundStationTask(mTask, new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(GroundStationResult result) {
                String ResultsString = "upload waypoint return code =" + result.toString();
//                toastCallback.ToastResult(ResultsString);
                startTask();
            }
        });

    }

    private void startTask(){
        if(!checkGetHomePoint()) return;
        DJIDrone.getDjiGroundStation().startGroundStationTask(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(GroundStationResult result) {
                String ResultsString = "start task return code =" + result.toString();
//                toastCallback.ToastResult(ResultsString);
            }
        });
    }

    public void closeGs(final double latitude, final double longitude, final float altitude, final short heading){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().closeGroundStation(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(GroundStationResult result) {
                String ResultsString = "close gs return code =" + result.toString();
//                toastCallback.ToastResult(ResultsString);
                openGs(latitude, longitude, altitude, heading);
            }

        });
    }

    public void closeGs(){
        if(!checkGetHomePoint()) return;

        DJIDrone.getDjiGroundStation().closeGroundStation(new DJIGroundStationExecuteCallBack() {

            @Override
            public void onResult(GroundStationResult result) {
                String ResultsString = "close gs return code =" + result.toString();
//                toastCallback.ToastResult(ResultsString);
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

                DJIDrone.getDjiGimbal().updateGimbalAttitude(mPitch, null, null);

            }
        }.start();
    }

    // sets the angle the drone is facing relative to north
    public void setYawAngle(float angle){
        final float finalAngle = convertYawAngle(angle);
        openGs();
        new Thread()
        {
            public void run()
            {
                DJIDrone.getDjiGroundStation().sendFlightControlData(finalAngle, 0, 0, 0, new DJIExecuteResultCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                    }
                });

            }
        }.start();
    }

    public void resume(){

        DJIDrone.getDjiMainController().startUpdateTimer(100);
        DJIDrone.getDjiGroundStation().startUpdateTimer(100);
    }

    public void pause(){

        DJIDrone.getDjiMainController().stopUpdateTimer();
        DJIDrone.getDjiGroundStation().stopUpdateTimer();
    }

    public boolean droneUpdatesAreInitialized(){
        if (getCurrentLongitude() != 0 && getCurrentLatitude() != 0) {
            return true;
        }
        return false;
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

    public float getCurrentGimbalPitch(){
        return currentGimbalPitch;
    }

    public float getGimbalMinPitchAngle(){
        return -90;//DJIDrone.getDjiGimbal().getGimbalPitchMinAngle();
    }

    public float getGimbalMaxPitchAngle() {
        return 0;//DJIDrone.getDjiGimbal().getGimbalPitchMaxAngle();
    }

    public float getCurrentYaw(){
        return -currentYaw;
    }

    // receives new coordinates in meters
    public void setNewGPSCoordinates(float x, float y, float z, float heading) {

        // convert longitude and latitude to gps coordinates
        double converted_lat = homeLocationLatitude - metersToGPS(z);
        double converted_long = homeLocationLongitude + metersToGPS(x);
        float converted_heading = convertYawAngle(heading);

        flyToNewWaypoint(converted_lat, converted_long, y, converted_heading);
    }

    private void flyToNewWaypoint(double latitude, double longitude, float altitude, float heading) {
        closeGs(latitude, longitude, altitude, (short) heading);
    }

    private double metersToGPS(float meters) {
        return toDegrees(atan2(meters, EARTHS_RADIUS_IN_METERS));
    }

    public float getCurrentLongitudeInMeters(){
        return (float) (EARTHS_RADIUS_IN_METERS * tan(toRadians(currentLongitude - homeLocationLongitude)));
    }

    public float getCurrentLatitudeInMeters(){
        return (float) (EARTHS_RADIUS_IN_METERS * tan(toRadians(-(currentLatitude - homeLocationLatitude))));
    }

    private float convertYawAngle(float angle) {
        float converted_angle = -angle%360;
        if (converted_angle < -180){
            converted_angle += 360;
        } else if(converted_angle > 180){
            converted_angle -= 360;
        }
        return converted_angle;
    }

    public String getDroneInfoToString(){
        final StringBuffer sb = new StringBuffer();

        sb.append("drone altitude: ").append(currentAltitude).append("\n");
        sb.append("drone latitude: ").append(currentLatitude).append("\n");
        sb.append("drone longitude: ").append(currentLongitude).append("\n");
        sb.append("drone yaw: ").append(currentYaw).append("\n");
        sb.append("drone pitch: ").append(currentGimbalPitch).append("\n");

        return sb.toString();
    }

}
