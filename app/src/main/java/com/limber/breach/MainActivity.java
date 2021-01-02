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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.limber.breach.fragments.CaptureFragmentDirections;
import com.limber.breach.utils.SoundPlayer;
import com.limber.breach.utils.Vibrator;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    /**
     * Preferences name
     */
    private static final String kPREFS_NAME = "BreachPrefs";

    /**
     * Vibration setting preference key
     */
    private static final String kPREFS_KEY_VIBRATION_ENABLED = "VibrationEnabled";

    /**
     * Sound setting preference key
     */
    private static final String kPREFS_KEY_SOUND_ENABLED = "SoundEnabled";

    /**
     * Permissions we need
     */
    private static final String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.VIBRATE
    };

    /**
     * Preferences handle
     */
    private SharedPreferences mPrefs;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Check/un-check menu options
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
            loadSettings();

            if (item.getItemId() == R.id.menuToggleSound) {
                SoundPlayer.get().play(this, SoundPlayer.Effect.success);
            } else {
                Vibrator.get().play(Vibrator.Effect.success);
            }

            return true;
        } else if (item.getItemId() == R.id.menuLoadExample) {
            // Load example scan
            NavGraphDirections.ActionGlobalCaptureFragment action = CaptureFragmentDirections.actionGlobalCaptureFragment()
                    .setBitmap(BitmapFactory.decodeResource(getResources(),
                            R.drawable.test_5x5_3_01));

            Navigation.findNavController(this, R.id.fragment).navigate(action);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    /**
     * Configure components based on loaded settings
     */
    private void loadSettings() {
        SoundPlayer.get().setEnabled(mPrefs.getBoolean(kPREFS_KEY_SOUND_ENABLED, true));
        Vibrator.get().setEnabled(mPrefs.getBoolean(kPREFS_KEY_VIBRATION_ENABLED, true));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Force dark mode, since that's what the UI was built around
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        mPrefs = getSharedPreferences(kPREFS_NAME, Context.MODE_PRIVATE);

        SoundPlayer.create();
        Vibrator.create(this);

        loadSettings();

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

    /**
     * Start the application
     */
    private void start() {
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

    /**
     * Check if we have all the permissions we need, otherwise make the request
     */
    private void checkPermissions() {
        if (!hasPermissions(this, PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS,
                        REQUEST_CODE_PERMISSIONS);
            }
        }

    }

    /**
     * Check if we have all the permissions
     */
    private boolean allPermissionsGranted() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if we have all the permissions needed
     */
    private static boolean hasPermissions(Context context, String... permissions) {
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
