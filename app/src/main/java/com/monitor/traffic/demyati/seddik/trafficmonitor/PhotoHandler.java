package com.monitor.traffic.demyati.seddik.trafficmonitor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PhotoHandler implements PictureCallback {

    private final Context context;
    Bitmap result;

    String RotatedFilename;

    public PhotoHandler(Context context) {
        this.context = context;
    }

    private File getDir() {
        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(sdDir, "ReceivedImages");
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        // Create Folder to save pictures
        File pictureFileDir = getDir();
        //Check if file is created
        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
            Log.d("MakePhotoActivity", "Can't create directory to save image.");
            Toast.makeText(context, "Can't create directory to save image.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        //Set name for the taken photo
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoFile = "OldPicture_" + date + ".jpg";
        String RotatedPhotoFile = "Picture_" + date + ".jpg";
        String filename = pictureFileDir.getPath() + File.separator + photoFile;
        //create photo
        File pictureFile = new File(filename);
        //save it to sdCard
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            Bitmap RotatedPhoto= RotateBitmap(filename,90);
            RotatedFilename=pictureFileDir.getPath() + File.separator + RotatedPhotoFile;
            RotatedPhoto.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(RotatedFilename));
            //send photo to ConnectActivity
            Intent intent=new Intent("IncomingMessages");
            intent.putExtra("IncomingMessage",RotatedFilename);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            pictureFile.delete();
            //Cleanup
            RotatedPhoto.recycle();
            result.recycle();
        } catch (Exception error) {
            Log.d("MakePhotoActivity", "File" + filename + "not saved: "
                    + error.getMessage());
            Toast.makeText(context, "Image could not be saved.",
                    Toast.LENGTH_LONG).show();
        }
    }

    //Rotate taken photo
    public Bitmap RotateBitmap(String FilePath, float angle) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        //reducing image size to load in memory
        //for more info
        //https://stackoverflow.com/questions/25719620/how-to-solve-java-lang-outofmemoryerror-trouble-in-android
        bmOptions.inSampleSize = 4;
        Bitmap source = BitmapFactory.decodeFile(FilePath,bmOptions);
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        result=Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        return result;
    }
}