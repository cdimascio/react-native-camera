package com.lwansbrough.RCTCamera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;


public class RCTCameraImageUtil {

    private static final String TAG = "ImageUtil";

    /**
     * Saves an image to Environment.DIRECTORY_PICTURES in the external storage public
     * directory.
     * @param bitmap
     * @param context
     * @return image path
     */
    public static String saveImage(Bitmap bitmap, final Context context) throws IOException {

        return saveImageHelper(new ImageFileCreator() {
            @Override
            public File create() throws IOException {
                File dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                return File.createTempFile(
                        generateUniqueFileName(),  /* prefix */
                        ".jpg",         /* suffix */
                        dir      /* directory */
                );
            }

            @Override
            public boolean shouldSaveToGallery() {
                return true;
            }

        }, bitmap, context);
    }


    private static String saveImageHelper(final ImageFileCreator fileCreator, Bitmap bitmap, final Context context) throws IOException {
        File imageFile = fileCreator.create();
        FileOutputStream stream = new FileOutputStream(imageFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        stream.flush();
        stream.close();
        Log.i(TAG, String.format("Saved bitmap to '%s'", imageFile.getAbsolutePath()));
        if (fileCreator.shouldSaveToGallery() && context != null) {
            addImageToGallery(imageFile.getAbsolutePath(), context);
        }
        return imageFile.toURI().toString();
    }

    private static String generateUniqueFileName() {
        return Long.toString(new Date().getTime());
    }

    private static void addImageToGallery(String path, Context context) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    interface ImageFileCreator {
        File create() throws IOException;
        boolean shouldSaveToGallery();
    }
}

