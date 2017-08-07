package com.example.tomcat.nougatcamera;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(mPublicPhotoPath));
                    Log.i(TAG, "bitmap size: " + bitmap.getByteCount() + " bytes");

                } catch (IOException e) {
                    e.printStackTrace();
                }


                //setUriToBitmap();
                //Bitmap  bitmap = new Bitmap().
                File f2 = new File(mPublicPhotoPath);
                Log.i(TAG, "f2 size: " + f2.length());

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
        ivPicture.setImageBitmap(PictureUtils.getSamllBitmap(path, mTargetW, mTargetH));

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

    private void startTake()
    {
        Log.i(TAG, "startTake()...");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
        {
            File photoFile = null;
            try
            {
                photoFile = PictureUtils.createPublicImageFile();
                mPublicPhotoPath = photoFile.getAbsolutePath();
                Log.i(TAG, "mPublicPhotoPath: " + mPublicPhotoPath);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            if (photoFile != null)
            {
                takePictureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri photoURI = FileProvider.getUriForFile(this, PROVIDER_PATH, photoFile);
                Log.i(TAG, "photoURI: " + photoURI + ", photoFile: " + photoFile.length());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQ_GALLERY);
            }
        }
    }

}


