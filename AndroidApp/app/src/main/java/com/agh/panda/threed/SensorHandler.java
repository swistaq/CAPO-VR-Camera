package com.agh.panda.threed;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.agh.panda.filters.GyroFilter;

import pl.edu.agh.amber.common.AmberClient;
import pl.edu.agh.amber.hitec.HitecProxy;

import static android.content.Context.SENSOR_SERVICE;

public class SensorHandler extends Thread implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] mGravity;
    private float[] mGeomagnetic;

    private float azimut = 0.0f;
    private float roll = 0.0f;

    private final static int IGNORE_DELTA = 10;
    private int prevAzimut = 0;
    private int prevRoll = 0;

    float azimutSmoothed = 0.0f;
    float rollSmoothed = 0.0f;

    GyroFilter azimutFilter = new GyroFilter();
    GyroFilter rollFilter = new GyroFilter();

    private float horizontalZero = 0.0f;
    private boolean calibrateHorizontal = false;

    private AmberClient client;
    private Context context;
    private final HitecProxy hitecProxy;

    public SensorHandler(AmberClient client, Context context) {
        super();
        this.context = context;
        this.client = client;
        hitecProxy = new HitecProxy(client, 0);
        hitecProxy.setSpeed(0,20);
    }

    @Override
    public void run() {
        init();
        super.run();
    }

    private void init() {
        mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // pobranie danych z czujników, przetworzenie ich i wygładzenie przy pomocy filtra
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                azimut = orientation[0]; // patrz lewo/prawo
                roll = orientation[2]; // patrz góra/dół

            }
        }

        // kalibracja północy
        azimutSmoothed = azimutFilter.filterValue(azimut);
        if (calibrateHorizontal) {
            horizontalZero = azimutSmoothed;
            System.out.println("Horizontal position calibrated");
            calibrateHorizontal = false;
        }
        rollSmoothed = rollFilter.filterValue(roll);

        int turnAddress = 0;

        int leftZero = 180;
        int leftAddress = 1;
        //poprawka przesunięcia mocowania
        int rightZero = 2;
        int rightAddress = 2;

        int horizontalDelta = convertAzimut(normalizeAzimut(azimutSmoothed));
        int verticalDelta = convertRoll(rollSmoothed);
//        System.out.println("lewo/prawo: " + azimut + " góra/dół: " + roll + " obrót w poziomie: " + horizontalDelta+" obrót góra/dół: "+verticalDelta);

        // sprawdzenie czy zmiana przekracza ustaloną deltę, jeśli nie to nie zmieniamy pozycji - zmniejsza to drgania i skoki w przypadku zakłóceń
        if (Math.abs(prevAzimut - horizontalDelta) > IGNORE_DELTA) {
            hitecProxy.setAngle(turnAddress, horizontalDelta);
            prevAzimut = horizontalDelta;
        }

        if (Math.abs(prevRoll - verticalDelta) > IGNORE_DELTA) {
            {   //te dwa wywołania muszą być jednoczesne
                hitecProxy.setAngle(leftAddress, leftZero - verticalDelta);
                hitecProxy.setAngle(rightAddress, rightZero + verticalDelta);
            }
            prevRoll = verticalDelta;
        }
    }

    private float normalizeAzimut(float azimutSmoothed) {
        return (float) ((((azimutSmoothed - horizontalZero) + Math.PI) % (Math.PI*2)) - Math.PI);
    }

    private static int convertRoll(float roll) {
        // patrzenie poniżej horyzontu
        if (roll >= -Math.PI/2) {
            return 0;
        }
        if (roll <= -0.83*Math.PI){
            return 70;
        }
        //konwersja rad -> stopnie
        int degs = (int) ((-roll * 180) / Math.PI);
        return degs - 90;
    }

    private static int convertAzimut(float azimut) {
        // patrzenie za daleko w lewo
        if (azimut < -(Math.PI/2))
            return 180;
        // patrzenie za daleko w prawo
        if (azimut > (Math.PI/2))
            return 0;
        return 90+(int)((-azimut * 180)/Math.PI);
    }


    public void calibrate() {
        calibrateHorizontal = true;
    }
}
