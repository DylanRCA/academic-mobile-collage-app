package com.example.appcollagefotos.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ImageProcessor {

    public static Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float widthRatio = (float) maxWidth / width;
        float heightRatio = (float) maxHeight / height;
        float scale = Math.min(widthRatio, heightRatio);

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    public static Bitmap scaleBitmap(Bitmap bitmap) {
        return scaleBitmap(bitmap, 1024, 768);
    }

    public static String compressAndEncodeBase64(Bitmap bitmap, int quality, int maxWidth, int maxHeight) {
        Bitmap scaled = scaleBitmap(bitmap, maxWidth, maxHeight);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        byte[] byteArray = stream.toByteArray();
        return encodeByteArrayToBase64(byteArray);
    }

    public static String compressAndEncodeBase64(Bitmap bitmap) {
        return compressAndEncodeBase64(bitmap, 65, 1024, 768);
    }

    public static Bitmap decodeBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = decodeBase64ToByteArray(base64String);
            if (decodedBytes == null) return null;
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    public static String encodeByteArrayToBase64(byte[] bytes) {
        if (bytes == null) return "";
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static byte[] decodeBase64ToByteArray(String base64String) {
        if (base64String == null) return null;
        try {
            return Base64.decode(base64String, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }
}
