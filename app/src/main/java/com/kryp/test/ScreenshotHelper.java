package com.kryp.test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;

/**
 * 截图辅助类
 * 支持两种截图方式：保存图片文件、管道传输
 */
public class ScreenshotHelper {
    private static final String TAG = "ScreenshotHelper";
    private static final String SCREENSHOT_PATH = "/sdcard/tmp/screenshot.png";
    
    /**
     * 截图方式枚举
     */
    public enum ScreenshotMode {
        FILE,      // 保存到文件
        PIPE       // 管道传输
    }

    /**
     * 获取屏幕截图
     * @param mode 截图方式
     * @return 截图 Bitmap，失败返回 null
     */
    public static Bitmap captureScreen(ScreenshotMode mode) {
        switch (mode) {
            case FILE:
                return captureScreenToFile();
            case PIPE:
                return captureScreenByPipe();
            default:
                return captureScreenByPipe();
        }
    }

    /**
     * 保存图片方式截图
     * 将截图保存到 /sdcard/tmp/screenshot.png
     * @return 截图 Bitmap，失败返回 null
     */
    public static Bitmap captureScreenToFile() {
        try {
            // 确保临时目录存在
            ShizukuHelper.createTempDir();
            
            // 执行截图命令
            String cmd = "screencap -p " + SCREENSHOT_PATH;
            String result = ShizukuHelper.exec(cmd);
            
            if (result == null) {
                Log.e(TAG, "截图命令执行失败");
                return null;
            }
            
            // 从文件读取 Bitmap
            File file = new File(SCREENSHOT_PATH);
            if (!file.exists()) {
                Log.e(TAG, "截图文件不存在: " + SCREENSHOT_PATH);
                return null;
            }
            
            Bitmap bitmap = BitmapFactory.decodeFile(SCREENSHOT_PATH);
            if (bitmap == null) {
                Log.e(TAG, "解析截图文件失败");
            }
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "保存图片方式截图失败", e);
            return null;
        }
    }

    /**
     * 管道方式截图
     * 直接从 screencap 命令的输出流读取数据
     * @return 截图 Bitmap，失败返回 null
     */
    public static Bitmap captureScreenByPipe() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", "screencap -p"});
            
            DataInputStream dis = new DataInputStream(process.getInputStream());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = dis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            dis.close();
            process.waitFor();
            process.destroy();
            
            byte[] pngData = baos.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(pngData, 0, pngData.length);
            
            if (bitmap == null) {
                Log.e(TAG, "管道方式解析截图失败");
            }
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "管道方式截图失败", e);
            return null;
        }
    }

    /**
     * 裁剪图片的左上角区域
     * @param bitmap 原始图片
     * @param heightRatio 高度比例 (0.0 - 1.0)，例如 0.3 表示截取前 30% 高度
     * @return 裁剪后的图片
     */
    public static Bitmap cropTopLeft(Bitmap bitmap, float heightRatio) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        
        if (heightRatio <= 0 || heightRatio > 1) {
            heightRatio = 0.3f; // 默认 30%
        }
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int cropHeight = (int) (height * heightRatio);
        
        if (cropHeight <= 0) {
            cropHeight = height / 3;
        }
        
        try {
            return Bitmap.createBitmap(bitmap, 0, 0, width, cropHeight);
        } catch (Exception e) {
            Log.e(TAG, "裁剪图片失败", e);
            return bitmap;
        }
    }

    /**
     * 裁剪图片的指定区域
     * @param bitmap 原始图片
     * @param x 起始 x 坐标
     * @param y 起始 y 坐标
     * @param width 裁剪宽度
     * @param height 裁剪高度
     * @return 裁剪后的图片
     */
    public static Bitmap cropBitmap(Bitmap bitmap, int x, int y, int width, int height) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        
        // 确保坐标在图片范围内
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + width > bitmap.getWidth()) {
            width = bitmap.getWidth() - x;
        }
        if (y + height > bitmap.getHeight()) {
            height = bitmap.getHeight() - y;
        }
        
        if (width <= 0 || height <= 0) {
            return null;
        }
        
        try {
            return Bitmap.createBitmap(bitmap, x, y, width, height);
        } catch (Exception e) {
            Log.e(TAG, "裁剪图片失败", e);
            return null;
        }
    }

    /**
     * 删除截图文件
     */
    public static void deleteScreenshotFile() {
        try {
            File file = new File(SCREENSHOT_PATH);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "删除截图文件失败", e);
        }
    }
}