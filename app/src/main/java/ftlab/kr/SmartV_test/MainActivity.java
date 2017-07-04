package ftlab.kr.SmartV_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;

import kr.ftlab.lib.SmartSensor;
import kr.ftlab.lib.SmartSensorEventListener;
import kr.ftlab.lib.SmartSensorResultMDI;


public class MainActivity extends AppCompatActivity implements SmartSensorEventListener {
    private SmartSensor mMI;
    private SmartSensorResultMDI mResultMDI;

    private Button btnStart;
    private TextView txtResultV;

    int mProcess_Status = 0;//초기값은 0
    int Process_Stop = 0;//미결합일 경우 0
    int Process_Start = 1;//값을 1로 잡은 이유는 단자의 결합유무 확인

    private static final int UI_UPDATE = 0;

    BroadcastReceiver mHeadSetConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG)) { // 이어폰 단자에 센서 결함 유무 확인
                if (intent.hasExtra("state")) {
                    if (intent.getIntExtra("state", 0) == 0) {//센서 분리 시
                        stopSensing();
                        btnStart.setEnabled(false);//센서가 분리되면 START/STOP 버튼 비활성화, 클릭 불가
                        Toast.makeText(MainActivity.this, "Sensor not found", Toast.LENGTH_SHORT).show();
                    } else if (intent.getIntExtra("state", 0) == 1) {//센서 결합 시
                        Toast.makeText(MainActivity.this, "Sensor found", Toast.LENGTH_SHORT).show();
                        btnStart.setEnabled(true);//센서가 연결되면 START/STOP 버튼 활성화
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//어플이 실행되는 동안 스마트폰 디스플레이 항상 켜지도록 설정

        btnStart = (Button) findViewById(R.id.button);//start/stop 버튼 생
        txtResultV = (TextView) findViewById(R.id.VValue); //전류 값 텍스트뷰 생성

        mMI = new SmartSensor(MainActivity.this, this);
        mMI.selectDevice(SmartSensor.MDI);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//마시멜로 이상 버전에서는 권한을 별도로 요청함, 유저가 미허용시, 어플 사용 못함
            //checkPermission();
            PermissionListener permissionlistener = new PermissionListener() {
                @Override
                public void onPermissionGranted() {//유저가 정상적으로 권한 허용 했을 경우
                }

                @Override
                public void onPermissionDenied(ArrayList<String> deniedPermissions) {//유저가 권한 허용을 안했을 경우, 어플 사용 못하므로 어플 종료
                    finish();
                }
            };

            new TedPermission(this)//권한 확인 팝업창.. 허용시 onPermissionGranted,  거부 시 : onPermissionDenied
                    .setPermissionListener(permissionlistener)
                    .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                    .setPermissions(android.Manifest.permission.RECORD_AUDIO)
                    .check();
        }
    }

    public void mOnClick(View v) {//버튼을 클릭 하였을 때.  최초 설정은 mProcess_Status = Process_Stop
        if (mProcess_Status == Process_Start) {
            stopSensing();
        } else {//최초 버튼 클릭 시 Process_Stop 상태 이므로 측정 시작
            startSensing();
        }
    }

    Handler MeasureHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_UPDATE://측정 값이 전달 될때마다 결과 값 업데이트
                    String str = "";

                    mResultMDI = mMI.getResultMDI();//측정된 값을 가져옴

                    Log.d("test", "value : " + mResultMDI.MDI_Value);

                    str = String.format("%1.2f\n", mResultMDI.MDI_Value); // 측정값은 전류 값
                    txtResultV.setText(str);
                    break;
            }
        }
    };

    public void startSensing() {//측정 시작
        btnStart.setText("STOP");
        mProcess_Status = Process_Start;//현재 상태를 start로 설정
        mMI.start();//측정 시작
    }

    public void stopSensing() {//측정 종료
        btnStart.setText("START");
        mProcess_Status = Process_Stop;//현재 상태를 stop로 설정
        mMI.stop();//측정 종료
    }

    public boolean onCreateOptionsMenu(Menu menu) { //상단 메뉴 버튼 생성
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intflt = new IntentFilter();
        intflt.addAction(Intent.ACTION_HEADSET_PLUG);//오디오 등록
        this.registerReceiver(mHeadSetConnectReceiver, intflt);//센서 결합 유무 판단 위해 BroadcastReceiver 등록
    }

    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(mHeadSetConnectReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() { //앱 종료 시 수행
        mMI.quit();
        finish();
        System.exit(0);
        super.onDestroy();
    }

    @Override
    public void onMeasured() {
        MeasureHandler.sendEmptyMessage(UI_UPDATE);
    }

    @Override
    public void onSelfConfigurated() {
        mProcess_Status = 0;
        btnStart.setText("START");
    }
}