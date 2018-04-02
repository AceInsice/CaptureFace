
package org.easydarwin.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.VideoView;

import org.easydarwin.common.EasyApplication;
import org.easydarwin.config.MyPreference;
import org.easydarwin.easypusher.R;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import android.media.FaceDetector;
import android.media.FaceDetector.Face;


public final class Utilities {



    public static Bitmap getPreviewBitmap(byte[] previewbyte,Camera.Size size){
        Bitmap previewimg = null;
        try {

            YuvImage image = new YuvImage(previewbyte, ImageFormat.NV21, size.width, size.height, null);
            if(image!=null){
                ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, outstream);
                outstream.flush();
                byte[] jpgImage = outstream.toByteArray();
                previewimg = BitmapFactory.decodeByteArray(jpgImage, 0,
                        jpgImage.length);
            }
        } catch (IOException e) {
        }
			/*Matrix matrix = new Matrix();
			matrix.postRotate(90);
			return Bitmap.createBitmap(previewimg,
			        		0, 0, previewimg.getWidth(),  previewimg.getHeight(), matrix, true);*/
        return previewimg;
    }
    private static Face[] mCurrentFaces;
    private static long mCurrentTime;
    private static long mFaceDeteckDuration;
    private static int mMoveRate;
    public static synchronized List<byte[]> getFaceBitmap(Bitmap b) {
        mMoveRate = MyPreference.getInstance().getMoverate();
        //mMoveRate = 30;
        List<byte[]> facesList = new ArrayList<>();
        try {
            Bitmap bitmap = b.copy(Bitmap.Config.RGB_565, true);
            //bitmap.
            b.recycle();
            if (null != bitmap) {
                FaceDetector mFaceDetector = new FaceDetector(
                        bitmap.getWidth(), bitmap.getHeight(), 10);

                Face[] mFaces = new Face[10];
                Face[] moreFaces = null;
                mFaceDetector.findFaces(bitmap, mFaces);
                if(null == mCurrentFaces){
                    mCurrentFaces = mFaces;
                    mCurrentTime = System.currentTimeMillis();
                    moreFaces = mFaces;
                }else {
                    moreFaces = getMoreFace(mFaces);
                }
                if(null == moreFaces){
                    return facesList;
                }
                for(int i = 0; i < moreFaces.length;i++){

                    byte[] face = null;
                    Face bestface = moreFaces[i];
                    if (null != bestface) {
                        PointF point = new PointF();
                        int width = bitmap.getWidth() > (int) (bestface
                                .eyesDistance() * 3) ? (int) (bestface
                                .eyesDistance() * 3.5) : bitmap.getWidth();
                        int height = bitmap.getHeight() > (int) (width * 1.4) ? (int) (width * 1.4)
                                : bitmap.getHeight();
                        bestface.getMidPoint(point);
                        if (width > 0 && height > 0) {
                            int pointx = (int) point.x - width / 2 > 0 ? (int) point.x
                                    - width / 2 : 0;
                            int pointy = (int) point.y - height / 2 > 0 ? (int) point.y
                                    - height / 2
                                    : 0;
                            Bitmap scaleface = null;
                            if ((pointx + width) >= bitmap.getWidth()
                                    ) {
                                width = bitmap.getWidth() - (int) point.x;
                                pointx = (int) point.x - width / 2 > 0 ? (int) point.x
                                        - width / 2 : 0;
                            }
                            if((pointy + height) >= bitmap.getHeight()){
                                height = bitmap.getHeight() - (int) point.y;
                                pointy = (int) point.y - height / 2 > 0 ? (int) point.y
                                        - height / 2 : 0;
                            }

                            scaleface = Bitmap.createBitmap(bitmap, pointx,
                                    pointy, width, height);


                            face = Utilities.scaleBitmap(scaleface);
                            facesList.add(face);
                        }
                    }

                }

            }
            return facesList;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return null;
        }

    }
    private static Face[] getMoreFace(Face[] faces) {
        if(null == faces || faces.length == 0){
            return null;
        }
        Face[] moreFace = null;
        mFaceDeteckDuration = System.currentTimeMillis() - mCurrentTime;

        if(mFaceDeteckDuration > 4000){
            Log.e("nimei","mFaceDeteckDuration:   " + mFaceDeteckDuration);
            mCurrentTime =  System.currentTimeMillis();
            mCurrentFaces = faces;
            return faces;
        }
			/*if(mFaceDeteckDuration < 100){
				return null;
			}*/


        List<Face> mFaceList = new ArrayList<>();
        for(int i = 0;i < faces.length;i++){
            if(null != faces[i]){
                double moverate = getMoveRate(faces[i]);
                Log.e("nimei","movereate:   " + moverate);
                if(moverate > mMoveRate){
                    mFaceList.add(faces[i]);
                }
            }else{
                break;
            }
        }

        if(mFaceList.size() > 0){
            moreFace = new Face[mFaceList.size()];
            for(int i = 0;i < mFaceList.size();i++){
                moreFace[i] = mFaceList.get(i);
            }
        }
        mCurrentFaces = faces;
        mCurrentTime =  System.currentTimeMillis();
        return moreFace;
    }

    private static double getMoveRate(Face face) {
        double rate = 1000;
        for(int i = 0; i < mCurrentFaces.length; i++){
            if(null != mCurrentFaces[i]){
                double newrate = getMoveRate(mCurrentFaces[i],face);
                if(newrate < rate){
                    rate = newrate;
                }
            }else{
                break;
            }

        }
        return  rate;
    }

    private static double getMoveRate(Face face, Face face2) {
        PointF faceone = new PointF();
        PointF facetwo = new PointF();
        face.getMidPoint(faceone);
        face2.getMidPoint(facetwo);
        float xDistance = faceone.x - facetwo.x;
        float yDistance = faceone.y - facetwo.y;
        Log.e("nimei",faceone.x +"faceone.x" +facetwo.x +"facetwo.x");
        double xf = Math.pow(xDistance,2) ;
        double yf = Math.pow(yDistance,2);
        return  Math.pow(xf+yf, 0.5);
    }
}