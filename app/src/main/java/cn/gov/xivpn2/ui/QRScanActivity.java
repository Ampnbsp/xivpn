package cn.gov.xivpn2.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;

import cn.gov.xivpn2.R;
import cn.gov.xivpn2.service.SubscriptionWork;

public class QRScanActivity extends AppCompatActivity {

    private final String TAG = "QRScanActivity";
    private PreviewView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qrscan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // title

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.qrcode_scan);
        }

        previewView = findViewById(R.id.preview_view);

        // request camera permission

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 2);
        } else {
            // start camera
            startCamera();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);

        // camera permission
        if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, R.string.no_camera_permission, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.wtf(TAG, "process camera provider", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        // preview

        Preview preview = new Preview.Builder().build();
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // mlkit barcode scanner

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(getMainExecutor(), imageProxy -> {

            Log.d(TAG, "on analyze " + imageProxy.getWidth() + "x" + imageProxy.getHeight());

            Bitmap bitmap = imageProxy.toBitmap();
            InputImage inputImage = InputImage.fromBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());
            BarcodeScanner client = BarcodeScanning.getClient(options);

            client.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.isEmpty()) return;

                        // qrcode found

                        Barcode barcode = barcodes.get(0);
                        String rawValue = barcode.getRawValue();

                        Log.i(TAG, "qr code found " + rawValue);

                        try {
                            SubscriptionWork.parseLine(rawValue, "none");
                            Toast.makeText(QRScanActivity.this, R.string.proxy_added, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, "parse line", e);

                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.error)
                                    .setMessage(getString(R.string.invalid_link) + "\n\n" + e.getMessage())
                                    .setPositiveButton(R.string.ok, null)
                                    .show();
                        }

                        cameraProvider.unbind(preview);
                        cameraProvider.unbind(imageAnalysis);

                        finish();
                    })
                    .addOnCompleteListener(task -> imageProxy.close());

        });

        // bind to camera provider

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}