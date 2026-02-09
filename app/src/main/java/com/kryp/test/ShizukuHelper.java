package com.kryp.test;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import moe.shizuku.api.Shizuku;
import moe.shizuku.api.ShizukuBinderWrapper;

/**
 * Shizuku 辅助类
 * 用于执行系统级操作（点击、截图等）
 */
public class ShizukuHelper {
    private static final String TAG = "ShizukuHelper";
    private static final int REQUEST_CODE_SHIZUKU = 1000;

    private Context context;
    private OnAuthChangeListener authChangeListener;

    public interface OnAuthChangeListener {
        void onAuthGranted();
        void onAuthDenied();
    }

    public ShizukuHelper(Context context) {
        this.context = context;
        Shizuku.addBinderReceivedListenerSticky(() -> {
            checkPermission();
        });
        Shizuku.addBinderDeadListener(() -> {
            if (authChangeListener != null) {
                authChangeListener.onAuthDenied();
            }
        });
    }

    /**
     * 检查 Shizuku 权限状态
     */
    public boolean checkPermission() {
        int result = Shizuku.checkSelfPermission();
        if (result == PackageManager.PERMISSION_GRANTED) {
            if (authChangeListener != null) {
                authChangeListener.onAuthGranted();
            }
            return true;
        } else {
            if (authChangeListener != null) {
                authChangeListener.onAuthDenied();
            }
            return false;
        }
    }

    /**
     * 请求 Shizuku 权限
     */
    public void requestPermission() {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU);
        }
    }

    /**
     * 设置授权状态监听器
     */
    public void setOnAuthChangeListener(OnAuthChangeListener listener) {
        this.authChangeListener = listener;
    }

    /**
     * 执行 Shell 命令并返回输出
     */
    public static String exec(String command) {
        try {
            ShizukuBinderWrapper binderWrapper = new ShizukuBinderWrapper(Shizuku.getBinder());
            return binderWrapper.exec(command);
        } catch (RemoteException e) {
            Log.e(TAG, "执行命令失败: " + command, e);
            return null;
        }
    }

    /**
     * 模拟点击
     */
    public static boolean tap(int x, int y) {
        String cmd = String.format("input tap %d %d", x, y);
        String result = exec(cmd);
        return result != null;
    }

    /**
     * 获取屏幕尺寸
     */
    public static int[] getScreenSize() {
        String result = exec("wm size");
        if (result != null) {
            // 格式: Physical size: 1080x2400
            String[] parts = result.split(" ");
            if (parts.length >= 3) {
                String size = parts[2];
                String[] dimensions = size.split("x");
                if (dimensions.length == 2) {
                    try {
                        int width = Integer.parseInt(dimensions[0]);
                        int height = Integer.parseInt(dimensions[1]);
                        return new int[]{width, height};
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "解析屏幕尺寸失败", e);
                    }
                }
            }
        }
        // 默认返回 1080x2400
        return new int[]{1080, 2400};
    }

    /**
     * 开启无障碍服务
     */
    public static boolean enableAccessibilityService(String serviceName) {
        try {
            // 获取当前已启用的无障碍服务列表
            String currentServices = exec("settings get secure enabled_accessibility_services");
            
            // 检查是否已包含本服务
            if (currentServices != null && currentServices.contains(serviceName)) {
                return true;
            }
            
            // 添加本应用的无障碍服务
            String newServices;
            if (currentServices == null || currentServices.isEmpty() || currentServices.equals("null")) {
                newServices = serviceName;
            } else {
                newServices = currentServices + ":" + serviceName;
            }
            
            // 设置新的无障碍服务列表
            String result = exec("settings put secure enabled_accessibility_services \"" + newServices + "\"");
            return result != null;
        } catch (Exception e) {
            Log.e(TAG, "开启无障碍服务失败", e);
            return false;
        }
    }

    /**
     * 关闭无障碍服务
     */
    public static boolean disableAccessibilityService(String serviceName) {
        try {
            String currentServices = exec("settings get secure enabled_accessibility_services");
            if (currentServices == null || !currentServices.contains(serviceName)) {
                return true;
            }
            
            // 移除本应用的无障碍服务
            String newServices = currentServices.replace(":" + serviceName, "");
            newServices = newServices.replace(serviceName, "");
            
            String result = exec("settings put secure enabled_accessibility_services \"" + newServices + "\"");
            return result != null;
        } catch (Exception e) {
            Log.e(TAG, "关闭无障碍服务失败", e);
            return false;
        }
    }

    /**
     * 创建临时目录
     */
    public static boolean createTempDir() {
        String result = exec("mkdir -p /sdcard/tmp");
        return result != null;
    }
}
