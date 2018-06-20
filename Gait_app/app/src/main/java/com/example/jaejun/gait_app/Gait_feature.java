package com.example.jaejun.gait_app;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;

import mr.go.sgfilter.SGFilter;

/**
 * Created by jaejun on 2018-05-27.
 */

public class Gait_feature {

    double[] features = new double[9];
    double[] mean = {3.29748038e+04, 6.96591656e+00, 1.35007044e+01, 3.25221561e+00,
            4.00627063e+02, 6.09306931e+02, 5.88492789e+03, 5.08393580e+02,
            1.72852883e+01};
    double[] var = {1.28851754e+08, 1.09039102e+01, 8.08726021e+00, 6.46714886e+00,
            6.53855069e+03, 3.45331503e+03, 5.46831203e+07, 1.59753302e+03,
            1.14380687e+02};

    public Gait_feature(double average_power, double min_stretch_length, double max_stretch_length, double var_stretch_length,
                        double min_step_time, double max_step_time, double var_step_time, double avg_step_time, double sway_area){
        if (average_power == -1){
            features[0] = -1;
            features[1] = -1;
            features[2] = -1;
            features[3] = -1;
            features[4] = -1;
            features[5] = -1;
            features[6] = -1;
            features[7] = -1;
            features[8] = -1;
        }
        else {
            features[0] = (average_power - mean[0]) / Math.sqrt(var[0]);
            features[1] = (min_stretch_length - mean[1]) / Math.sqrt(var[1]);
            features[2] = (max_stretch_length - mean[2]) / Math.sqrt(var[2]);
            features[3] = (var_stretch_length - mean[3]) / Math.sqrt(var[3]);
            features[4] = (min_step_time - mean[4]) / Math.sqrt(var[4]);
            features[5] = (max_step_time - mean[5]) / Math.sqrt(var[5]);
            features[6] = (var_step_time - mean[6]) / Math.sqrt(var[6]);
            features[7] = (avg_step_time - mean[7]) / Math.sqrt(var[7]);
            features[8] = (sway_area - mean[8]) / Math.sqrt(var[8]);
            //Log.d("after", String.valueOf(features[0])+", "+String.valueOf(features[1])+", "+String.valueOf(features[2])+", "+String.valueOf(features[3]));
        }
    }

    // hyper parameter
    static final int smooth_nl = 21;
    static final int smooth_nr = 26;
    static final int smooth_degree = 9;
    static final int peak_min_distance = 25;
    static final double accel_gravity = 10;
    static final double outlier_m_magnitude = 3.8186357354712595;
    static final double outlier_m_other = 1.9937811659023188;
    static final double sway_accuracy = 0.95;
    static final double sway_unit = 0.01;

    // hyper parameter

    public enum Peak_type{PEAK, VALLEY, BOTH};

    public static class Magnitude{
        long time;
        double magnitude;

        public Magnitude(long time, double magnitude){
            this.time = time;
            this.magnitude = magnitude;
        }
    }

    public static class Peak implements Comparable<Peak>{
        int index;
        long time;
        double magnitude;

        public Peak(int index, long time, double magnitude){
            this.index = index;
            this.time = time;
            this.magnitude = magnitude;
        }

        @Override
        public int compareTo(@NonNull Peak b) {
            if (this.magnitude < b.magnitude){
                return -1;
            }
            else if(this.magnitude > b.magnitude){
                return 1;
            }
            else{
                return 0;
            }
        }
    }

