package com.limber.breach;

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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class CaptureFragment extends Fragment {
    static public class BitmapCaptureModel extends ViewModel {
        private final MutableLiveData<Bitmap> mCapturedBitmap = new MutableLiveData<>();

        public BitmapCaptureModel() {
            super();
        }

        public void capture(Bitmap bitmap) {
            mCapturedBitmap.setValue(bitmap);
        }

        public LiveData<Bitmap> getCapturedBitmap() {
            return mCapturedBitmap;
        }
    }

    public CaptureFragment() {
        super(R.layout.fragment_capture);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mModel = new ViewModelProvider((requireActivity())).get(BitmapCaptureModel.class);

        initialize();

        ((Button) view.findViewById(R.id.camera_capture_button)).setOnClickListener((View.OnClickListener) view1 -> {
            capture();
        });
    }

    private void capture() {
        mImageCapture.takePicture(
                ContextCompat.getMainExecutor(getActivity()),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
                        byte[] bytes = new byte[planeProxy.getBuffer().remaining()];
                        planeProxy.getBuffer().get(bytes);

                        NavDirections action = CaptureFragmentDirections.actionCaptureFragmentToAnalyzerFragment(
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.length));

                        Navigation.findNavController(getView()).navigate(action);

                        super.onCaptureSuccess(image);
                    }
                }
        );
    }

    void initialize() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getActivity());

        cameraProviderFuture.addListener(() -> {
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException e) {
                // TODO
                e.printStackTrace();
                return;
            } catch (InterruptedException e) {
                // TODO
                e.printStackTrace();
                return;
            }

            Preview preview = new Preview.Builder().build();

            PreviewView viewFinder = getView().findViewById(R.id.viewFinder);
            preview.setSurfaceProvider(viewFinder.createSurfaceProvider());

            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            mImageCapture = new ImageCapture.Builder()
                    .build();
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, mImageCapture);

        }, ContextCompat.getMainExecutor(getActivity()));
    }

    private ImageCapture mImageCapture;
    private BitmapCaptureModel mModel;
}
