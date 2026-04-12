package cn.gov.xivpn2.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GeoDownloaderWork extends Worker {

    private final static String TAG = "GeoDownloadWork";

    private boolean interuppted = false;

    public GeoDownloaderWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "do work");

        Data inputData = getInputData();
        String url = inputData.getString("URL");
        String path = inputData.getString("PATH");

        if (url == null || path == null || url.isEmpty() || path.isEmpty()) {
            return Result.failure();
        }


        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .build();


        setProgressAsync(new Data.Builder().putInt("progress", 0).putBoolean("indeterminate", true).build());

        try {

            File file = new File(path);
            Log.i(TAG, "download " + url + " => " + file.getAbsolutePath());

            Request request = new Request.Builder().url(url).build();

            Response response = client.newCall(request).execute();

            if (response.code() != 200) {
                response.close();
                throw new IOException("unexpected status code: " + response.code());
            }

            int totalLength = -1;
            String contentLengthHeader = response.header("Content-Length");
            if (contentLengthHeader != null) {
                totalLength = Integer.parseInt(contentLengthHeader);
            }

            OutputStream outputStream = FileUtils.newOutputStream(file, false);
            byte[] buffer = new byte[4096];

            long lastUpdateProgress = 0;
            int length = 0;
            while (!interuppted) {
                InputStream inputStream = response.body().byteStream();
                int n = inputStream.read(buffer);
                if (n <= 0) break;

                length += n;
                if (System.currentTimeMillis() - lastUpdateProgress > 100) {
                    int progress = (int) (Math.floor((double) length / totalLength * 100));
                    setProgressAsync(new Data.Builder().putInt("progress", progress).putBoolean("indeterminate", totalLength == -1).build());
                    lastUpdateProgress = System.currentTimeMillis();
                }

                outputStream.write(buffer, 0, n);
                outputStream.flush();
            }

            Thread.sleep(500);

            setProgressAsync(new Data.Builder().putInt("progress", 100).putBoolean("indeterminate", false).build());

            outputStream.close();

            response.close();

        } catch (IOException e) {
            Log.e(TAG, "download error", e);

            return Result.failure(new Data.Builder().putString("error", e.getClass().getSimpleName() + ": " + e.getMessage()).build());
        } catch (InterruptedException e) {
            // ignored
        }

        return Result.success();
    }

    @Override
    public void onStopped() {
        interuppted = true;
        super.onStopped();
    }
}
