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

        float mag = sqrt(dot(v, v));

        for (int i = 0; i < v.length; i++) {
            out[i] = v[i]/mag;
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

    public static float[] multiplyMV4(float[] M, float[] V){
        float[] output = new float[4];
        for (int i = 0; i < 4; i++){
            for (int j = 0; j < 4; j++){
                output[i] += M[j*4 + i] * V[j];
            }
        }
        return output;
    }

    public static float dot(float[] a, float[] b){
        if (a.length != b.length) return 0;
        float result = 0.0f;
        for (int i = 0; i < a.length; i++){
            result += (double) a[i] * b[i];
        }
        return result;
    }

    public static float[] getRotationMatrix(float angle, float[] rotation_axis){
        float[] rotationMatrix = new float[16];
        Matrix.setRotateM(rotationMatrix, 0, angle, rotation_axis[0], rotation_axis[1], rotation_axis[2]);
        return rotationMatrix;
    }

    public static float[] multiplyMM(float[] M1, float[] M2){
        float[] output = new float[16];
        Matrix.multiplyMM(output, 0, M1, 0, M2, 0);
        return output;
    }

    public static float[] getTranspose(float[] M){
        float[] transpose = new float[16];
        Matrix.transposeM(transpose, 0, M, 0);
        return transpose;
    }

    public static float magnitude(float[] v){
        return sqrt(dot(v, v));
    }

    public static float[] scaleVtoMag(float[] v, float mag){
        float[] result = new float[v.length];
        float old_mag = magnitude(v);
        for (int i = 0; i < v.length; i++){
            result[i] = v[i]/old_mag * mag;
        }
        return result;
    }


}
