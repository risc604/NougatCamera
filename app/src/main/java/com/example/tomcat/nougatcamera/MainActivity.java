package com.example.tomcat.nougatcamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;


//https://stackoverflow.com/questions/16060143/android-take-photo-and-resize-it-before-saving-on-sd-card/36210688#36210688

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_PICK_IMAGE = 222;
    private static final int REQ_GALLERY = 333;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int WRITE_PERMISSION_CODE = 101;
    private static final int READ_PERMISSION_CODE = 102;

    private static final String PROVIDER_PATH = BuildConfig.APPLICATION_ID + ".provider";

    private String mPublicPhotoPath;
    private Uri     uri;
    String  path;
    int     mTargetW;
    int     mTargetH;

    ImageView   ivPicture;
    Button      btnPhoto;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "onCreate()...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initControl();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.i(TAG, "onActivityResult(), requestCode: " + requestCode +
                    ", resultCode: " + resultCode + ", data: " + data);
        super.onActivityResult(requestCode, resultCode, data);

        mTargetW = ivPicture.getWidth();
        mTargetH = ivPicture.getHeight();

        switch (requestCode)
        {
            case REQ_GALLERY:
                if (resultCode != Activity.RESULT_OK)
                    return;

                //setUriToBitmap();
                //Bitmap  bitmap = new Bitmap().
                //File f2 = new File(mPublicPhotoPath);
               // Log.i(TAG, "f2 size: " + f2.length());

                uri = Uri.parse(mPublicPhotoPath);
                path = uri.getPath();
                PictureUtils.gallerAddPicture(mPublicPhotoPath, this);
                Log.i(TAG, "mPublicPhotoPath: " + mPublicPhotoPath + ", uri: " + uri +
                            ", path: " + path );
                break;

            case REQUEST_CODE_PICK_IMAGE:
                if (data == null)   return;

                uri = data.getData();
                if (Build.VERSION.SDK_INT >= 19)
                {
                    path = this.uri.getPath();
                    path = PictureUtils.getPath_above19(MainActivity.this, this.uri);
                }
                //else
                //{
                //    path = PictureUtils.getFilePath_below19(MainActivity.this, this.uri);
                //}

                break;
        }
        int degree = getOrientention(path);
        Bitmap tmpBMP = rotateImage(PictureUtils.getSamllBitmap(path, mTargetW, mTargetH), degree);
        //tmpBMP = resize(tmpBMP, 800, 600);
        tmpBMP = getResizedBitmap(tmpBMP, 640, 480);
        //tmpBMP = Bitmap.createScaledBitmap(tmpBMP, 640, 480, true);
        Log.i(TAG, "degree: " + degree + ", BMP size: " + tmpBMP.getByteCount());

        try {
            FileOutputStream fos = new FileOutputStream(
                    new File(   Environment.getExternalStorageDirectory()+"/mt24hr/" +
                                System.currentTimeMillis() + ".png"));
            tmpBMP.compress(Bitmap.CompressFormat.PNG, 90, fos);
            Log.i(TAG, "File size: " + (float)fos.getChannel().size()/1024);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ivPicture.setImageBitmap(tmpBMP);
        //ivPicture.setImageBitmap(PictureUtils.getSamllBitmap(path, mTargetW, mTargetH));
        if (photoFile != null && photoFile.exists()) {
            Log.i(TAG, "photoFile: " + photoFile.getAbsolutePath() +
                    ", size: " + ((float)photoFile.length())/1024 + " KBytes.");
            photoFile.delete();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        Log.i(TAG, "onRequestPermissionsResult(), requestCode: " + requestCode +
                ", permissions: " + Arrays.toString(permissions) +
                ",\ngrantResults: " + Arrays.toString(grantResults));

        switch (requestCode)
        {
            case WRITE_PERMISSION_CODE:
                if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED))
                {
                }
                else
                {
                    Toast.makeText(this, "NO Permission to write SD cord. Fail!!",
                            Toast.LENGTH_SHORT).show();
                }
                break;

            case READ_PERMISSION_CODE:
                if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED))
                {
                }
                else
                {
                    Toast.makeText(this, "NO Permission to Read SD cord. Fail!!",
                            Toast.LENGTH_SHORT).show();
                }
                break;

            case CAMERA_PERMISSION_CODE:
                if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED))
                {
                    startTake();
                }
                else
                {
                    Toast.makeText(this, "NO Permission open Camera. Fail!!",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void btnGetPhotoOnClick(final View view)
    {
        Log.i(TAG, "btnGetPhotoOnClick(), view: " + view);
        CharSequence[] items = {"相 簿", "相 機"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("選 取 照 片");
        builder.setItems(items, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int which)
            {
                switch (which)
                {
                    case 0:     // Album
                        getImageFormAlbum();
                        break;

                    case 1:     // Camera
                        showTakePicture();
                        break;

                    default:
                        Toast.makeText(view.getContext(), "Error !!", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        builder.show();
    }


    //--------------- User define function -------------------------------//
    private void initView()
    {
        Log.i(TAG, "initView()...");
        ivPicture = (ImageView) findViewById(R.id.imageView1);
        btnPhoto = (Button) findViewById(R.id.btnGetPhoto);
    }

    private void initControl()
    {
        Log.i(TAG, "initControl()...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_CODE);
        }
        else
        {
            Toast.makeText(this, "SD card Permission has always OK.", Toast.LENGTH_SHORT).show();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION_CODE);
        }
        else
        {
            Toast.makeText(this, "SD card Read Permission always OK.", Toast.LENGTH_SHORT).show();
        }
    }

    //private void setUriToBitmap() throws IOException {
    //    Bitmap photo = (Bitmap) "your Bitmap image";
    //    photo = Bitmap.createScaledBitmap(photo, 100, 100, false);
    //    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    //    photo.compress(Bitmap.CompressFormat.JPEG, 40, bytes);
    //
    //    File f = new File(Environment.getExternalStorageDirectory()
    //            + File.separator + "Imagename.jpg");
    //    f.createNewFile();
    //    FileOutputStream fo = new FileOutputStream(f);
    //    fo.write(bytes.toByteArray());
    //    fo.close();
    //}

    private void getImageFormAlbum()
    {
        Log.i(TAG, "getImageFormAlbum()...");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    private void showTakePicture()
    {
        Log.i(TAG, "showTakePicture()...");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        }
        else
        {
            startTake();
        }
    }

    File photoFile = null;
    private void startTake()
    {
        Log.i(TAG, "startTake()...");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
        {
            //File photoFile = null;
            //photoFile = PictureUtils.createPublicImageFile();
            photoFile = new File(Uri.parse("file:////sdcard/image_output.jpg").getPath());
            //photoFile = Uri.("file:////sdcard/image_output.jpg");
            mPublicPhotoPath = photoFile.getAbsolutePath();
            Log.i(TAG, "mPublicPhotoPath: " + mPublicPhotoPath);

            if (photoFile != null)
            {
                takePictureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri photoURI = null;
                if (Build.VERSION.SDK_INT >= 24)
                    photoURI = FileProvider.getUriForFile(this, PROVIDER_PATH, photoFile);
                else
                {
                    photoURI = Uri.fromFile(photoFile);
                }
                Log.i(TAG, "photoURI: " + photoURI + ", photoFile: " + photoFile.length());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQ_GALLERY);
            }
        }
    }

    //public static Bitmap rotateImage(Bitmap img, int degree)
    private Bitmap rotateImage(Bitmap img, int degree)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        return rotatedImg;
    }

    //public static int getOrientention(String filePath)
    private int getOrientention(String filePath)
    {
        File f = new File(filePath);
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(f.getPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            Log.i(TAG, "orientation: " + orientation + ", exif: " + exif.toString());

            int angle = 0;

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    angle = 90;
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    angle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    angle = 270;
                    break;

                default:
                    Log.e(TAG, "Error !! orientation: " + orientation);
                    break;
            }

            return angle;
        }
        else
        {
            Log.e(TAG, "Error !! exif is null, no Orientention info, degree set 0 !!");
            return 0;
        }
    }

    //public static Bitmap rotateImageIfRequired(Bitmap img, Context context, Uri selectedImage) throws IOException
    private Bitmap rotateImageIfRequired(Bitmap img, Context context, Uri selectedImage) throws IOException
    {
        if (selectedImage.getScheme().equals("content"))
        {
            String[] projection = { MediaStore.Images.ImageColumns.ORIENTATION };
            Cursor c = context.getContentResolver().query(selectedImage, projection, null, null, null);
            if (c.moveToFirst())
            {
                final int rotation = c.getInt(0);
                Log.w(TAG, "rotation: " + rotation);

                c.close();
                return rotateImage(img, rotation);
            }
            return img;
        }
        else
        {
            ExifInterface ei = new ExifInterface(selectedImage.getPath());
            int orientation = ei.getAttributeInt(   ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            //Timber.d("orientation: %s", orientation);
            Log.d(TAG, "orientation: " + orientation);

            switch (orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        }
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth)
    {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // RECREATE THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    private Bitmap resize(Bitmap image, int maxWidth, int maxHeight)
    {
        if (maxHeight > 0 && maxWidth > 0)
        {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1)
            {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            }
            else
            {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }

            Log.i(TAG, "resize(), ratioMax: " + ratioMax +
                    ", finalWidth: " + finalWidth + ", finalHeight: " + finalHeight);
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        }
        else
        {
            return image;
        }
    }


}


