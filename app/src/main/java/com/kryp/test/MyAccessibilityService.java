package com.kryp.test;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * 无障碍服务
 * 用于辅助悬浮窗操作和获取屏幕信息
 */
public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = "MyAccessibilityService";
    private static MyAccessibilityService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "无障碍服务已创建");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 处理无障碍事件
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "无障碍服务被中断");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "无障碍服务已销毁");
    }

    /**
     * 获取服务实例
     */
    public static MyAccessibilityService getInstance() {
        return instance;
    }

    /**
     * 使用无障碍服务执行点击手势
     * @param x x 坐标
     * @param y y 坐标
     */
    public boolean performClickGesture(int x, int y) {
        if (instance == null) {
            return false;
        }

        try {
            Path path = new Path();
            path.moveTo(x, y);
            
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
            
            GestureDescription gesture = builder.build();
            return instance.dispatchGesture(gesture, null, null);
        } catch (Exception e) {
            Log.e(TAG, "执行点击手势失败", e);
            return false;
        }
    }

    /**
     * 检查服务是否正在运行
     */
    public static boolean isServiceRunning() {
        return instance != null;
    }
}