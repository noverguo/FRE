package com.nv.fre.api;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.nv.fre.FuckServiceGrpc;
import com.nv.fre.nano.Fre;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.grpc.Channel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;

/**
 * Created by nover on 2016/1/16 0016.
 */
public class GrpcServer {
    private static final String TAG = GrpcServer.class.getSimpleName();
    private static String SERVER_DATA_URL = "https://raw.githubusercontent.com/nvlove/freserver/master/s.dat";
    private static String VERSION_CODE_URL = "https://raw.githubusercontent.com/nvlove/freserver/master/v.dat";
    private static String APK_URL = "https://raw.githubusercontent.com/nvlove/freserver/master/fre.apk";
    private static String ADDRESS = "192.168.1.109";
    private static int PORT = 50051;
    private static Channel sChannel;
    private static Handler sHandler = new Handler(Looper.getMainLooper());
    private static Runnable sDelayChecker = new Runnable() {
        @Override
        public void run() {
            synchronized (GrpcServer.class) {
                sChannel = null;
            }
        }
    };
    private synchronized static Channel getChannel() {
        if(sChannel == null) {
            sChannel = OkHttpChannelBuilder.forAddress(ADDRESS, PORT).usePlaintext(true).build();
        }
        sHandler.removeCallbacks(sDelayChecker);
        sHandler.postDelayed(sDelayChecker, 10000);
        return sChannel;
    }
    public static boolean init() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(SERVER_DATA_URL + "?t=" + System.currentTimeMillis()).get().cacheControl(CacheControl.FORCE_NETWORK).build();
        try {
            Response response = client.newCall(request).execute();
            String values = response.body().string();
            if (TextUtils.isEmpty(values)) {
                return false;
            }
            String[] arr = values.split(":");
            if(arr == null || arr.length < 1) {
                Log.i(TAG, "init server error size: " + arr[0]);
                return false;
            }
            if(!Pattern.matches("^(\\d+\\.){3}\\d+$", arr[0])) {
                Log.i(TAG, "init server error match: " + arr[0]);
                return false;
            }
            ADDRESS = arr[0];
            if(arr.length == 2) {
                try {
                    int port = Integer.parseInt(arr[1].trim());
                    if(port > 65535 || port < 1) {
                        Log.i(TAG, "init server error port: " + port);
                        return false;
                    }
                    PORT = port;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Log.i(TAG, "init server error: " + e.getMessage());
                    return false;
                }
            }
            Log.i(TAG, "init server ip: " + ADDRESS + ", port: " + PORT);
            return true;
        } catch (IOException e) {
            Log.i(TAG, "init server error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static String checkUpdate(int versionCode) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(VERSION_CODE_URL + "?t=" + System.currentTimeMillis()).get().cacheControl(CacheControl.FORCE_NETWORK).build();
        try {
            Response response = client.newCall(request).execute();
            String value = response.body().string().trim();
            if(versionCode < Integer.parseInt(value)) {
                return APK_URL;
            }
        } catch (IOException e) {
        } catch (NumberFormatException e) {
        }
        return null;
    }

    public static void fuckMM(Fre.FuckRequest req, StreamObserver<Fre.FuckReply> replyCallback) {
        FuckServiceGrpc.newStub(getChannel()).withDeadlineAfter(10, TimeUnit.SECONDS).fuckMM(req, replyCallback);
    }

    public static void upload(Fre.UploadRequest req, StreamObserver<Fre.EmptyReply> replyCallback) {
        FuckServiceGrpc.newStub(getChannel()).withDeadlineAfter(10, TimeUnit.SECONDS).upload(req, replyCallback);
    }
}
