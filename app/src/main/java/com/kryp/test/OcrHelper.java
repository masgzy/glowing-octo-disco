package com.kryp.test;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * OCR 识别辅助类
 * 使用 Google ML Kit 进行文字识别
 */
public class OcrHelper {
    private static final String TAG = "OcrHelper";
    
    // 中文识别器
    private com.google.mlkit.vision.text.TextRecognizer recognizer;

    public OcrHelper() {
        // 创建中文识别器
        recognizer = TextRecognition.getClient(
            new ChineseTextRecognizerOptions.Builder().build()
        );
    }

    /**
     * 识别图片中的文字
     * @param bitmap 要识别的图片
     * @return 识别到的文字内容
     */
    public String recognizeText(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.e(TAG, "Bitmap 为空或已回收");
            return "";
        }

        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            
            // 使用 CountDownLatch 等待异步结果
            final String[] resultText = new String[1];
            final CountDownLatch latch = new CountDownLatch(1);
            
            recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    resultText[0] = visionText.getText();
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR 识别失败", e);
                    resultText[0] = "";
                    latch.countDown();
                });
            
            // 等待最多 5 秒
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            if (!completed) {
                Log.e(TAG, "OCR 识别超时");
                return "";
            }
            
            return resultText[0] != null ? resultText[0] : "";
        } catch (Exception e) {
            Log.e(TAG, "OCR 识别异常", e);
            return "";
        }
    }

    /**
     * 检查图片中是否包含指定文字
     * @param bitmap 要识别的图片
     * @param targetText 目标文字
     * @return 是否包含目标文字
     */
    public boolean containsText(Bitmap bitmap, String targetText) {
        String recognizedText = recognizeText(bitmap);
        return recognizedText.contains(targetText);
    }

    /**
     * 获取图片中所有文字块的位置信息
     * @param bitmap 要识别的图片
     * @return 文字块位置列表
     */
    public List<TextBlock> getTextBlocks(Bitmap bitmap) {
        List<TextBlock> blocks = new ArrayList<>();
        
        if (bitmap == null || bitmap.isRecycled()) {
            return blocks;
        }

        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            final CountDownLatch latch = new CountDownLatch(1);
            
            recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        Rect boundingBox = block.getBoundingBox();
                        String text = block.getText();
                        if (boundingBox != null && text != null) {
                            blocks.add(new TextBlock(text, boundingBox));
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "获取文字块失败", e);
                    latch.countDown();
                });
            
            latch.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "获取文字块异常", e);
        }
        
        return blocks;
    }

    /**
     * 查找指定文字在图片中的位置
     * @param bitmap 要识别的图片
     * @param targetText 目标文字
     * @return 文字位置矩形，未找到返回 null
     */
    public Rect findTextPosition(Bitmap bitmap, String targetText) {
        List<TextBlock> blocks = getTextBlocks(bitmap);
        for (TextBlock block : blocks) {
            if (block.text.contains(targetText)) {
                return block.rect;
            }
        }
        return null;
    }

    /**
     * 释放资源
     */
    public void close() {
        if (recognizer != null) {
            recognizer.close();
        }
    }

    /**
     * 文字块数据类
     */
    public static class TextBlock {
        public String text;
        public Rect rect;

        public TextBlock(String text, Rect rect) {
            this.text = text;
            this.rect = rect;
        }
    }
}