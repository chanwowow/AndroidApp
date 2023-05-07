package com.example.calltesthelper

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        // Setting 값 설정
        val ratSpinner = findViewById<Spinner>(R.id.spinnerRat)
        val autoAnsSwitch = findViewById<Switch>(R.id.switchAutoans)
        val periodTextInput = findViewById<EditText>(R.id.periodInput)
        val startButton = findViewById<Button>(R.id.buttonStart)
        val stopButton = findViewById<Button>(R.id.buttonStop)
        val serviceIntent = Intent(this@MainActivity, ForegroundService::class.java)

        // 1. 서비스 시작
        startButton.setOnClickListener {
            val ratType = ratSpinner.selectedItem.toString()
            val autoAns = autoAnsSwitch.isChecked
            val period = Integer.parseInt(periodTextInput.text.toString())

            serviceIntent.putExtra("RAT", ratType)
            serviceIntent.putExtra("AutoAnswer", autoAns)
            serviceIntent.putExtra("Period", period)

            // 1-1. 오버레이 권한 체크 후 실행
            if(Settings.canDrawOverlays(this)) {
                startForegroundService(serviceIntent)
                moveTaskToBack(true)
            }
            else{
                permissionCheckRequest()
            }
        }

        // 2. 서비스 종료
        stopButton.setOnClickListener {
            stopService(serviceIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        permissionCheckRequest()

        val hasPermission = checkAccess()
        if (!hasPermission) {
            // 접근성 권한설정
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun checkAccess(): Boolean {
        // accessibility 켜졌는지 확인
        val string = getString(R.string.accessibility_service_id)
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)

        for (id in list) {
            if (string == id.id)
                return true
        }
        return false
    }

    private fun permissionCheckRequest(){
        // 1. 오버레이 권한
        if(!Settings.canDrawOverlays(this)){
            Toast.makeText(this, "Overlay Permission is required", Toast.LENGTH_SHORT).show()
            val permissionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(permissionIntent,110) // 여기 request code 는 이게 맞나?
        }
        // 2. 휴대폰 상태 읽기 권한
        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Phone Read Permission is required", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 123)
        }
        // 3. 전화 자동응답 권한
        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ANSWER_PHONE_CALLS)!= PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Call Answer Permission is required", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), 124)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy()")
    }
}