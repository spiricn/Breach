package com.limber.breach;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    private static final String kPREFS_NAME = "BreachPrefs";
    private static final String kPREFS_KEY_VIBRATION_ENABLED = "VibrationEnabled";
    private static final String kPREFS_KEY_SOUND_ENABLED = "SoundEnabled";

    private static final String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.VIBRATE
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuToggleSound) {
            mPrefs.edit().putBoolean(kPREFS_KEY_SOUND_ENABLED, !mPrefs.getBoolean(kPREFS_KEY_SOUND_ENABLED, true)).apply();
            updatePrefs();
            return true;
        } else if (item.getItemId() == R.id.menuToggleVibration) {
            mPrefs.edit().putBoolean(kPREFS_KEY_VIBRATION_ENABLED, !mPrefs.getBoolean(kPREFS_KEY_VIBRATION_ENABLED, true)).apply();
            updatePrefs();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void updatePrefs() {
        SoundPlayer.get().setEnabled(mPrefs.getBoolean(kPREFS_KEY_SOUND_ENABLED, true));
        Vibrator.get().setEnabled(mPrefs.getBoolean(kPREFS_KEY_VIBRATION_ENABLED, true));
    }

    SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getSharedPreferences(kPREFS_NAME, Context.MODE_PRIVATE);

        SoundPlayer.create(this);
        Vibrator.create(this);

        updatePrefs();

        // Make application fullscreen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Hide the status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        if (allPermissionsGranted()) {
            start();
            return;
        }

        checkPermissions();
    }

    void start() {
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Snackbar.make(getWindow().getDecorView().getRootView(),
                        "Permissions not granted by user..", Snackbar.LENGTH_LONG)
                        .show();
                finish();
            } else {
                start();
            }
        }
    }

    void checkPermissions() {
        if (!hasPermissions(this, PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS,
                        REQUEST_CODE_PERMISSIONS);
            }
        }

    }

    boolean allPermissionsGranted() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;


    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

}
