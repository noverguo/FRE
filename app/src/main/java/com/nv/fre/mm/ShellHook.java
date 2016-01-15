package com.nv.fre.mm;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by noverguo on 2016/1/15.
 */
public class ShellHook {
    private static ShellHook shellHook;
    private HookInfo hi;
    List<MM_MethodHook> hookMethodList = new ArrayList<>();
    ShellHook(HookInfo hi) {
        this.hi = hi;
        hookExec();
    }

    private void hookExec() {
//       Process java.lang.ProcessManager.exec(String[] taintedCommand, String[] taintedEnvironment, File workingDirectory, boolean redirectErrorStream)
        XposedHelpers.findAndHookMethod("java.lang.ProcessManager", hi.classLoader, "exec", String[].class, String[].class, File.class, boolean.class, new MM_MethodHook() {
            @Override
            public void MM_beforeHookedMethod(MethodHookParam param) throws Throwable {
                for(MM_MethodHook xcMethodHook : hookMethodList) {
                    xcMethodHook.MM_beforeHookedMethod(param);
                }
            }

            @Override
            public void MM_call(Param param) throws Throwable {
                for(MM_MethodHook xcMethodHook : hookMethodList) {
                    xcMethodHook.MM_call(param);
                }
            }

            @Override
            public void MM_afterHookedMethod(MethodHookParam param) throws Throwable {
                for(MM_MethodHook xcMethodHook : hookMethodList) {
                    xcMethodHook.MM_afterHookedMethod(param);
                }
            }
        });
    }

    /**
     * Process java.lang.ProcessManager.exec(String[] taintedCommand, String[] taintedEnvironment, File workingDirectory, boolean redirectErrorStream)
     * @param hi
     */
    public static void hookExec(HookInfo hi, MM_MethodHook mmMethodHook) {
        if(shellHook == null) {
            shellHook = new ShellHook(hi);
        }
        shellHook.hookMethodList.add(mmMethodHook);
    }

    public static class ProcessWrapper extends Process {
        private Process process;
        public ProcessWrapper(Process process) {
            this.process = process;
        }

        @Override
        public void destroy() {
            process.destroy();
        }

        @Override
        public int exitValue() {
            return process.exitValue();
        }

        @Override
        public InputStream getErrorStream() {
            return process.getErrorStream();
        }

        @Override
        public InputStream getInputStream() {
            return process.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() {
            return process.getOutputStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }
    }
}
