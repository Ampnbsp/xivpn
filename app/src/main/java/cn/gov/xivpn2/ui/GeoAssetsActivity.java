package cn.gov.xivpn2.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import cn.gov.xivpn2.R;
import cn.gov.xivpn2.service.GeoDownloaderWork;

public class GeoAssetsActivity extends AppCompatActivity {


    private final String TAG = "GeoAssetsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BlackBackground.apply(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_geo_assets);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        MaterialButton btnGeoip = findViewById(R.id.geoip_download);
        btnGeoip.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.download)
                    .setItems(R.array.geoip, (dialog, which) -> {
                        String url = "";
                        switch (which) {
                            case 0:
                                url = "https://github.com/v2fly/geoip/releases/latest/download/geoip.dat";
                                break;
                            case 1:
                                url = "https://github.com/v2fly/geoip/releases/latest/download/geoip-only-cn-private.dat";
                                break;
                            case 2:
                                url = "https://github.com/Exclude0122/geoip/releases/latest/download/geoip.dat";
                                break;
                            case 3:
                                url = "https://github.com/Exclude0122/geoip/releases/latest/download/geoip-only-cn-private.dat";
                                break;
                            case 4:
                                url = "https://github.com/Exclude0122/geoip/releases/latest/download/geoip-only-ru-private.dat";
                                break;
                            case 5:
                                url = "https://github.com/Exclude0122/geoip/releases/latest/download/geoip-only-ir-private.dat";
                                break;
                            case 6:
                                url = "https://github.com/Exclude0122/geoip/releases/latest/download/geoip-only-kp-private.dat";
                                break;
                            case 7:
                                url = "https://github.com/runetfreedom/russia-v2ray-rules-dat/releases/latest/download/geoip.dat";
                                break;
                        }
                        startDownload(url, "geoip.dat");
                    })
                    .show();
        });

        MaterialButton btnGeosite = findViewById(R.id.geosite_download);
        btnGeosite.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.download)
                    .setItems(R.array.geosite, (dialog, which) -> {
                        String url = "";
                        switch (which) {
                            case 0:
                                url = "https://github.com/v2fly/domain-list-community/releases/latest/download/dlc.dat";
                                break;
                            case 1:
                                url = "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat";
                                break;
                            case 2:
                                url = "https://github.com/Chocolate4U/Iran-v2ray-rules/releases/latest/download/geosite.dat";
                                break;
                            case 3:
                                url = "https://github.com/Chocolate4U/Iran-v2ray-rules/releases/latest/download/geosite-lite.dat";
                                break;
                            case 4:
                                url = "https://github.com/runetfreedom/russia-v2ray-rules-dat/releases/latest/download/geosite.dat";
                                break;
                        }
                        startDownload(url, "geosite.dat");
                    })
                    .show();
        });

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) supportActionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setMargin(View view) {
        int marginInDp = 16;
        int marginInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, marginInDp, getResources().getDisplayMetrics());

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
        view.setLayoutParams(params);
    }

    private void startDownload(String url, String file) {
        try {
            WorkManager workManager = WorkManager.getInstance(this);
            List<WorkInfo> works = workManager.getWorkInfosByTag("geoassets").get();

            boolean running = false;
            for (WorkInfo work : works) {
                if (work.getState() == WorkInfo.State.RUNNING || work.getState() == WorkInfo.State.ENQUEUED) {
                    running = true;
                    break;
                }
            }

            if (running) {
                Toast.makeText(this, R.string.already_updating_geo_assets, Toast.LENGTH_SHORT).show();
                return;
            }


            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(GeoDownloaderWork.class)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .addTag("geoassets")
                    .setInputData(new Data.Builder().putString("URL", url).putString("PATH", new File(getFilesDir(), file).getAbsolutePath()).build())
                    .build();

            LinearProgressIndicator progressBar = new LinearProgressIndicator(this);
            progressBar.setIndeterminate(true);
            FrameLayout frameLayout = new FrameLayout(this);
            frameLayout.addView(progressBar);
            setMargin(progressBar);

            AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.downloading)
                    .setView(frameLayout)
                    .setCancelable(false)
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        workManager.getWorkInfoById(workRequest.getId()).cancel(true);
                    })
                    .create();

            alertDialog.show();


            // start work

            Operation operation = workManager.enqueue(workRequest);
            operation.getResult().addListener(() -> {
                UUID workId = workRequest.getId();
                workManager.getWorkInfoByIdLiveData(workId)
                        .observe(this, workInfo -> {
                            WorkInfo.State state = workInfo.getState();
                            if (state == WorkInfo.State.RUNNING) {

                                Data progressData = workInfo.getProgress();
                                int progress = progressData.getInt("progress", 0);
                                boolean indeterminate = progressData.getBoolean("indeterminate", true);

                                Log.d(TAG, "progress " + progress + " " + indeterminate);

                                if (indeterminate) {
                                    progressBar.setIndeterminate(true);
                                } else {
                                    progressBar.setIndeterminate(false);
                                    progressBar.setProgress(progress);
                                    progressBar.setMax(100);
                                }

                            } else if (state == WorkInfo.State.FAILED) {
                                Toast.makeText(this, R.string.downloading_error, Toast.LENGTH_SHORT).show();
                                alertDialog.dismiss();
                                refresh();
                            } else if (state == WorkInfo.State.SUCCEEDED) {
                                Toast.makeText(this, R.string.download_success, Toast.LENGTH_SHORT).show();
                                alertDialog.dismiss();
                                refresh();
                            }
                        });
            }, getMainExecutor());

        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "enqueue work", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        refresh();
    }

    private String prettyPrintLastModified(long lastModified) {
        if (lastModified <= 0) return "Unknown";
        if (lastModified > System.currentTimeMillis()) return "Future";

        long duration = System.currentTimeMillis() - lastModified;

        if (duration < 1000) return "Just now";
        if (duration < 60 * 1000) return duration / 1000 + " seconds ago";
        if (duration < 60 * 60 * 1000) return duration / 60 / 1000 + " minutes ago";
        if (duration < 24 * 60 * 60 * 1000) return duration / 60 / 60 / 1000 + " hours ago";
        return duration / 24 / 60 / 60 / 1000 + " days ago";
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void refresh() {
        TextView geoipSize = findViewById(R.id.geoip_size);
        TextView geoipDate = findViewById(R.id.geoip_date);
        TextView geositeSize = findViewById(R.id.geosite_size);
        TextView geositeDate = findViewById(R.id.geosite_date);

        File filesDir = getFilesDir();
        File geoip = new File(filesDir, "geoip.dat");
        File geosite = new File(filesDir, "geosite.dat");

        if (geoip.exists()) {
            geoipSize.setText((String.format("%.2f MB", ((float) geoip.length()) / 1024.0 / 1024.0)));
            geoipDate.setText(prettyPrintLastModified(geoip.lastModified()));
        } else {
            geoipSize.setText(R.string.not_found);
            geoipDate.setText("");
        }

        if (geosite.exists()) {
            geositeSize.setText((String.format("%.2f MB", ((float) geosite.length()) / 1024.0 / 1024.0)));
            geositeDate.setText(prettyPrintLastModified(geosite.lastModified()));
        } else {
            geositeSize.setText(R.string.not_found);
            geositeDate.setText("");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.geo_assets_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }


}