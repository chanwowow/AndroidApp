package com.example.clicker

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var stopButton : Button
    private lateinit var airplaneButton : Button
    private lateinit var switchButton1 : Switch
    private lateinit var switchButton2 : Switch
    private var autoAns = false
    private var showState = false

    private var serviceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionCheckRequest()

        airplaneButton=findViewById (R.id.airplaneModeSet)
        airplaneButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
        }

        startButton = findViewById (R.id.startButtonMain)
        startButton.setOnClickListener {
            val inputPeriod = findViewById<EditText>(R.id.inputPeriod)
            var period = Integer.parseInt(inputPeriod.text.toString())

            // 1. 오버레이 권한이 있으면 아래 실행
            if(Settings.canDrawOverlays(this)){
                serviceIntent = Intent(this@MainActivity, Service::class.java)
                serviceIntent!!.putExtra("period",period )
                serviceIntent!!.putExtra("AutoAnswer", autoAns)
                serviceIntent!!.putExtra("showState", showState)

                startService(serviceIntent)
                onBackPressed()
            }
            //2. 오버레이 권한 없다면 먼저 권한 신청
            else{
                permissionCheckRequest()
            }
        }

        stopButton=findViewById(R.id.stopButtonMain)
        stopButton.setOnClickListener {
            stopService(serviceIntent)
        }

        switchButton1 = findViewById(R.id.switch1)
        switchButton1.setOnCheckedChangeListener { buttonView, isChecked ->
            autoAns = isChecked
        }
        switchButton2 = findViewById(R.id.switch2)
        switchButton2.setOnCheckedChangeListener{buttonView, isChecked->
            showState = isChecked
        }
    }

    private fun checkAccess(): Boolean {
        // accessibility svc 중 해당 app 켜졌는지 확인 후 true/false 반환
        val string = getString(R.string.accessibility_service_id)
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (id in list) {
            if (string == id.id)
                return true
        }
        return false
    }

    // 이 부분은 오버레이 권한 설정 후 돌아왔을때도 다시 체크되는 방식
  override fun onResume() {
        super.onResume()
        permissionCheckRequest()

        val hasPermission = checkAccess()
        if (!hasPermission) {
            // 접근성 권한설정
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("test", "stop")
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("test", "destroy")
    }

    private fun permissionCheckRequest(){
        // 1. 오버레이 권한 신청
        if(!Settings.canDrawOverlays(this)){
            Toast.makeText(this, "Overlay Permission is required", Toast.LENGTH_SHORT).show()
            val permissionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(permissionIntent,110) // 여기 request code 는 이게 맞나?
        }
        // 2. 휴대폰 상태 읽기 권한 신청
        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Phone Read Permission is required", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 123)
        }
        // 3. 전화 자동응답 권한 신청
        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ANSWER_PHONE_CALLS)!= PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Call Answer Permission is required", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), 124)
        }

    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

}
