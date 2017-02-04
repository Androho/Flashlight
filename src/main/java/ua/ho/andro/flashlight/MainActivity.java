package ua.ho.andro.flashlight;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, SoundPool.OnLoadCompleteListener {

    private int sound;
    private SoundPool soundPool;
    private Camera camera;
    private Parameters parameters;
    private Switch mySwitch;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private InterstitialAd mInterstitialAd;
    private AdView mAdView;
    private List supportedFlashModes;
    private boolean previewIsRunning;
    private int countStarApp = 1;
    private SharedPreferences preferences;
    public static final String APP_PREFERENCES = "mysettings";
    public static final String APP_PREFERENCES_COUNTER = "counter";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mAdView = (AdView) findViewById(R.id.adView);
        mySwitch = (Switch) findViewById(R.id.my_switch);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        //Couner
        preferences=getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        if (countStarApp>10 & countStarApp<20){
            // GooglePlay reit
            new AlertDialog.Builder(this)
                    .setTitle(R.string.propouse_title)
                    .setMessage(R.string.error_text)
                    .setPositiveButton(R.string.exit_message, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        }
        //

//AdMob Interstitial
        MobileAds.initialize(getApplicationContext(), getString(R.string.app_ad_unit_id));
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                finish();
            }
        });

//End of AdMob Interstitial

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createSoundPoolWithBuilder();
        } else {
            createSoundPoolWithConstructor();
        }

        soundPool.setOnLoadCompleteListener(this);
        sound = soundPool.load(this, R.raw.click, 1);

        mySwitch.setChecked(true);
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    setFlashLigthOn();
                } else {
                    setFlashLightOff();
                }
            }
        });

        boolean isCameraFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!isCameraFlash) {
            showCameraAlert();
        } else {
            camera = Camera.open();
        }
    }

    private void showCameraAlert() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_title)
                .setMessage(R.string.error_text)
                .setPositiveButton(R.string.exit_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void createSoundPoolWithBuilder() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(1).build();
    }

    @SuppressWarnings("deprecation")
    protected void createSoundPoolWithConstructor() {
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    }

    private void setFlashLigthOn() {
        soundPool.play(sound, 1, 1, 0, 0, 1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    parameters = camera.getParameters();
                    if (parameters != null) {
                        supportedFlashModes = parameters.getSupportedFlashModes();
                        if (supportedFlashModes.contains(Parameters.FLASH_MODE_TORCH)) {
                            parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                            camera.setParameters(parameters);
                        } else if (supportedFlashModes.contains(Parameters.FLASH_MODE_ON)) {
                            parameters.setFlashMode(Parameters.FLASH_MODE_ON);
                            camera.setParameters(parameters);
                        } else camera = null;
                    }
                }
            }
        }).start();
    }

    private void setFlashLightOff() {
        soundPool.play(sound, 1, 1, 0, 0, 1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                }
            }
        }).start();

    }

    private void releaseCamera() {
        if (previewIsRunning && (camera != null)) {
            camera.stopPreview();
            camera.release();
            camera = null;
            previewIsRunning = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        mySwitch.setChecked(false);
        if (mAdView != null) {
            mAdView.pause();
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(APP_PREFERENCES_COUNTER, countStarApp);
        editor.apply();
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera == null) {
            camera = Camera.open();
        } else {
            setFlashLigthOn();
        }
        mySwitch.setChecked(true);
        if (mAdView != null) {
            mAdView.resume();
        }
        if (!mInterstitialAd.isLoaded()) {
            requestNewInterstitial();
        }
        if (preferences.contains(APP_PREFERENCES_COUNTER)) {
            // Получаем число из настроек
            countStarApp = preferences.getInt(APP_PREFERENCES_COUNTER, 0);
            // Выводим на экран данные из настроек
            countStarApp=countStarApp+1;
            int i=countStarApp;
        }
    }

    @Override
    public void onBackPressed() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            super.finish();
        }
        super.finish();
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int i, int i1) {
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mInterstitialAd.loadAd(adRequest);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!previewIsRunning && (camera != null)) {
            camera.startPreview();
            previewIsRunning = true;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

}