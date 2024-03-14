package com.example.execamarax;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import android.widget.Toast;


public class CamaraX extends AppCompatActivity {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;
    private ImageCapture imageCapture;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camaralayout);

        Button takePhotoButton = findViewById(R.id.takePhoto);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        textView = findViewById(R.id.orientation);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (imageCapture != null) {
                    File photoFile = createPhotoFile();
                    ImageCapture.OutputFileOptions outputFileOptions =
                            new ImageCapture.OutputFileOptions.Builder(photoFile).build();

                    imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(CamaraX.this),
                            new ImageCapture.OnImageSavedCallback() {
                                @Override
                                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            rotateImage90Degrees(photoFile);

                                            Toast.makeText(CamaraX.this, "Picture taken saved at " + photoFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }

                                @Override
                                public void onError(@NonNull ImageCaptureException exception) {
                                    exception.printStackTrace();
                                }
                            });
                }
            }
        });
    }



    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                image.close();
            }
        });

        // Obtener la orientación actual del dispositivo

        // Inicializar ImageCapture aquí
        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(Surface.ROTATION_90)
                .build();

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Asignar el ImageCapture al lifecycle de la actividad
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview, imageCapture);
    }
    private void rotateImage90Degrees(File imageFile) {
        try {
            // Decodificar el archivo de imagen en un objeto Bitmap
            Bitmap originalBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

            // Crear una matriz de transformación para rotar la imagen 90 grados
            Matrix matrix = new Matrix();
            matrix.postRotate(90);

            // Aplicar la transformación a la imagen original
            Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0,
                    originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);

            // Guardar la imagen rotada en el mismo archivo
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private File createPhotoFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis());
        String fileName = "IMG_" + timeStamp + ".jpg";
        File storageDir = getExternalFilesDir(null);
        return new File(storageDir, fileName);
    }
}