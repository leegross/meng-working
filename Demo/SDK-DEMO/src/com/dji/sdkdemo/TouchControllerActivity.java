package com.dji.sdkdemo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import dji.sdk.api.Camera.DJICameraDecodeTypeDef;
import dji.sdk.api.DJIDrone;
import dji.sdk.api.DJIDroneTypeDef;
import dji.sdk.api.DJIError;
import dji.sdk.api.Gimbal.DJIGimbalAttitude;
import dji.sdk.api.mediacodec.DJIVideoDecoder;
import dji.sdk.interfaces.DJIGeneralListener;
import dji.sdk.interfaces.DJIGimbalUpdateAttitudeCallBack;
import dji.sdk.interfaces.DJIReceivedVideoDataCallBack;

public class TouchControllerActivity extends DemoBaseActivity
{
    private TextView mConnectStateTextView;

    private TextView mGroundStationTextView;
    private MyGLSurfaceView mGLView;
    private DJIVideoDecoder mVideoDecoder = null;

    private DroneWrapper droneWrapper;
    private Timer mTimer;

    public void showMessage(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_waypoints);

        onInitSDK();

        checkDronePermissions();

        mGLView = (MyGLSurfaceView) findViewById(R.id.surfaceview);
        mConnectStateTextView = (TextView)findViewById(R.id.ConnectStateGsTextView);
        mGroundStationTextView = (TextView)findViewById(R.id.GroundStationInfoTV);

        droneWrapper = new DroneWrapper(mGLView);
        droneWrapper.setToastCallback(getToastCallback());
        droneWrapper.setUICallback(getUICallback());

        initDecoder();

        mGLView.setDroneWrapper(droneWrapper);

    }

    private void onInitSDK(){
        DJIDrone.initWithType(this.getApplicationContext(), DJIDroneTypeDef.DJIDroneType.DJIDrone_Inspire1);

        DJIDrone.connectToDrone();

    }

    private void checkDronePermissions(){
        new Thread(){
            public void run() {
                try {
                    DJIDrone.checkPermission(getApplicationContext(), new DJIGeneralListener() {

                        @Override
                        public void onGetPermissionResult(final int result) {
                            if (result == 0) {
                                TouchControllerActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showMessage(getString(R.string.demo_activation_message_title), DJIError.getCheckPermissionErrorDescription(result));
                                    }
                                });
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private DroneWrapper.uiCallback getUICallback() {
        DroneWrapper.uiCallback ui_callback = new DroneWrapper.uiCallback() {
            @Override
            public void UICallback(final String type, final String result) {
                TouchControllerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (type == "ground_station_text_view") {
                            mGroundStationTextView.setText(result);
                        }
                    }
                });
            }
        };
        return ui_callback;
    }

    class Task extends TimerTask {
        //int times = 1;

        @Override
        public void run()
        {
            checkConnectState();
        }

    };

    private DroneWrapper.setResultToToastCallback getToastCallback() {
        DroneWrapper.setResultToToastCallback toast_callback = new DroneWrapper.setResultToToastCallback(){

            @Override
            public void ToastResult(String result) {
                setResultToToast(result);
            }
        };
        return toast_callback;
    }

    private void setResultToToast(String result){
        Toast.makeText(TouchControllerActivity.this, result, Toast.LENGTH_SHORT).show();
    }

    private void checkConnectState(){

        TouchControllerActivity.this.runOnUiThread(new Runnable(){

            @Override
            public void run()
            {
                if(DJIDrone.getDjiCamera() != null){
                    boolean bConnectState = DJIDrone.getDjiCamera().getCameraConnectIsOk();
                    if(bConnectState){
                        mConnectStateTextView.setText(R.string.camera_connection_ok);
                    }
                    else{
                        mConnectStateTextView.setText(R.string.camera_connection_break);
                    }
                }
            }
        });

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
//        mDjiGLSurfaceView.resume();
        mGLView.onResume();

        mTimer = new Timer();
        Task task = new Task();
        mTimer.schedule(task, 0, 500);

        droneWrapper.resume();

        DJIDrone.getDjiGimbal().startUpdateTimer(1000);

        super.onResume();
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onPause() {
//        mDjiGLSurfaceView.pause();
        mGLView.onPause();

        if(mTimer!=null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }

        droneWrapper.pause();

        DJIDrone.getDjiGimbal().stopUpdateTimer();

        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        if(DJIDrone.getDjiCamera() != null)
            DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(null);
//        mDjiGLSurfaceView.destroy();
        super.onDestroy();
    }

    public void takeOff(View v){
        droneWrapper.takeOff();
    }

    public void addWaypoints1(View v){
        droneWrapper.openGs(droneWrapper.getHomeLocationLatitude(), droneWrapper.getHomeLocationLongitude(), 10f, (short) 0);
    }

    public void addWaypoints2(View v){
        droneWrapper.openGs(droneWrapper.getHomeLocationLatitude(), droneWrapper.getHomeLocationLongitude(), 2f, (short) 0);
    }

    public void closeGs(View v){
        droneWrapper.closeGs();
    }

    private void initDecoder() {
        DJIDrone.getDjiCamera().setDecodeType(DJICameraDecodeTypeDef.DecoderType.Hardware);
        final Context mContext = this;

        DJIReceivedVideoDataCallBack mReceivedVideoDataCallBack = new DJIReceivedVideoDataCallBack() {

            @Override
            public void onResult(final byte[] videoBuffer, final int size) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SurfaceTexture mSurfaceTexture = mGLView.getRenderer().getSurfaceTexture();
                        if (mSurfaceTexture == null) {
                            return;
                        }
                        if (mVideoDecoder == null) {
                            Surface mSurface = new Surface(mGLView.getRenderer().getSurfaceTexture());
                            mVideoDecoder = new DJIVideoDecoder(mContext, mSurface);
                        }

                        DJIDrone.getDjiCamera().sendDataToDecoder(videoBuffer, size);
                    }
                });

            }
        };

        DJIDrone.getDjiCamera().setReceivedVideoDataCallBack(mReceivedVideoDataCallBack);

        DJIGimbalUpdateAttitudeCallBack mGimbalUpdateAttitudeCallBack = new DJIGimbalUpdateAttitudeCallBack() {

            DroneWrapper dw = droneWrapper;

            @Override
            public void onResult(final DJIGimbalAttitude attitude) {

                StringBuffer sb = new StringBuffer();
                sb.append("pitch=").append(attitude.pitch).append("\n");
                sb.append("roll=").append(attitude.roll).append("\n");
                sb.append("yaw=").append(attitude.yaw).append("\n");
                sb.append("yawAngle=").append(DJIDrone.getDjiGimbal().getYawAngle()).append("\n");
                sb.append("roll adjust=").append(attitude.rollAdjust).append("\n");

                if (dw != null) {
                    droneWrapper.setCurrentGimbalPitch(attitude.pitch);

                }

            }

        };

        DJIDrone.getDjiGimbal().setGimbalUpdateAttitudeCallBack(mGimbalUpdateAttitudeCallBack);
    }
}
