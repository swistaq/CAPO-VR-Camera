package com.agh.panda.threed;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.agh.panda.threed.gl.VideoTextureRenderer;

import java.io.IOException;

import pl.edu.agh.amber.common.AmberClient;

import static com.agh.panda.threed.R.id.textureViewL;
import static com.agh.panda.threed.R.id.textureViewR;


public class StreamActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;

    private DisplayMetrics metrics;

    private MediaPlayer mediaPlayerL;
    private MediaPlayer mediaPlayerR;

    private final static String LEFT_STREAM_IP = "rtsp://192.168.2.102:8080/";
    private final static String RIGHT_STREAM_IP = "rtsp://192.168.2.102:8081/";

    private TextureView mPreviewL;
    private TextureView mPreviewR;

    private SensorHandler sensorHandler;
    private GamePadHandler gamePadHandler;

    private View mControlsView;
    private boolean mVisible;

    private VideoTextureRenderer rendererR;
    private VideoTextureRenderer rendererL;
    public static final String hostname = "192.168.2.102";
    public static final int port = 26233;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayerL.pause();
        mediaPlayerR.pause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stream);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.linearLayout);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(view -> toggle());

        mPreviewL = (TextureView) findViewById(textureViewL);
        mPreviewL.setSurfaceTextureListener(this);

        mPreviewR = (TextureView) findViewById(textureViewR);
        mPreviewR.setSurfaceTextureListener(this);

        mediaPlayerL = new MediaPlayer();
        mediaPlayerR = new MediaPlayer();

        // ustawienie źródła dla media playerów
        try {
            mediaPlayerL.setDataSource(LEFT_STREAM_IP);
            mediaPlayerR.setDataSource(RIGHT_STREAM_IP);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // uruchomienie AmberClient i inicjalizacja handlerów
        try {
            AmberClient client = new AmberClient(hostname, port);
            sensorHandler = new SensorHandler(client,this);
            gamePadHandler = new GamePadHandler(client, this);
            sensorHandler.run();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(50);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
//            logowanie momentu, w którym oba preview są gotowe do użycia
            System.out.println("### L isAvailable:" + mPreviewL.isAvailable());
            System.out.println("### R isAvailable:" + mPreviewR.isAvailable());

            if (mPreviewR.isAvailable() && mPreviewL.isAvailable()) {
                // ustawienie rendererów dla obu preview
                rendererL = new VideoTextureRenderer(this, mPreviewL.getSurfaceTexture(), metrics.widthPixels / 2, metrics.heightPixels);
                rendererR = new VideoTextureRenderer(this, mPreviewR.getSurfaceTexture(), metrics.widthPixels / 2, metrics.heightPixels);

                while (rendererL.getVideoTexture() == null) ; // :( czekamy w pętli aż VideoTexture będzie dostępne po czym ustawiamy je jako surface do wyświetlania dla mediaplayera
                mediaPlayerL.setSurface(new Surface(rendererL.getVideoTexture()));
                rendererL.setVideoSize(metrics.widthPixels / 2, metrics.heightPixels); //

                while (rendererR.getVideoTexture() == null) ; // :(
                mediaPlayerR.setSurface(new Surface(rendererR.getVideoTexture()));
                rendererR.setVideoSize(metrics.widthPixels / 2, metrics.heightPixels);

                // wywołanie asynchronicznego przygotowania
                mediaPlayerL.prepareAsync();
                mediaPlayerR.prepareAsync();

                // listenery uruchamiające odtwarzanie gdy player gotowy
                mediaPlayerL.setOnPreparedListener(mp -> mediaPlayerL.start());
                mediaPlayerR.setOnPreparedListener(mp -> mediaPlayerR.start());
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    // przy aktualizacji SurfaceTexture wywołujemy metodę przycinającą obraz
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        try {
            updateTextureViewSize(metrics.widthPixels / 2, metrics.heightPixels);
        } catch (ArithmeticException e) {
            //divide by 0 when exiting application
        }
    }

    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        int pivotPointX = viewWidth / 2;
        int pivotPointY = viewHeight / 2;

        Matrix matrix = new Matrix();
        matrix.setScale(2.0f, 1.0f, pivotPointX, pivotPointY);

        mPreviewL.setTransform(matrix);
        mPreviewR.setTransform(matrix);
    }

    // reakcja na zmianę danych z sensorów pozycji
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        gamePadHandler.handleMotionEvent(event);
        return super.onGenericMotionEvent(event);
    }

    // reakcja na użycie pada
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        processKeyEvent(keyCode, event);
        return super.onKeyDown(keyCode, event);
    }

    // metoda kalibrująca czujnik położenia po wciśnięciu, któregoś z przycisków akcji
    private void processKeyEvent(int keyCode, KeyEvent event) {
        if ((event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A) || (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B) || (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_X) || (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y)) {
            sensorHandler.calibrate();
        }
    }
}
