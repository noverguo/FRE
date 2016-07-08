package com.nv.fre.utils;

import android.media.AudioAttributes;

import com.nv.fre.BuildConfig;

import java.util.Arrays;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by noverguo on 2016/7/8.
 */
public class XposedUtils {
    public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        int size = parameterTypesAndCallback.length - 1;
        Class[] argsClass = new Class[size];
        for (int i=0;i<size;++i) {
            argsClass[i] = (Class) parameterTypesAndCallback[i];
        }
        Class newClazz = ClassUtils.getDeclaredMethodClass(clazz, methodName, argsClass);
        if (newClazz == null) {
            if (BuildConfig.DEBUG) {
                XposedBridge.log("XposedUtils.findAndHookMethod error: " + clazz.getName() + methodName + Arrays.toString(argsClass));
            }
        }
        XposedHelpers.findAndHookMethod(newClazz, methodName, parameterTypesAndCallback);
    }
}
