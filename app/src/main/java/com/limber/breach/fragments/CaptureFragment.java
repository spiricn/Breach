package com.limber.breach.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.limber.breach.Vibrator;
import com.limber.breach.analyzer.Analyzer;
import com.limber.breach.R;
import com.limber.breach.SoundPlayer;

import java.util.concurrent.ExecutionException;

/**
 * Captures a bitmap from camera and analyzes it
 */
public class CaptureFragment extends Fragment {

    /**
     * Camera capture button
     */
    private Button mCaptureButton;

    /**
     * Life cycle aware handler, used to execute all callbacks
     */
    private Handler mHandler;

    /**
     * Used to show notifications to the user
     */
    private Snackbar mSnackbar = null;

    /**
     * Camera capture interface
     */
    private ImageCapture mImageCapture;

    public CaptureFragment() {
        super(R.layout.fragment_capture);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mCaptureButton = view.findViewById(R.id.camera_capture_button);
        mCaptureButton.setEnabled(false);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void dispatchMessage(@NonNull Message msg) {
                if (getView() == null) {
                    // Ignore all callbacks if fragment was destroyed
                    return;
                }

                super.dispatchMessage(msg);
            }
        };

        CaptureFragmentArgs args = CaptureFragmentArgs.fromBundle(requireArguments());
        if (args.getBitmap() != null) {
            // No need to capture our own bitmap since we got it from elsewhere
            view.post(() -> analyzeBitmap(args.getBitmap()));
            return;
        }

        // Use the camera to capture bitmap
        mCaptureButton.setOnClickListener(view1 -> captureBitmap());
        initializeCamera();
    }

    @Override
    public void onDestroyView() {
        // Clear cached arguments
        requireArguments().clear();

        // Ignore pending callbacks
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    /**
     * Capture a bitmap from the camera
     */
    private void captureBitmap() {
        mCaptureButton.setEnabled(false);
        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }

        Vibrator.get().play(Vibrator.Effect.ok);

        mImageCapture.takePicture(
                ContextCompat.getMainExecutor(requireActivity()),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
                        byte[] bytes = new byte[planeProxy.getBuffer().remaining()];
                        planeProxy.getBuffer().get(bytes);

                        image.close();
                        super.onCaptureSuccess(image);

                        mHandler.post(() -> analyzeBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length)));

                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        exception.printStackTrace();
                        super.onError(exception);

                        mHandler.post(() -> Snackbar.make(requireView(),
                                R.string.errorTakePicture, Snackbar.LENGTH_LONG)
                                .show());
                    }
                }
        );
    }

    /**
     * Analyze a captured bitmap
     */
    private void analyzeBitmap(Bitmap bitmap) {
        Analyzer.analyze(bitmap, result -> {
            SoundPlayer.get().play(SoundPlayer.Effect.success);
            Vibrator.get().play(Vibrator.Effect.success);
            NavDirections action = CaptureFragmentDirections.actionCaptureFragmentToFragmentVerify(result, VerifyFragment.Mode.matrix);
            Navigation.findNavController(requireView()).navigate(action);
        }, error -> {
            SoundPlayer.get().play(SoundPlayer.Effect.error);
            Vibrator.get().play(Vibrator.Effect.error);

            mSnackbar = Snackbar.make(requireView(),
                    R.string.tryAgainError, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.cyberpunk_red))
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
            mSnackbar.show();

            mCaptureButton.setEnabled(true);
        }, mHandler);
    }

    /**
     * Set up camera & preview
     */
    private void initializeCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireActivity());

        future.addListener(() -> mHandler.post(() -> {
            ProcessCameraProvider cameraProvider;
            try {
                cameraProvider = future.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                return;
            }

            Preview preview = new Preview.Builder().build();

            PreviewView viewFinder = requireView().findViewById(R.id.viewFinder);
            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            mImageCapture = new ImageCapture.Builder()
                    .build();
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, mImageCapture);

            mCaptureButton.setEnabled(true);
        }), ContextCompat.getMainExecutor(requireActivity()));
    }
}
