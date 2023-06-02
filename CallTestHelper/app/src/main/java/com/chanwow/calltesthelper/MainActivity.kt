package com.chanwow.calltesthelper

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
        val ratSpinner = findViewById<Spinner>(R.id.spinnerRat)
        val autoAnsSwitch = findViewById<Switch>(R.id.switchAutoans)
        val periodTextInput = findViewById<EditText>(R.id.periodInput)
        val startButton = findViewById<Button>(R.id.buttonStart)
        val stopButton = findViewById<Button>(R.id.buttonStop)
        val serviceIntent = Intent(this@MainActivity, ForegroundService::class.java)

        // 1. 서비스 시작
        startButton.setOnClickListener {
            // 권한 체크 후 실행
            if (!(permissionOthers() && permissionOverlay() && permissionAccess()))
                return@setOnClickListener

            val ratType = ratSpinner.selectedItem.toString()
            val autoAns = autoAnsSwitch.isChecked
            val period = Integer.parseInt(periodTextInput.text.toString())

            serviceIntent.putExtra("RAT", ratType)
            serviceIntent.putExtra("AutoAnswer", autoAns)
            serviceIntent.putExtra("Period", period)

            startForegroundService(serviceIntent)
            moveTaskToBack(true)
        }
        // 2. 서비스 종료
        stopButton.setOnClickListener {
            stopService(serviceIntent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (permissionOthers()) {
            if (permissionOverlay()) {
                if (!permissionAccess()) {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }
    }

    private fun permissionAccess(): Boolean {
        // accessibility 서비스 권한
        val string = getString(R.string.accessibility_service_id)
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)

        for (id in list) {
            if (string == id.id)
                return true
        }
        return false
    }

    private fun permissionOverlay() : Boolean {
        // 오버레이 권한
        if (!Settings.canDrawOverlays(this)){
            val permissionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(permissionIntent,110)
            return false
        }
        else return true
    }

    private fun permissionOthers() : Boolean {
        var checker = true
        // 1. 전화 상태 읽기, 응답 권한
        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS), 123)
            checker = false
        }
        // 2. Fine Location 권한
        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 104)
            checker = false
        }
//        // 3. BackGround Location 권한
//        if(ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.ACCESS_BACKGROUND_LOCATION)!= PackageManager.PERMISSION_GRANTED){
//            Toast.makeText(this, "Access Location Always Permission is required", Toast.LENGTH_SHORT).show()
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 126)
//            checker = false
//        }
        return checker
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy()")
    }
}