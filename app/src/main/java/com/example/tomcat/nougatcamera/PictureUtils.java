package com.example.tomcat.nougatcamera;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by tomcat on 2017/8/4.
 */
// http://blog.csdn.net/wmz199123/article/details/69371800


public class PictureUtils
{
    private final static String TAG = PictureUtils.class.getSimpleName();

    public PictureUtils()
    {}

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getFilePath_below19(Context context, Uri uri)
    {
        Log.i(TAG, "getFilePath_below19(), context: " + context +
                ", uri: " + uri);
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor   cursor = context.getContentResolver().query(uri, proj, null, null);
        int      column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath_above19(final Context context, final Uri uri)
    {
        Log.i(TAG, "getPath_above19(), context: " + context +
                ", uri: " + uri);
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        //DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri))
        {
            if (isExternalStorageDocument(uri))     // External Storage Provider
            {
                final String    docId = DocumentsContract.getDocumentId(uri);
                final String[]  split = docId.split(":");
                final String    type = split[0];
                if ("primary".equalsIgnoreCase(type))
                {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            else if (isDownloadsDocument(uri))      // Downloads Provider
            {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri    contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            else if (isMediaDocument(uri))          // Media Provider
            {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;

                if ("image".equals(type))
                {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("video".equals(type))
                {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("audio".equals(type))
                {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        //MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme()))
        {
            if (isGooglePhotosUri(uri))
            {
                return uri.getLastPathSegment();
            }

            return getDataColumn(context, uri, null, null);
        }
        //File
        else if ("file".equalsIgnoreCase(uri.getScheme()))
        {
            return uri.getPath();
        }

        return null;
    }


    public static String getDataColumn(Context context,
                                       Uri uri, String selection,
                                       String[] selectionArgs)
    {
        Log.i(TAG,  "getDataColumn(), context: " + context +
                    ", uri: " + uri +
                    ", selectionArgs: " + Arrays.toString(selectionArgs));

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try
        {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if ((cursor != null) && (cursor.moveToFirst()))
            {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        }
        finally
        {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri)
    {
        Log.i(TAG,  "isExternalStorageDocument(), uri: " + uri);
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri)
    {
        Log.i(TAG,  "isDownloadsDocument(), uri: " + uri);
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri)
    {
        Log.i(TAG,  "isMediaDocument(), uri: " + uri);
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri)
    {
        Log.i(TAG,  "isGooglePhotosUri(), uri: " + uri);
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        Log.i(TAG,  "calculateInSampleSize(), options: " + options +
                    ", reqWidth; " + reqWidth +
                    ", reqHeight: " + reqHeight);

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if ((height > reqHeight) || (width > reqWidth))
        {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float)width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            Log.i(TAG, "height: " + height + ", width: " + width +
                    ", heightRatio: " + heightRatio + ", widthRatio: " + widthRatio +
                    ", inSampleSize: " + inSampleSize);
        }

        return inSampleSize;
    }

    public static Bitmap getSamllBitmap(String filePath, int reqWidth, int reqHeight)
    {
        Log.i(TAG,  "getSamllBitmap(), filePath: " + filePath +
                ", reqWidth; " + reqWidth +
                ", reqHeight: " + reqHeight);

        final BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath, options);
    }

    public static boolean hasSdcard()
    {
        Log.i(TAG,  "hasSdcard()..." );

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static void gallerAddPicture(String mPublicPhotoPath, Context context)
    {
        Log.i(TAG,  "gallerAddPicture(), mPublicPhotoPath: " + mPublicPhotoPath +
                    ", context: " + context);

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mPublicPhotoPath);

        Uri  contentUri = Uri.fromFile(f);

        Log.i(TAG,  "mediaScanIntent: " + mediaScanIntent +
                    ", contentUri: " + contentUri + ", File size: " + f.length());

        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    public static File createPublicImageFile() throws IOException
    {
        Log.i(TAG,  "createPublicImageFile()...");

        //File appDir = new File(Environment.getExternalStorageDirectory() + "/photodemo");
        File appDir = new File(Environment.getExternalStorageDirectory() + "/mt24hr/");
        if (!appDir.exists())
        {
            appDir.mkdir();
        }

        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);

        return file;
    }

}

