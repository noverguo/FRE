package com.nv.fre.mm;

import com.nv.fre.api.GrpcServer;
import com.nv.fre.mm.itf.Checker;
import com.nv.fre.utils.ConnectedHelper;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by noverguo on 2016/1/29.
 */
public class MMChecker implements Checker {
    MMContext hi;
    Callback callback;
    public void init(MMContext hookInfo, Callback callback) {
        this.hi = hookInfo;
        this.callback = callback;
        XposedHelpers.findAndHookMethod("com.tencent.mm.ui.LauncherUI", hi.classLoader, "onResume", new MM_MethodHook() {
            @Override
            public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
//                XposedBridge.log("allow: " + hi.allow + ", checking: " + checking + ", needCheck: " + needCheck);
                if(!hi.allow && !isChecking()) {
                    postCheckAllow();
                }
            }
        });
        postCheckAllow();
    }
    private ConnectedHelper connectedHelper = new ConnectedHelper();
    private void registerCheck() {
        connectedHelper.registerConnectedCheck(hi.context, new ConnectedHelper.Callback() {
            @Override
            public boolean canRun() {
                return needCheck && !checking;
            }

            @Override
            public void onConnected() {
                if(canRun()) {
                    postCheckAllow();
                }
            }
        });
    }

    private void unregisterCheck() {
        connectedHelper.unregisterConnectedCheck(hi.context);
    }

    private boolean needCheck = false;
    private boolean checking = false;
    private Runnable checkAllowRunnable = new Runnable() {
        @Override
        public void run() {
            check();
        }
    };
    public void postCheckAllow() {
        checking = false;
        needCheck = false;
        unregisterCheck();
        hi.bgHandler.removeCallbacks(checkAllowRunnable);
        hi.bgHandler.postDelayed(checkAllowRunnable, 8000);
    }
    public boolean isChecking() {
        return checking;
    }

    private void check() {
        //XposedBridge.log("fuckMM check");
        checking = true;
        GrpcServer.initHostAndPort();
//        XposedBridge.log("fuckMM check start");
        callback.run(this);
    }

    public void finish() {
        checking = false;
        // 24小时后重新检测，防止非法使用
        hi.bgHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GrpcServer.sInit = false;
                needCheck = true;
                registerCheck();
            }
        }, 1000 * 60 * 60 * 24);
    }

    public void error() {
        needCheck = true;
        checking = false;
        registerCheck();
    }

    public static interface Callback {
        void run(Checker checker);
    }

}
