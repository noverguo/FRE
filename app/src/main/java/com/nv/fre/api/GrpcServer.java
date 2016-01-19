package com.nv.fre.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.nv.fre.FuckServiceGrpc;
import com.nv.fre.nano.Fre;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.TlsVersion;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedBridge;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.internal.GrpcUtil;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;

/**
 * Created by nover on 2016/1/16 0016.
 */
public class GrpcServer {
    private static String SERVER_DATA_URL = "https://raw.githubusercontent.com/nvlove/freserver/master/s.dat";
    private static String VERSION_CODE_URL = "https://raw.githubusercontent.com/nvlove/freserver/master/v.dat";
    private static String APK_URL = "https://raw.githubusercontent.com/nvlove/freserver/master/fre.apk";
    public static final boolean USE_TLS = false;
    public static String HOST = "54.201.110.240";
    public static int PORT = USE_TLS ? 50052 : 50051;
    public static final String CA_PATH = "ca.pem";
    public static final String REPLACE_SERVER_HOST = "foo.test.google.fr";
    public static final int NETWORK_TIMEOUT_SEC = 10;
    public static final int TRANSPORT_TIMEOUT = 20 * 1000;

    private static Context sContext;
    private static ManagedChannel sChannel;
    private static Handler sHandler = new Handler(Looper.getMainLooper());
    private static Runnable closeRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (GrpcServer.class) {
                if(sChannel != null && !sChannel.isShutdown()) {
                    sChannel.shutdown();
                    sChannel = null;
                }
            }
        }
    };
    private static void timeoutClose() {
        sHandler.removeCallbacks(closeRunnable);
        sHandler.postDelayed(closeRunnable, TRANSPORT_TIMEOUT);
    }
    private static ManagedChannel getChannel() {
        synchronized (GrpcServer.class) {
            if (sChannel == null || sChannel.isShutdown()) {
                OkHttpChannelBuilder builder = OkHttpChannelBuilder.forAddress(HOST, PORT);
                if(USE_TLS) {
                    builder.connectionSpec(new ConnectionSpec.Builder(OkHttpChannelBuilder.DEFAULT_CONNECTION_SPEC)
                            .cipherSuites(GrpcTlsUtils.preferredCiphers().toArray(new String[0]))
                            .tlsVersions(ConnectionSpec.MODERN_TLS.tlsVersions().toArray(new TlsVersion[0]))
                            .build())
                            .overrideAuthority(GrpcUtil.authorityFromHostAndPort(REPLACE_SERVER_HOST, PORT));
                    try {
                        builder.sslSocketFactory(GrpcTlsUtils.newSslSocketFactoryForCa(sContext.getAssets().open(CA_PATH)));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    builder.usePlaintext(true);
                }
                sChannel = builder.build();
            }
        }
        timeoutClose();
        return sChannel;
    }
    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }

    public static boolean sInit = false;
    public static synchronized boolean initHostAndPort() {
        if(sInit) {
            return true;
        }
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(8, TimeUnit.SECONDS);
        client.setReadTimeout(8, TimeUnit.SECONDS);
        Request request = new Request.Builder().url(SERVER_DATA_URL + "?t=" + System.currentTimeMillis()).get().cacheControl(CacheControl.FORCE_NETWORK).build();
        try {
            Response response = client.newCall(request).execute();
            String values = response.body().string();
            if (TextUtils.isEmpty(values)) {
                //XposedBridge.log("init server error isEmpty: " + values);
                return false;
            }
            //XposedBridge.log("init server: " + values);
            String[] arr = values.split(":");
            if(arr == null || arr.length < 1) {
                //XposedBridge.log("init server error size: " + arr[0]);
                return false;
            }
            if(!Pattern.matches("^(\\d+\\.){3}\\d+$", arr[0])) {
                //XposedBridge.log("init server error match: " + arr[0]);
                return false;
            }
            HOST = arr[0];
            if(arr.length == 2) {
                try {
                    int port = Integer.parseInt(arr[1].trim());
                    if(port > 65535 || port < 1) {
                        //XposedBridge.log("init server error port: " + port);
                        return false;
                    }
                    PORT = port;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    //XposedBridge.log("init server error: " + e.getMessage());
                    return false;
                }
            }
            sInit = true;
            //XposedBridge.log("init server ip: " + HOST + ", port: " + PORT);
            return true;
        } catch (IOException e) {
            //XposedBridge.log("init server error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static String checkUpdate(int versionCode) {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(8, TimeUnit.SECONDS);
        client.setReadTimeout(8, TimeUnit.SECONDS);
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
        FuckServiceGrpc.newStub(getChannel()).withDeadlineAfter(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS).fuckMM(req, replyCallback);
    }

    public static void upload(Fre.UploadRequest req, StreamObserver<Fre.EmptyReply> replyCallback) {
        FuckServiceGrpc.newStub(getChannel()).withDeadlineAfter(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS).upload(req, replyCallback);
    }
}
