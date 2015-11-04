package com.dji.sdkdemo;

import android.os.Bundle;
import android.view.View;

/**
 * Created by leegross on 10/31/15.
 */
public class MyWaypointsActivity extends DemoBaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_waypoints);
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    public void onClick(View v) {

    }
}

