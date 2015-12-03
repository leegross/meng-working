package com.dji.sdkdemo.util;

import android.opengl.Matrix;

import static android.util.FloatMath.sqrt;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.toDegrees;
import static java.lang.StrictMath.atan2;

/**
 * Created by leegross on 11/23/15.
 */
public class OperationsHelper {
    public static boolean floatEquals(float v1, float v2) {
        if (abs(v1 - v2) < .00001) {
            return true;
        }
        return false;
    }

    public static float[] addArrays(float[] a1, float[] a2){
        if (a1.length != a2.length) {
            return new float[0];
        }
        float[] sum = new float[a1.length];
        for (int i = 0; i < a1.length; i++) {
            sum[i] = a1[i] + a2[i];
        }
        return sum;
    }

    public static float[] getRotationMatrixFromAtoB(float[] a, float[] b){
        // compute crossproduct
        float v1 = a[1] * b[2] - a[2] * b[1];
        float v2 = a[2] * b[0] - a[0] * b[2];
        float v3 = a[0] * b[1] - a[1] * b[0];

        // compute magnitude/ sine
        float s = sqrt(v1 * v1 + v2 * v2 + v3 * v3);
        // compute dot product/ cos
        float c = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];

        float[] v_x = {0, -v3, v2, v3, 0, -v1, -v2, v1, 0};

        float[] v_x_sq = {-v3*v3 - v2*v2, v1*v2, v1*v3, v1*v2, -v3*v3 - v1*v1, v2*v3, v1*v3, v2*v3, -v2*v2-v1*v1};

        float[] I = {1, 0, 0, 0, 1, 0, 0, 0, 1};
        float k;
        if (s == 0) {
            k = 1;
        } else {
            k =  (1-c)/(s * s);
        }

        float[] R = addMM(addMM(I, v_x), multiplyMk(v_x_sq, k));
        return R;
    }

    // element wise addition
    public static float[] addMM(float[] A, float[] B){
        if (A.length != B.length) return new float[0];
        float[] out = new float[A.length];
        for (int i = 0; i < A.length; i++){
            out[i] = A[i] + B[i];
        }
        return out;
    }

    // multiplies a every element in matrix M by a constant k
    public static float[] multiplyMk(float[] M, float k){
        float[] out = new float[M.length];
        for (int i = 0; i < M.length; i++){
            out[i] = M[i] * k;
        }
        return out;
    }

    public static float[] normalizeV(float[] v){
        float[] out = new float[v.length];

        float mag = 0;
        for (float aV : v) {
            mag = aV * aV;
        }
        mag = sqrt(mag);

        for (int i = 0; i < v.length; i++) {
            out[i] = v[i]/mag;
        }
        return out;
    }

    public static float rotationMatrixtoTheta(float[] R){
        float[] unit_z = new float[]{0, 0, 1};
        float[] v = multiplyMV3(R, unit_z);
        return (float) toDegrees(atan2(v[1], v[2]));
    }

    public static float rotationMatrixtoPhi(float[] R){
        float[] unit_z = new float[]{0, 0, 1};
        float[] v = multiplyMV3(R, unit_z);
        return (float) toDegrees(atan2(v[0], v[2]));
    }

    public static float[] multiplyMV3(float[] M, float[] v){
        float[] out = new float[3];
        for (int i = 0; i < 3; i ++){
            out[i] = M[i*3] * v[0] + M[i*3+1] * v[1] + M[i*3+2] * v[2];
        }
        return out;
    }

    public static float[] getInverse(float[] M){
        float[] inverse = new float[M.length];
        Matrix.invertM(inverse, 0, M, 0);
        return inverse;
    }

    public static float[] multiplyMV(float[] M, float[] V){
        float[] output = new float[V.length];
        Matrix.multiplyMV(output, 0, M, 0, V, 0);
        return output;
    }

}
