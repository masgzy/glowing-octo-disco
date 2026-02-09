package com.kryp.test;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * 悬浮窗服务
 * 提供用户交互界面，用于设置点击位置和控制自动识别
 */
public class FloatingWindowService extends Service {
    private static final String TAG = "FloatingWindowService";
    
    private WindowManager windowManager;
    private View floatingView;
    private TextView statusText;
    private TextView coordinateText;
    private Button startButton;
    private Button stopButton;
    private Button selectPosButton;
    
    private int targetX = -1;
    private int targetY = -1;
    private boolean isSelectingPosition = false;
    private OnFloatingWindowListener listener;
    
    public interface OnFloatingWindowListener {
        void onStartAutoClick();
        void onStopAutoClick();
        void onPositionSelected(int x, int y);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "悬浮窗服务已创建");
        createFloatingWindow();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null && floatingView != null) {
            windowManager.removeView(floatingView);
        }
        Log.d(TAG, "悬浮窗服务已销毁");
    }

    /**
     * 创建悬浮窗
     */
    private void createFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.floating_window, null);
        
        // 设置窗口参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 200;
        
        // 添加视图
        windowManager.addView(floatingView, params);
        
        // 初始化控件
        initViews();
        
        // 设置拖拽功能
        setupDraggable(params);
    }

    /**
     * 初始化控件
     */
    private void initViews() {
        statusText = floatingView.findViewById(R.id.tv_status);
        coordinateText = floatingView.findViewById(R.id.tv_coordinate);
        startButton = floatingView.findViewById(R.id.btn_start);
        stopButton = floatingView.findViewById(R.id.btn_stop);
        selectPosButton = floatingView.findViewById(R.id.btn_select_pos);
        
        // 初始状态
        updateStatus("等待开始");
        updateCoordinate("未设置");
        stopButton.setEnabled(false);
        
        // 开始按钮点击事件
        startButton.setOnClickListener(v -> {
            if (targetX < 0 || targetY < 0) {
                updateStatus("请先选择点击位置");
                return;
            }
            updateStatus("运行中");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            selectPosButton.setEnabled(false);
            
            if (listener != null) {
                listener.onStartAutoClick();
            }
        });
        
        // 停止按钮点击事件
        stopButton.setOnClickListener(v -> {
            updateStatus("已停止");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            selectPosButton.setEnabled(true);
            
            if (listener != null) {
                listener.onStopAutoClick();
            }
        });
        
        // 选择位置按钮点击事件
        selectPosButton.setOnClickListener(v -> {
            isSelectingPosition = true;
            updateStatus("请点击屏幕选择位置");
        });
        
        // 整个悬浮窗点击事件（用于选择位置）
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isSelectingPosition) {
                    // 位置选择模式
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // 记录点击位置
                            int[] location = new int[2];
                            floatingView.getLocationOnScreen(location);
                            targetX = location[0] + (int) event.getX();
                            targetY = location[1] + (int) event.getY();
                            updateCoordinate("(" + targetX + ", " + targetY + ")");
                            isSelectingPosition = false;
                            updateStatus("位置已设置");
                            
                            if (listener != null) {
                                listener.onPositionSelected(targetX, targetY);
                            }
                            break;
                    }
                    return false;
                } else {
                    // 拖拽模式
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                            
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingView, params);
                            return true;
                    }
                    return false;
                }
            }
        });
    }

    private WindowManager.LayoutParams params;

    /**
     * 设置悬浮窗可拖拽
     */
    private void setupDraggable(WindowManager.LayoutParams layoutParams) {
        this.params = layoutParams;
    }

    /**
     * 更新状态显示
     */
    public void updateStatus(String status) {
        if (statusText != null) {
            statusText.setText("状态: " + status);
        }
    }

    /**
     * 更新坐标显示
     */
    public void updateCoordinate(String coord) {
        if (coordinateText != null) {
            coordinateText.setText("坐标: " + coord);
        }
    }

    /**
     * 获取目标点击位置
     */
    public Point getTargetPosition() {
        if (targetX >= 0 && targetY >= 0) {
            return new Point(targetX, targetY);
        }
        return null;
    }

    /**
     * 设置目标点击位置
     */
    public void setTargetPosition(int x, int y) {
        this.targetX = x;
        this.targetY = y;
        updateCoordinate("(" + x + ", " + y + ")");
    }

    /**
     * 设置监听器
     */
    public void setOnFloatingWindowListener(OnFloatingWindowListener listener) {
        this.listener = listener;
    }
}