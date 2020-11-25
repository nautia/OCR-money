package com.example.moneyocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class main extends AppCompatActivity {
    /* 사용 변수 모음 */

    private static ImageView img;
    Button replay, camera;
    Bitmap bitmap;
    int chk = 1;
    File filePath;
    TextView txt;
    int cnt = 0;

    TextToSpeech myTTS;

    private String[] permissions = {                          /* permissions 모음 */
            Manifest.permission.WRITE_EXTERNAL_STORAGE,     // 기기, 사진, 미디어, 파일 엑세스 권한
            Manifest.permission.CAMERA
    };
    private static final int MULTIPLE_PERMISSIONS = 101;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= 23) {              // 안드로이드 6.0 이상일 경우 퍼미션 체크 (API 23 이상!!)
            checkPermissions();  // checkPermissions() 함수 호출
        }

        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                myTTS.setLanguage(Locale.KOREAN);
                myTTS.speak("지폐인식 기능이 실행되었습니다. 촬영 후 오른쪽 하단을 터치하십시오. 촬영 방법은 볼륨키 입니다.", TextToSpeech.QUEUE_FLUSH,null);
            }
        });

        img = findViewById(R.id.viewImage);
        replay = findViewById(R.id.button_replay);
        camera = findViewById(R.id.button_camera);
        txt = findViewById(R.id.textView3);

        AssetManager am = getResources().getAssets();
        InputStream is = null;{
            try {
                is = am.open("main_logo.png");
                bitmap = BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 촬영 선택
        camera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                chk = 1;
                setImageUri();
                myTTS.speak("지폐인식 기능이 실행되었습니다. 촬영 후 오른쪽 하단을 터치하십시오. 촬영 방법은 볼륨키 입니다.", TextToSpeech.QUEUE_FLUSH,null);
            }
        });

        // 다시듣기 선택
        replay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTextFromImage(bitmap);
            }
        });


    }

    // 사진 촬영 함수
    void setImageUri() {
        try {
            //dirPath = 데이터를 myApp앱에 넣어두고 불러와서 처리(경로 지정)
            String dirPath = Environment.getExternalStorageDirectory().getPath() + "/myApp/";

            File dir = new File(dirPath);//dir = dirPath를 파일로 생성
            if (!dir.exists()) { //경로가 없을 경우
                dir.mkdir(); //경로에 대한 폴더를 만듬
            }
            //filePath = dir 경로에 있는 파일을 IMG 로 하고 .jpg 형식으로 생성
            filePath = File.createTempFile("IMG", ".jpg", dir);
            if (!filePath.exists()) { //filePath가 없을 경우
                filePath.createNewFile(); //새로운 파일을 만든다.
            }

            Uri imgUri = FileProvider.getUriForFile(this, "com.example.moneyocr", filePath);
            ActivityCompat.requestPermissions(main.this, new String[]{Manifest.permission.CAMERA}, 1);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
            startActivityForResult(intent, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 선택된 이미지를 텍스트로 변환하기 전 화면에 사진을 셋팅
    protected final void onActivityResult(int requestCode, int resultCode, @Nullable Intent i) {
        super.onActivityResult(requestCode, resultCode, i);

        // 사진 촬영을 선택할 때
        if (chk == 1) {
            if (filePath != null) {//filePath에 이미지가 없을 경우
                setProfileImage(filePath,img);//filePath에서 찍은 이미지를 img에 보여라
                img.setImageBitmap(bitmap);
            }
        }
    }

    // 셋팅된 이미지를 텍스트로 변환하는 함수, (해당 지폐가 얼마인지 출력 포함)
    public void getTextFromImage(Bitmap v){
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if(!textRecognizer.isOperational()){
            Toast.makeText(this, "OCR이 작동되지 않음", Toast.LENGTH_SHORT).show();
        }
        else {
            try {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();

                SparseArray<TextBlock> items = textRecognizer.detect(frame);

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < items.size(); i++) {
                    TextBlock myItem = items.valueAt(i);
                    // 인식된 텍스트를 sb변수에 한줄 한줄 붙여넣는다. (한줄 한줄 받아오면 인식률 향상)
                    sb.append(myItem.getValue());
                    sb.append("\n");
                }
                String str = sb.toString();
                // 문장을 줄바꿈 기준으로 자르고 배열에 하나씩 넣는다.
                String[] array = str.split("\n");

                // 배열을 한줄 한줄 읽는다.
                for (int i = 0; i < array.length; i++) {
                    if (array[i].equals("1000")) {
                        txt.setText("1000원 지폐가 인식되었습니다.");
                        myTTS.speak("천원 입니다. 다시 촬영하시려면 왼쪽 하단 부분을 터치해주세요. 다시 들으시려면 오른쪽 하단 부분을 터치해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                        cnt++;
                    } else if (array[i].equals("5000")) {
                        txt.setText("5000원 지폐가 인식되었습니다.");
                        myTTS.speak("오천원 입니다. 다시 촬영하시려면 왼쪽 하단 부분을 터치해주세요. 다시 들으시려면 오른쪽 하단 부분을 터치해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                        cnt++;
                    } else if (array[i].equals("10000")) {
                        txt.setText("10000원 지폐가 인식되었습니다.");
                        myTTS.speak("만원 입니다. 다시 촬영하시려면 왼쪽 하단 부분을 터치해주세요. 다시 들으시려면 오른쪽 하단 부분을 터치해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                        cnt++;
                    } else if (array[i].equals("50000")) {
                        txt.setText("50000원 지폐가 인식되었습니다.");
                        myTTS.speak("오만원 입니다. 다시 촬영하시려면 왼쪽 하단 부분을 터치해주세요. 다시 들으시려면 오른쪽 하단 부분을 터치해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                        cnt++;
                    }
                    // 인식률 저하 및 선택된 사진을 분석하지 못하기 때문에 차후에 생긴 버그에 대해서 수정해야함
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(cnt >= 2) {
            txt.setText("지폐 두장이 인식되었습니다. 한장만 촬영해주세요.");
            myTTS.speak("지폐 두장이 인식되었습니다. 한장만 촬영해주세요. 다시 촬영하시려면 왼쪽 하단 부분을 터치해주세요. 다시 들으시려면 오른쪽 하단 부분을 터치해주세요.", TextToSpeech.QUEUE_FLUSH, null);
        }
        else if(cnt == 0) {
            txt.setText("지폐가 인식되지 않았습니다. 다시 촬영해주세요.");
            myTTS.speak("지폐가 인식되지 않았습니다. 다시 촬영해주세요. 다시 촬영하시려면 왼쪽 하단 부분을 터치해주세요. 다시 들으시려면 오른쪽 하단 부분을 터치해주세요.", TextToSpeech.QUEUE_FLUSH, null);
        }
        cnt = 0;
    }
    public void setProfileImage(File f, ImageView v){ //이미지 출력
        int inSampleSize = 1;

        //사진 옵션 값
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; //이미지크기만 가져옴
        try { //f => 파일경로 받은거를 읽어옴
            InputStream in = new FileInputStream(f);
            //BitmapFactory => 이미지 형식으로 사용할 수 있게 불러옴
            BitmapFactory.decodeStream(in, null, options);
            in.close();
            in = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // width, height 값에 따라 inSaampleSize 값 계산
        BitmapFactory.Options imgOptions = new BitmapFactory.Options();
        imgOptions.inSampleSize = inSampleSize;

        //bitmap = 파일 생성경로 확인 후 파일 생성
        bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), imgOptions);
        try{ //사진 메타 데이터 생성 후 가져오기
            ExifInterface exif = new ExifInterface(f.getPath());
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int exifDegree = exifOrientationToDegrees(exifOrientation);
            bitmap = rotate(bitmap,exifDegree);
        }catch(Exception e){
            //   Log.d(Common.TAG,"Error");
        }

        v.setImageBitmap(bitmap);
        v.setScaleType(ImageView.ScaleType.FIT_XY);
        img.setImageBitmap(bitmap);
        getTextFromImage(bitmap);
    }

    /**
     * EXIF정보를 회전각도로 변환하는 메서드
     *
     * @param exifOrientation EXIF 회전각
     * @return 실제 각도
     */
    public static int exifOrientationToDegrees(int exifOrientation) {
        if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    /**
     * 이미지를 회전시킵니다.
     *
     * @param bitmap 비트맵 이미지
     * @param degrees 회전 각도
     * @return 회전된 이미지
     */
    public static Bitmap rotate(Bitmap bitmap, int degrees) { //회전하기전의 bitmap을 받아서 회전을 한 bitmap을 반환
        if(degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2,
                    (float) bitmap.getHeight() / 2);

            try {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if(bitmap != converted) {
                    bitmap.recycle();
                    bitmap = converted;
                }
            }
            catch(OutOfMemoryError ex) {
                // 메모리가 부족하여 회전을 시키지 못할 경우 그냥 원본을 반환합니다.
            }
        }
        return bitmap;
    }

    private boolean checkPermissions() {      // 단말기에 필요한 권한이 승인되었는지 확인
        int result;
        List<String> permissionList = new ArrayList<>();
        for (String pm : permissions) {
            result = ContextCompat.checkSelfPermission(this, pm);
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(pm);
            }
        }
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(this.permissions[i])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showToast_PermissionDeny();
                            }
                        }
                    }
                }
                else {
                    showToast_PermissionDeny();
                }
                return;
            }
        }

    }

    private void showToast_PermissionDeny() {
        Toast.makeText(this, "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    //앱종료시 tts를 같이 종료해 준다. (menu2 소스 참고함)
    @Override
    protected void onDestroy() {
        if (myTTS != null) {
            myTTS.stop();
            myTTS.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myTTS.stop();
    }
}