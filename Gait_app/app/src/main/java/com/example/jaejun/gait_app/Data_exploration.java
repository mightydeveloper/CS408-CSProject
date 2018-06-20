package com.example.jaejun.gait_app;

import junit.framework.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by jaejun on 2018-05-27.
 */

public class Data_exploration {

    static final int interval = 10;

    public static class Datas{
        ArrayList<Long> time;
        ArrayList<Double> x, y, z;

        public Datas(){
            this.time = new ArrayList<Long>();
            this.x = new ArrayList<Double>();
            this.y = new ArrayList<Double>();
            this.z = new ArrayList<Double>();
        }

        public void add(long time, double x, double y, double z){
            this.time.add(time);
            this.x.add(x);
            this.y.add(y);
            this.z.add(z);
        }

        public void add(Datas data){
            Assert.assertTrue(data.size() == 1);

            this.time.add(data.time.get(0));
            this.x.add(data.x.get(0));
            this.y.add(data.y.get(0));
            this.z.add(data.z.get(0));
        }

        public Datas get(int i){
            Datas data = new Datas();
            data.time.add(this.time.get(i));
            data.x.add(this.x.get(i));
            data.y.add(this.y.get(i));
            data.z.add(this.z.get(i));

            return data;
        }

        public int size(){
            Assert.assertTrue(this.time.size() == this.x.size()
                    && this.x.size() == this.y.size()
                    && this.y.size() == this.z.size());

            return this.time.size();
        }
    }

    public static Datas data_utilize (Datas origin_datas){
        int old_end_index = origin_datas.size() - 1;
        long old_start_time = origin_datas.time.get(0);
        long old_end_time = origin_datas.time.get(old_end_index);
        int interp_begin_index = 0, interp_end_index = 0;
        long t;
        double x, y, z;
        Datas utilized_datas = new Datas(), interp_data;

        for (long time=old_start_time; time<old_end_time; time+=interval){
            Assert.assertTrue(time >= origin_datas.time.get(interp_begin_index));

            while (true){
                interp_begin_index++;
                if (origin_datas.time.get(interp_begin_index) > time){
                    interp_end_index = interp_begin_index;
                    interp_begin_index --;
                    break;
                }
            }

            interp_data = interpolate (origin_datas.get(interp_begin_index), origin_datas.get(interp_end_index), time);

            t = interp_data.time.get(0) - old_start_time;
            x = interp_data.x.get(0);
            y = interp_data.y.get(0);
            z = interp_data.z.get(0);
            utilized_datas.add (t, x, y, z);
        }

        return utilized_datas;
    }

    public static Datas interpolate(Datas begin_data, Datas end_data, long interp_time){
        Assert.assertTrue(begin_data.size() == 1 && end_data.size() == 1);

        Datas interp_data = new Datas();

        long begin_time = begin_data.time.get(0);
        double begin_x = begin_data.x.get(0);
        double begin_y = begin_data.y.get(0);
        double begin_z = begin_data.z.get(0);
        long end_time = end_data.time.get(0);
        double end_x = end_data.x.get(0);
        double end_y = end_data.y.get(0);
        double end_z = end_data.z.get(0);

        double interp_x = ((double)(interp_time - begin_time) * end_x + (double)(end_time - interp_time) * begin_x) / (double)(end_time - begin_time);
        double interp_y = ((double)(interp_time - begin_time) * end_y + (double)(end_time - interp_time) * begin_y) / (double)(end_time - begin_time);
        double interp_z = ((double)(interp_time - begin_time) * end_z + (double)(end_time - interp_time) * begin_z) / (double)(end_time - begin_time);

        interp_data.add(interp_time, interp_x, interp_y, interp_z);

        return interp_data;
    }
}
