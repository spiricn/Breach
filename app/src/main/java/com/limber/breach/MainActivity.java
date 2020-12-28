package com.limber.breach;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
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
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.limber.breach.fragments.CaptureFragmentDirections;
import com.limber.breach.fragments.VerifyFragmentDirections;

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
    public boolean onPrepareOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.menuToggleSound) {
                item.setChecked(mPrefs.getBoolean(kPREFS_KEY_SOUND_ENABLED, true));
            } else if (item.getItemId() == R.id.menuToggleVibration) {
                item.setChecked(mPrefs.getBoolean(kPREFS_KEY_VIBRATION_ENABLED, true));
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuToggleSound || item.getItemId() == R.id.menuToggleVibration) {
            // Toggle vibration/sound on or off
            String settingsKey;

            if (item.getItemId() == R.id.menuToggleSound) {
                settingsKey = kPREFS_KEY_SOUND_ENABLED;
            } else {
                settingsKey = kPREFS_KEY_VIBRATION_ENABLED;
            }

            boolean state = !mPrefs.getBoolean(settingsKey, true);
            mPrefs.edit().putBoolean(settingsKey, state).apply();
            updatePrefs();

            if (item.getItemId() == R.id.menuToggleSound) {
                SoundPlayer.get().play(SoundPlayer.Effect.success);
            } else {
                Vibrator.get().play(Vibrator.Effect.success);
            }

            return true;
        } else if (item.getItemId() == R.id.menuLoadExample) {
            NavGraphDirections.ActionGlobalCaptureFragment action = CaptureFragmentDirections.actionGlobalCaptureFragment()
                    .setBitmap(BitmapFactory.decodeResource(getResources(),
                            R.drawable.test_5x5_3_01));

            Navigation.findNavController(this, R.id.fragment).navigate(action);
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
                        R.string.permissionsNotGrantedSnack, Snackbar.LENGTH_LONG)
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
