package jp.techacademy.miura3.yuuko.autoslideshowapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView mImageView;
    Button mPrevButton;
    Button mNextButton;
    Button mAutoButton;
    Cursor mCursor = null;
    Timer mTimer;
    Handler mHandler = new Handler();

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String AUTO_BUTTON_LABEL_START = "再生";
    private static final String AUTO_BUTTON_LABEL_STOP = "停止";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNextButton = (Button)findViewById(R.id.nextButton);
        mNextButton.setOnClickListener(this);

        mPrevButton = (Button)findViewById(R.id.prevButton);
        mPrevButton.setOnClickListener(this);

        mAutoButton = (Button)findViewById(R.id.autoButton);
        mAutoButton.setOnClickListener(this);
        mAutoButton.setText(AUTO_BUTTON_LABEL_START);

        mImageView = (ImageView) findViewById(R.id.imageView);

        // ImageViewの高さを調整
        WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        double screenHeight = (double)(size.y);
        mImageView.setMaxHeight((int)(screenHeight * 0.7));
        mImageView.setAdjustViewBounds(true);
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                showFirstImage();
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
            // Android 5系以下の場合
        } else {
            showFirstImage();
        }
    }

    @Override
    public void onClick(View v) {
        if(mCursor == null) {
            return;
        }

        if( v.getId() == R.id.autoButton) {
            auto();
        } else if(v.getId() == R.id.nextButton) {
            next();
        } else if(v.getId() == R.id.prevButton) {
            prev();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCursor != null){
            mCursor.close();
            mCursor = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showFirstImage();
                }
                break;
            default:
                break;
        }
    }

    private void showFirstImage() {
        // 画像の情報を取得する
        ContentResolver resolver = getContentResolver();
        mCursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );

        mCursor.moveToFirst();
        showImage();
    }

    private void next() {
        if( mCursor.isLast()){
            mCursor.moveToFirst();
        }else {
            mCursor.moveToNext();
        }

        showImage();
    }

    private void prev() {
        if( mCursor.isFirst()){
            mCursor.moveToLast();
        }else {
            mCursor.moveToPrevious();
        }

        showImage();
    }

    private void auto() {
        boolean isAutoStart;

        // 再生ボタン
        if (mAutoButton.getText().toString().equals(AUTO_BUTTON_LABEL_START)) {
            isAutoStart = true;

            mAutoButton.setText(AUTO_BUTTON_LABEL_STOP);

            // タイマーの作成
            mTimer = new Timer();
            // タイマーの始動
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            next();
                        }
                    });
                }
            }, 2000, 2000);    // 最初に始動させるまで 2秒、ループの間隔を 2秒 に設定

        } else {
            // 停止ボタン
            isAutoStart = false;
            mAutoButton.setText(AUTO_BUTTON_LABEL_START);

            mTimer.cancel();
            mTimer = null;
        }

        mNextButton.setEnabled(!isAutoStart);
        mPrevButton.setEnabled(!isAutoStart);
    }

    private void showImage() {
        int fieldIndex = mCursor.getColumnIndex(MediaStore.Images.Media._ID);
        Long id = mCursor.getLong(fieldIndex);
        Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

        mImageView.setImageURI(imageUri);
    }
}