    public static Gait_feature gait_features(Data_exploration.Datas accel, Data_exploration.Datas gyro){

        ArrayList<Magnitude> magnitudes = calc_magnitude(accel);
        double[] magnitude_for_FFT = zero_padding(magnitudes);
        double[] FFTResult = new FFT(1024).getAbs(magnitude_for_FFT);

        double average_power = 0;
        for (int i=1; i<FFTResult.length; i++) {
            average_power += FFTResult[i];
        }

        magnitudes = reject_outliers_magnitude(magnitudes, outlier_m_magnitude);
        magnitudes = smoothing_magnitude(magnitudes, smooth_nl, smooth_nr, smooth_degree);

        ArrayList<Peak> peaks = Utils.find_peaks(magnitudes, Peak_type.PEAK, peak_min_distance, accel_gravity);
        ArrayList<Peak> boths = Utils.find_peaks(magnitudes, Peak_type.BOTH, peak_min_distance, accel_gravity);

        if (boths.size() <= 1 || peaks.size() <= 1){
            return new Gait_feature(-1, -1, -1, -1, -1, -1,
                    -1, -1, -1);
        }

        ArrayList<Double> stretch_lengths = get_stretch_lengths(boths);
        ArrayList<Integer> step_times = get_step_times(peaks);

        stretch_lengths = Utils.reject_outliers_double(stretch_lengths, outlier_m_other);
        step_times = Utils.reject_outliers_int(step_times, outlier_m_other);

        if (stretch_lengths.size() <= 1 || step_times.size() <= 1){
            return new Gait_feature(-1, -1, -1, -1, -1, -1,
                    -1, -1, -1);
        }

        double min_stretch_length = stretch_lengths.get(0), max_stretch_length = stretch_lengths.get(0);
        int min_step_time = step_times.get(0), max_step_time = step_times.get(0);

        for (double i : stretch_lengths) {
            if (i > max_stretch_length) {
                max_stretch_length = i;
            }
            if (i < min_stretch_length) {
                min_stretch_length = i;
            }
        }

        for (int i : step_times) {
            if (i > max_step_time) {
                max_step_time = i;
            }
            if (i < min_step_time) {
                min_step_time = i;
            }
        }

        double var_stretch_length = Utils.var_double(stretch_lengths);
        double var_step_time = Utils.var_int(step_times);
        double avg_step_time = Utils.mean_int(step_times);

        double theta = Utils.get_lr_slope(gyro.x, gyro.z);
        Data_exploration.Datas rotation_gyro = Utils.rotation_datas(-1 * theta, gyro);
        double sway_area = get_sway_area(rotation_gyro, sway_accuracy, sway_unit);

        Gait_feature result = new Gait_feature(average_power, min_stretch_length, max_stretch_length,
                var_stretch_length, (double)min_step_time, (double)max_step_time, var_step_time, avg_step_time, sway_area);

        return result;
    }

    public static double[] zero_padding(ArrayList<Magnitude> magnitudes){
        double[] new_magnitudes = new double[1024];
        for (int i = 0; i < 1024; i++) {
            if (i < magnitudes.size()) {
                new_magnitudes[i] = magnitudes.get(i).magnitude;
            }
            else {
                new_magnitudes[i] = 0.0;
            }
        }
        return new_magnitudes;
    }

    public static double get_sway_area(Data_exploration.Datas gyro, double accuracy, double unit){

        int size = gyro.size();

        double each_x, each_z;
        double center_x = 0, center_z = 0;
        double max_x = 0, max_z = 0, min_x = 9999, min_z = 9999;
        for (int i=0; i<size; i++){
            each_x = gyro.x.get(i);
            each_z = gyro.z.get(i);

            center_x += each_x / (double)(size);
            center_z += each_z / (double)(size);

            if (each_x > max_x){
                max_x = each_x;
            }
            if (each_x < min_x){
                min_x = each_x;
            }
            if (each_z > max_z){
                max_z = each_z;
            }
            if (each_z < min_z){
                min_z = each_z;
            }
        }

        double radian_x, radian_z;
        if (max_x - center_x > center_x - min_x){
            radian_x = max_x - center_x;
        }
        else{
            radian_x = center_x - min_x;
        }
        if (max_z - center_z > center_z - min_z){
            radian_z = max_z - center_z;
        }
        else{
            radian_z = center_z - min_z;
        }

        int cnt = 0;
        double k;
        for (int i=0; i<size; i++){
            each_x = gyro.x.get(i);
            each_z = gyro.z.get(i);

            k = Math.pow(each_x - center_x, 2) / Math.pow(radian_x, 2) + Math.pow(each_z - center_z , 2) / Math.pow (radian_z, 2);

            if (k <= 1){
                ++cnt;
            }
        }
        double ratio = ((double)(cnt) / (double)(size));

        if (ratio > accuracy){
            while (true){
                radian_x -= unit;
                radian_z -= unit;

                cnt = 0;
                for (int i=0; i<size; i++){
                    each_x = gyro.x.get(i);
                    each_z = gyro.z.get(i);

                    k = Math.pow(each_x - center_x, 2) / Math.pow(radian_x, 2) + Math.pow(each_z - center_z , 2) / Math.pow (radian_z, 2);

                    if (k <= 1){
                        ++cnt;
                    }
                }
                ratio = ((double)(cnt) / (double)(size));

                if (ratio < accuracy){
                    radian_x += unit;
                    radian_z += unit;
                    break;
                }
            }
        }
        else if (ratio < accuracy){
            while (true){
                radian_x += unit;
                radian_z += unit;

                cnt = 0;
                for (int i=0; i<size; i++){
                    each_x = gyro.x.get(i);
                    each_z = gyro.z.get(i);

                    k = Math.pow(each_x - center_x, 2) / Math.pow(radian_x, 2) + Math.pow(each_z - center_z , 2) / Math.pow (radian_z, 2);

                    if (k <= 1){
                        ++cnt;
                    }
                }
                ratio = ((double)(cnt) / (double)(size));

                if (ratio >= accuracy){
                    break;
                }
            }
        }

        return Utils.get_ellipse_area(radian_x, radian_z);
    }

