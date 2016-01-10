package com.dji.sdkdemo;

import static java.lang.Math.tan;
import static java.lang.StrictMath.toRadians;

/**
 * Created by leegross on 11/8/15.
 */
public class Constants {
    public static int TABLET_WIDTH = 1920;
    public int TABLET_HEIGHT = 1045;

    public static float HORIZONTAL_FOV = 94;
    public static float ASPECT_RATIO = 16f/9.0f;
    public static float GL_SURFACE_WIDTH = 1580;
    public static float GL_SURFACE_HEIGHT = 889;
    public static float SURFACE__HORIZONTAL_CENTER = GL_SURFACE_WIDTH/2.0f;
    public static float SURFACE_VERTICAL_CENTER = GL_SURFACE_HEIGHT/2.0f;

    public static float FRUST_NEAR = .1f;
    public static float SPHERE_RADIUS = 500f;

    public static float MAX_DIST = 30;
    public static float MIN_ALTITUDE = .5f;

    public static float STARTING_ALTITUDE = 1.1f;

    public static double EARTHS_RADIUS_IN_METERS = 6371393;

    // used to determine whether to use an image or the stream for the texture
    // don't forget to change code in fragment shader
    public static boolean USE_CAMERA_STREAM = false;

    public static float FRUST_NEAR_SCALE_X = (float) (FRUST_NEAR * tan(toRadians(HORIZONTAL_FOV)/2.0f));
    public static float FRUST_NEAR_SCALE_Y = (float) (FRUST_NEAR * tan(toRadians(HORIZONTAL_FOV/ASPECT_RATIO)/2.0f));



}
