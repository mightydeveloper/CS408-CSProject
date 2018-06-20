package com.example.jaejun.gait_app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Handler timer_handler;
    ImageView status_image;
    TextView text_view, status_text;
    boolean start = false;
    SensorManager sensorManager;
    Sensor accelerometer, gyroscope;
    SensorEventListener accelListener;
    boolean accelFlag = false, gyroFlag = false;
    long currentTimeStamp;
    long timediff = 2;
    double ax, ay, az, gx, gy, gz;
    Data_exploration.Datas origin_accel, origin_gyro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text_view = (TextView) findViewById(R.id.text);
        status_image = (ImageView) findViewById(R.id.image);
        status_text = (TextView) findViewById(R.id.status);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        timer_handler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                int sec = msg.what;

                if (sec == 0) {
                    this.sendEmptyMessageDelayed(-1, 100);
                    text_view.setText(String.valueOf(sec));
                    text_view.invalidate();
                } else if (sec == -1) {
                    text_view.setText("Now device is collecting your gait datas...");
                    text_view.invalidate();
                    collect_data(true);
                    this.sendEmptyMessageDelayed(-11, 10000);
                } else if (sec == -11) {
                    collect_data(false);
                    text_view.setText("Now device is classifying your condition...");
                    text_view.invalidate();

                    Data_exploration.Datas preprocessed_accel = Data_exploration.data_utilize(origin_accel);
                    Data_exploration.Datas preprocessed_gyro = Data_exploration.data_utilize(origin_gyro);

                    classify (preprocessed_accel, preprocessed_gyro);
                } else {
                    this.sendEmptyMessageDelayed(sec - 1, 1000);
                    text_view.setText(String.valueOf(sec));
                    text_view.invalidate();
                }
            }
        };

        accelListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int acc) {
            }

            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                long time = System.currentTimeMillis();

                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && !accelFlag) {
                    ax = event.values[0];
                    ay = event.values[1];
                    az = event.values[2];
                    accelFlag = true;
                } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE && !gyroFlag) {
                    gx = event.values[0];
                    gy = event.values[1];
                    gz = event.values[2];
                    gyroFlag = true;
                }

                if (accelFlag && gyroFlag && time >= currentTimeStamp + timediff) {
                    currentTimeStamp = time;

                    origin_accel.add(currentTimeStamp, ax, ay, az);
                    origin_gyro.add(currentTimeStamp, gx, gy, gz);

                    accelFlag = false;
                    gyroFlag = false;
                }
            }
        };
    }

    public void classify(Data_exploration.Datas accel, Data_exploration.Datas gyro){
        double[] features = Gait_feature.gait_features(accel, gyro).features;

        int type;
        if (features[0] == -1){
            type = 0;
        }
        else {
            type = KNeighborsClassifier.main(features);
        }
        text_view.setVisibility(View.INVISIBLE);
        status_text.setVisibility(View.VISIBLE);
        status_image.setVisibility(View.VISIBLE);

        if (type == 0){
            status_text.setText("Sober");
            status_image.setImageResource(R.drawable.sober);
        }
        /*else if (type == 1){
            status_text.setText("Mild Drunk");
            status_image.setImageResource(R.drawable.mild_drunk);
        }*/
        else{
            status_text.setText("Heavy Drunk");
            status_image.setImageResource(R.drawable.heavy_drunk);
        }
    }

    public void collect_data(boolean state) {
        if (state) {
            currentTimeStamp = System.currentTimeMillis();

            origin_accel = new Data_exploration.Datas();
            origin_gyro = new Data_exploration.Datas();

            accelFlag = false;
            gyroFlag = false;

            sensorManager.registerListener(accelListener, accelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(accelListener, gyroscope,
                    SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            sensorManager.unregisterListener(accelListener);
        }
    }

    public void click_start(View view) {
        if (start == false) {
            start = true;
            timer_handler.sendEmptyMessage(5);
        }
    }
}
