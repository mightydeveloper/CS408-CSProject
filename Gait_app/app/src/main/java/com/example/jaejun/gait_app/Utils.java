package com.example.jaejun.gait_app;

import android.util.Log;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by jaejun on 2018-05-27.
 */

public class Utils {

    public static double get_ellipse_area(double x, double y){
        return Math.PI * x * y;
    }

    public static Data_exploration.Datas rotation_datas(double theta, Data_exploration.Datas datas){
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);

        double each_x, each_z;
        double rotation_x, rotation_z;

        for (int i=0; i<datas.size(); i++){
            each_x = datas.x.get(i);
            each_z = datas.z.get(i);

            rotation_x = cos * each_x - sin * each_z;
            rotation_z = sin * each_x + cos * each_z;

            datas.x.set(i, rotation_x);
            datas.z.set(i, rotation_z);
        }

        return datas;
    }

    public static double get_lr_slope(ArrayList<Double> x, ArrayList<Double> y){
        Assert.assertTrue(x.size() == y.size());

        int size = x.size();
        double sum_x = 0, sum_x_square = 0, sum_xy = 0, sum_y = 0;
        double each_x, each_y;
        for (int i=0; i<size; i++){
            each_x = x.get(i);
            each_y = y.get(i);

            sum_x += each_x;
            sum_x_square += Math.pow(each_x, 2);
            sum_y += each_y;
            sum_xy += each_x * each_y;
        }

        double gradient = (size * sum_xy - sum_x * sum_y) / (size * sum_x_square - Math.pow(sum_x, 2));
        double theta = Math.atan(gradient);

        return theta;
    }

    public static ArrayList<Gait_feature.Peak> find_peaks(ArrayList<Gait_feature.Magnitude> magnitudes, Gait_feature.Peak_type peak_type, int min_distance, double accel_gravity){
        ArrayList<Gait_feature.Peak> first_peaks = new ArrayList<Gait_feature.Peak>();
        ArrayList<Gait_feature.Peak> first_valleys = new ArrayList<Gait_feature.Peak>();
        int total_size = magnitudes.size();

        double prev, now, next;
        now = magnitudes.get(0).magnitude;
        next = magnitudes.get(2).magnitude;
        for (int i = 1; i < total_size - 1; i ++){
            prev = now;
            now = next;
            next = magnitudes.get(i + 1).magnitude;

            long time = magnitudes.get(i).time;
            if (!peak_type.equals(Gait_feature.Peak_type.VALLEY) && now >= prev && now >= next && now > accel_gravity){
                first_peaks.add(new Gait_feature.Peak(i, time, now));
            }
            else if (!peak_type.equals(Gait_feature.Peak_type.PEAK) && now <= prev && now <= next && now < accel_gravity){
                first_valleys.add(new Gait_feature.Peak(i, time, now));
            }
        }

        ArrayList<Gait_feature.Peak> peaks = new ArrayList<Gait_feature.Peak>();
        ArrayList<Gait_feature.Peak> valleys = new ArrayList<Gait_feature.Peak>();
        boolean[] valid = new boolean[total_size];

        if (!peak_type.equals(Gait_feature.Peak_type.VALLEY)){
            Collections.sort(first_peaks, Collections.<Gait_feature.Peak>reverseOrder());

            Arrays.fill(valid, true);

            for(Gait_feature.Peak i : first_peaks){
                int index = i.index;
                if (valid[index]){
                    for (int j=1; j<=min_distance; j++){
                        if (index - j >= 0){
                            valid[index - j] = false;
                        }
                        if (index + j < total_size){
                            valid[index + j] = false;
                        }
                    }
                    peaks.add(i);
                }
            }
        }
        if (!peak_type.equals(Gait_feature.Peak_type.PEAK)){
            Collections.sort(first_valleys);
            Arrays.fill(valid, true);

            for(Gait_feature.Peak i : first_valleys){
                int index = i.index;
                if (valid[index]){
                    for (int j=1; j<=min_distance; j++){
                        if (index - j >= 0){
                            valid[index - j] = false;
                        }
                        if (index + j < total_size){
                            valid[index + j] = false;
                        }
                    }
                    valleys.add(i);
                }
            }
        }

        Comparator<Gait_feature.Peak> index_comparator = new Comparator<Gait_feature.Peak>(){
            public int compare(Gait_feature.Peak a, Gait_feature.Peak b){
                return a.index - b.index;
            }
        };

        if(peak_type.equals(Gait_feature.Peak_type.PEAK)){
            Collections.sort(peaks, index_comparator);
            return peaks;
        }
        else if(peak_type.equals(Gait_feature.Peak_type.VALLEY)){
            Collections.sort(valleys, index_comparator);
            return valleys;
        }
        else{
            ArrayList<Gait_feature.Peak> both = peaks;
            both.addAll(valleys);
            Collections.sort(both, index_comparator);
            return both;
        }
    }

    public static ArrayList<Double> reject_outliers_double(ArrayList<Double> data, double m){
        double mean = mean_double(data);
        double std = Math.sqrt(var_double(data));

        ArrayList<Double> new_data = new ArrayList<Double>();
        for(double i : data){
            if (Math.abs(i - mean) <= m * std && i > 0){
                new_data.add(i);
            }
        }

        return new_data;
    }
    public static ArrayList<Integer> reject_outliers_int(ArrayList<Integer> data, double m){
        double mean = mean_int(data);
        double std = Math.sqrt((double)var_int(data));

        ArrayList<Integer> new_data = new ArrayList<Integer>();
        for(int i : data){
            if (Math.abs((double)i - mean) <= m * std){
                new_data.add(i);
            }
        }

        return new_data;
    }

    public static double mean_double(ArrayList<Double> data) {
        double mean = 0;
        int size = data.size();

        for(double i : data){
            mean += i / (double)(size);
        }

        return mean;
    }
    public static double mean_int(ArrayList<Integer> data) {
        double mean = 0;
        int size = data.size();

        for(int i : data){
            mean += (double)(i) / (double)(size);
        }

        return mean;
    }
    public static double var_double(ArrayList<Double> data) {
        double mean = 0, mean_square = 0;
        int size = data.size();

        for(double i : data){
            mean += i / (double)(size);
            mean_square += Math.pow(i, 2) / (double)(size);
        }

        return mean_square - Math.pow(mean, 2);
    }
    public static double var_int(ArrayList<Integer> data) {
        double mean = 0, mean_square = 0;
        int size = data.size();

        for(int i : data){
            mean += (double)(i) / (double)(size);
            mean_square += Math.pow(i, 2) / (double)(size);
        }

        return mean_square - Math.pow(mean, 2);
    }
}