    public static ArrayList<Double> get_stretch_lengths(ArrayList<Peak> boths){
        double x1 = -1, x2 = -1;
        ArrayList<Double> stretch_lengths = new ArrayList<Double>();

        for (Peak i : boths){
            if (x2 == -1){
                x2 = i.magnitude;
                continue;
            }
            x1 = x2;
            x2 = i.magnitude;
            stretch_lengths.add(x1 - x2);
        }

        return stretch_lengths;
    }

    public static ArrayList<Integer> get_step_times(ArrayList<Peak> peaks){
        long x1 = -1, x2 = -1;
        ArrayList<Integer> step_times = new ArrayList<Integer>();

        for(Peak i : peaks){
            if (x1 == -1){
                x1 = i.time;
                continue;
            }
            x2 = x1;
            x1 = i.time;
            step_times.add((int)(x1 - x2));
        }

        return step_times;
    }

    public static ArrayList<Magnitude> calc_magnitude(Data_exploration.Datas datas){
        long time;
        double x, y, z, magnitude;
        ArrayList<Magnitude> magnitudes = new ArrayList<Magnitude>();

        for (int i=0; i<datas.size(); i++){
            time = datas.time.get(i);
            x = datas.x.get(i);
            y = datas.y.get(i);
            z = datas.z.get(i);
            magnitude = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
            magnitudes.add(new Magnitude(time, magnitude));
        }

        return magnitudes;
    }

    public static ArrayList<Magnitude> reject_outliers_magnitude(ArrayList<Magnitude> datas, double outlier_m){
        long time;
        double magnitude;
        int size = datas.size();
        double mean = 0, mean_square = 0;

        for (Magnitude i : datas){
            magnitude = i.magnitude;
            mean += magnitude / (double)size;
            mean_square += Math.pow(magnitude, 2) / (double)size;
        }

        double std = Math.sqrt(mean_square - Math.pow(mean, 2));
        ArrayList<Magnitude> new_magnitudes = new ArrayList<Magnitude>();

        for (Magnitude i : datas) {
            time = i.time;
            magnitude = i.magnitude;

            if (Math.abs(magnitude - mean) < outlier_m * std) {
                new_magnitudes.add(new Magnitude(time, magnitude));
            }
        }

        return new_magnitudes;
    }

    //SGFilter : https://code.google.com/archive/p/savitzky-golay-filter/
    public static ArrayList<Magnitude> smoothing_magnitude(ArrayList<Magnitude> datas, int nl, int nr, int degree){
        int data_size = datas.size();

        double[] no_smooth = new double[data_size];
        for (int i=0; i<data_size; i++){
            no_smooth[i] = datas.get(i).magnitude;
        }

        double[] coeffs = SGFilter.computeSGCoefficients(nl, nr, degree);
        SGFilter sgFilter = new SGFilter(nl, nr);
        double[] smooth = sgFilter.smooth(no_smooth, coeffs);

        for (int i=0; i<data_size; i++){
            datas.get(i).magnitude = smooth[i];
        }

        return datas;
    }
}
