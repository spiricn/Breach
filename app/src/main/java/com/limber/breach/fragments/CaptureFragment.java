package com.limber.breach.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
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

public class CaptureFragment extends Fragment {
    public CaptureFragment() {
        super(R.layout.fragment_capture);
    }

    Button mButton;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mButton = view.findViewById(R.id.camera_capture_button);
        mButton.setEnabled(false);

        mButton.setOnClickListener(view1 -> capture());

        initialize();
    }

    private void capture() {
        mButton.setEnabled(false);
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

                        super.onCaptureSuccess(image);

                        onCaptured(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                    }
                }
        );
    }

    void onCaptured(Bitmap bitmap) {
        Analyzer.analyze(bitmap, result -> {
            SoundPlayer.get().play(SoundPlayer.Effect.success);
            Vibrator.get().play(Vibrator.Effect.success);
            NavDirections action = CaptureFragmentDirections.actionCaptureFragmentToFragmentVerify(result);
            Navigation.findNavController(requireView()).navigate(action);
        }, error -> {
            SoundPlayer.get().play(SoundPlayer.Effect.error);
            Vibrator.get().play(Vibrator.Effect.error);

            mSnackbar = Snackbar.make(requireView(),
                    R.string.tryAgainError, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.cyberpunk_red))
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
            mSnackbar.show();

            mButton.setEnabled(true);
        });
    }

    Snackbar mSnackbar = null;


    void initialize() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity());

        cameraProviderFuture.addListener(() -> {
            ProcessCameraProvider cameraProvider;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                // TODO
                e.printStackTrace();
                return;
            }

            Preview preview = new Preview.Builder().build();

            PreviewView viewFinder = requireView().findViewById(R.id.viewFinder);
            preview.setSurfaceProvider(viewFinder.createSurfaceProvider());

            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            mImageCapture = new ImageCapture.Builder()
                    .build();
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, mImageCapture);

            mButton.setEnabled(true);

        }, ContextCompat.getMainExecutor(requireActivity()));
    }

    private ImageCapture mImageCapture;
}
