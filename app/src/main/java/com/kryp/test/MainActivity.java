package com.kryp.test;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * 主 Activity
 * 负责配置参数、管理 Shizuku 授权、启动自动点击功能
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    
    // SharedPreferences 键名
    private static final String PREF_NAME = "ClickerPrefs";
    private static final String KEY_SCREENSHOT_MODE = "screenshot_mode";
    private static final String KEY_DETECTION_INTERVAL = "detection_interval";
    private static final String KEY_CLICK_INTERVAL = "click_interval";
    private static final String KEY_TARGET_X = "target_x";
    private static final String KEY_TARGET_Y = "target_y";
    
    // 默认值
    private static final float DEFAULT_DETECTION_INTERVAL = 1.0f;
    private static final int DEFAULT_CLICK_INTERVAL = 500;
    private static final int DEFAULT_SCREENSHOT_MODE = 0; // 0=FILE, 1=PIPE
    
    // UI 控件
    private LinearLayout authBar;
    private TextView tvAuthStatus;
    private TextView tvAuthCheck;
    private RadioGroup rgScreenshotMode;
    private RadioButton rbFileMode;
    private RadioButton rbPipeMode;
    private EditText etDetectionInterval;
    private EditText etClickInterval;
    private Button btnSaveSettings;
    private Button btnShowFloating;
    private Button btnHideFloating;
    private TextView tvCurrentSettings;
    
    // 辅助类
    private ShizukuHelper shizukuHelper;
    private OcrHelper ocrHelper;
    
    // 配置参数
    private int screenshotMode = DEFAULT_SCREENSHOT_MODE;
    private float detectionInterval = DEFAULT_DETECTION_INTERVAL;
    private int clickInterval = DEFAULT_CLICK_INTERVAL;
    private int targetX = -1;
    private int targetY = -1;
    
    // 运行状态
    private boolean isRunning = false;
    private Handler handler;
    private Runnable clickRunnable;
    
    // 悬浮窗服务
    private Intent floatingWindowServiceIntent;
    private FloatingWindowService floatingWindowService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        handler = new Handler(Looper.getMainLooper());
        initViews();
        loadSettings();
        initShizuku();
        initOcrHelper();
        
        // 创建临时目录
        ShizukuHelper.createTempDir();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoClick();
        if (ocrHelper != null) {
            ocrHelper.close();
        }
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        authBar = findViewById(R.id.auth_bar);
        tvAuthStatus = findViewById(R.id.tv_auth_status);
        tvAuthCheck = findViewById(R.id.tv_auth_check);
        rgScreenshotMode = findViewById(R.id.rg_screenshot_mode);
        rbFileMode = findViewById(R.id.rb_file_mode);
        rbPipeMode = findViewById(R.id.rb_pipe_mode);
        etDetectionInterval = findViewById(R.id.et_detection_interval);
        etClickInterval = findViewById(R.id.et_click_interval);
        btnSaveSettings = findViewById(R.id.btn_save_settings);
        btnShowFloating = findViewById(R.id.btn_show_floating);
        btnHideFloating = findViewById(R.id.btn_hide_floating);
        tvCurrentSettings = findViewById(R.id.tv_current_settings);
        
        // 保存设置按钮
        btnSaveSettings.setOnClickListener(v -> saveSettings());
        
        // 显示悬浮窗按钮
        btnShowFloating.setOnClickListener(v -> showFloatingWindow());
        
        // 隐藏悬浮窗按钮
        btnHideFloating.setOnClickListener(v -> hideFloatingWindow());
        
        // 授权栏点击事件
        authBar.setOnClickListener(v -> {
            if (shizukuHelper != null) {
                shizukuHelper.requestPermission();
            }
        });
        
        // 截图方式选择
        rgScreenshotMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_file_mode) {
                screenshotMode = 0;
            } else if (checkedId == R.id.rb_pipe_mode) {
                screenshotMode = 1;
            }
        });
        
        updateSettingsDisplay();
    }
    
    /**
     * 初始化 Shizuku
     */
    private void initShizuku() {
        shizukuHelper = new ShizukuHelper(this);
        shizukuHelper.setOnAuthChangeListener(new ShizukuHelper.OnAuthChangeListener() {
            @Override
            public void onAuthGranted() {
                runOnUiThread(() -> updateAuthBar(true));
                Log.d(TAG, "Shizuku 授权成功");
            }
            
            @Override
            public void onAuthDenied() {
                runOnUiThread(() -> updateAuthBar(false));
                Log.d(TAG, "Shizuku 授权失败");
            }
        });
        
        // 检查当前授权状态
        if (!shizukuHelper.checkPermission()) {
            // 自动请求权限
            shizukuHelper.requestPermission();
        }
    }
    
    /**
     * 初始化 OCR 识别器
     */
    private void initOcrHelper() {
        ocrHelper = new OcrHelper();
    }
    
    /**
     * 更新授权栏状态
     */
    private void updateAuthBar(boolean isAuthorized) {
        if (isAuthorized) {
            authBar.setBackgroundColor(0xFF4CAF50);
            tvAuthStatus.setText("已准备就绪");
            tvAuthCheck.setVisibility(View.VISIBLE);
        } else {
            authBar.setBackgroundColor(0xFFFF5722);
            tvAuthStatus.setText("Shizuku 未授权（点击授权）");
            tvAuthCheck.setVisibility(View.GONE);
        }
    }
    
    /**
     * 保存设置
     */
    private void saveSettings() {
        // 读取截图方式
        if (rbFileMode.isChecked()) {
            screenshotMode = 0;
        } else {
            screenshotMode = 1;
        }
        
        // 读取检测间隔
        String intervalStr = etDetectionInterval.getText().toString().trim();
        if (!TextUtils.isEmpty(intervalStr)) {
            try {
                float interval = Float.parseFloat(intervalStr);
                if (interval >= 0.1f && interval <= 10.0f) {
                    detectionInterval = interval;
                } else {
                    Toast.makeText(this, "检测间隔必须在 0.1 - 10.0 秒之间", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "检测间隔格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // 读取点击间隔
        String clickIntervalStr = etClickInterval.getText().toString().trim();
        if (!TextUtils.isEmpty(clickIntervalStr)) {
            try {
                int interval = Integer.parseInt(clickIntervalStr);
                if (interval >= 50 && interval <= 5000) {
                    clickInterval = interval;
                } else {
                    Toast.makeText(this, "点击间隔必须在 50 - 5000 ms 之间", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "点击间隔格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // 保存到 SharedPreferences
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .edit()
            .putInt(KEY_SCREENSHOT_MODE, screenshotMode)
            .putFloat(KEY_DETECTION_INTERVAL, detectionInterval)
            .putInt(KEY_CLICK_INTERVAL, clickInterval)
            .putInt(KEY_TARGET_X, targetX)
            .putInt(KEY_TARGET_Y, targetY)
            .apply();
        
        updateSettingsDisplay();
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 加载设置
     */
    private void loadSettings() {
        var prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        screenshotMode = prefs.getInt(KEY_SCREENSHOT_MODE, DEFAULT_SCREENSHOT_MODE);
        detectionInterval = prefs.getFloat(KEY_DETECTION_INTERVAL, DEFAULT_DETECTION_INTERVAL);
        clickInterval = prefs.getInt(KEY_CLICK_INTERVAL, DEFAULT_CLICK_INTERVAL);
        targetX = prefs.getInt(KEY_TARGET_X, -1);
        targetY = prefs.getInt(KEY_TARGET_Y, -1);
        
        // 更新 UI
        if (screenshotMode == 0) {
            rbFileMode.setChecked(true);
        } else {
            rbPipeMode.setChecked(true);
        }
        etDetectionInterval.setText(String.valueOf(detectionInterval));
        etClickInterval.setText(String.valueOf(clickInterval));
        
        updateSettingsDisplay();
    }
    
    /**
     * 更新设置显示
     */
    private void updateSettingsDisplay() {
        String modeText = (screenshotMode == 0) ? "保存图片" : "管道传输";
        String text = "截图方式: " + modeText + "\n" +
                     "检测间隔: " + detectionInterval + " 秒\n" +
                     "点击间隔: " + clickInterval + " ms";
        if (targetX >= 0 && targetY >= 0) {
            text += "\n目标位置: (" + targetX + ", " + targetY + ")";
        }
        tvCurrentSettings.setText(text);
    }
    
    /**
     * 显示悬浮窗
     */
    private void showFloatingWindow() {
        if (shizukuHelper == null || !shizukuHelper.checkPermission()) {
            Toast.makeText(this, "请先授权 Shizuku", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查悬浮窗权限
        if (!checkFloatingWindowPermission()) {
            requestFloatingWindowPermission();
            return;
        }
        
        if (floatingWindowServiceIntent == null) {
            floatingWindowServiceIntent = new Intent(this, FloatingWindowService.class);
            startService(floatingWindowServiceIntent);
            
            // 设置悬浮窗监听器
            new Handler().postDelayed(() -> {
                if (floatingWindowService == null) {
                    floatingWindowService = new FloatingWindowService();
                    floatingWindowService.onCreate();
                }
                floatingWindowService.setOnFloatingWindowListener(new FloatingWindowService.OnFloatingWindowListener() {
                    @Override
                    public void onStartAutoClick() {
                        startAutoClick();
                    }
                    
                    @Override
                    public void onStopAutoClick() {
                        stopAutoClick();
                    }
                    
                    @Override
                    public void onPositionSelected(int x, int y) {
                        targetX = x;
                        targetY = y;
                        updateSettingsDisplay();
                    }
                });
                
                // 恢复之前设置的位置
                if (targetX >= 0 && targetY >= 0) {
                    floatingWindowService.setTargetPosition(targetX, targetY);
                }
            }, 500);
        }
        
        Toast.makeText(this, "悬浮窗已显示", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 隐藏悬浮窗
     */
    private void hideFloatingWindow() {
        if (floatingWindowServiceIntent != null) {
            stopService(floatingWindowServiceIntent);
            floatingWindowServiceIntent = null;
            floatingWindowService = null;
            Toast.makeText(this, "悬浮窗已隐藏", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 检查悬浮窗权限
     */
    private boolean checkFloatingWindowPermission() {
        // 简化处理，实际应用中应该检查 android.permission.SYSTEM_ALERT_WINDOW
        return true;
    }
    
    /**
     * 请求悬浮窗权限
     */
    private void requestFloatingWindowPermission() {
        // 实际应用中应该引导用户到设置页面开启悬浮窗权限
        Toast.makeText(this, "请在设置中开启悬浮窗权限", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 开始自动点击
     */
    private void startAutoClick() {
        if (isRunning) {
            return;
        }
        
        if (targetX < 0 || targetY < 0) {
            Toast.makeText(this, "请先选择点击位置", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (shizukuHelper == null || !shizukuHelper.checkPermission()) {
            Toast.makeText(this, "Shizuku 未授权", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isRunning = true;
        clickRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    return;
                }
                
                performAutoClickLoop();
                
                // 计算延迟时间（毫秒）
                long delay = (long) (detectionInterval * 1000);
                handler.postDelayed(this, delay);
            }
        };
        handler.post(clickRunnable);
        
        Log.d(TAG, "自动点击已启动");
    }
    
    /**
     * 停止自动点击
     */
    private void stopAutoClick() {
        isRunning = false;
        if (handler != null && clickRunnable != null) {
            handler.removeCallbacks(clickRunnable);
        }
        Log.d(TAG, "自动点击已停止");
    }
    
    /**
     * 执行自动点击循环
     */
    private void performAutoClickLoop() {
        try {
            // 1. 获取截图
            ScreenshotHelper.ScreenshotMode mode = (screenshotMode == 0) 
                ? ScreenshotHelper.ScreenshotMode.FILE 
                : ScreenshotHelper.ScreenshotMode.PIPE;
            Bitmap screenshot = ScreenshotHelper.captureScreen(mode);
            
            if (screenshot == null) {
                Log.e(TAG, "截图失败");
                return;
            }
            
            // 2. 截取左上角区域（前 30% 高度）
            Bitmap topLeftArea = ScreenshotHelper.cropTopLeft(screenshot, 0.3f);
            
            if (topLeftArea == null) {
                screenshot.recycle();
                return;
            }
            
            // 3. OCR 识别
            String text = ocrHelper.recognizeText(topLeftArea);
            Log.d(TAG, "识别结果: " + text);
            
            // 4. 根据识别结果执行操作
            if (text.contains("自动")) {
                // 点击预设位置
                boolean success = ShizukuHelper.tap(targetX, targetY);
                if (success) {
                    Log.d(TAG, "点击位置: (" + targetX + ", " + targetY + ")");
                }
                // 等待点击间隔
                try {
                    Thread.sleep(clickInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else if (text.contains("进行中")) {
                // 停止点击
                Log.d(TAG, "检测到'进行中'，停止点击");
                stopAutoClick();
                if (floatingWindowService != null) {
                    floatingWindowService.updateStatus("已停止");
                }
            }
            // 无上述文字 → 暂停，等待下一次循环
            
            // 释放 Bitmap
            topLeftArea.recycle();
            screenshot.recycle();
            
            // 清理临时文件（如果是文件模式）
            if (screenshotMode == 0) {
                ScreenshotHelper.deleteScreenshotFile();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "自动点击循环异常", e);
        }
    }
}