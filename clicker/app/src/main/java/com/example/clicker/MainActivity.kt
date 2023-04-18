package com.example.clicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import com.example.clicker.MyAccessibilityService


class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button

    private var serviceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById (R.id.allowPermission)
        startButton.setOnClickListener {
            // 1. 오버레이 권한이 있으면 아래 실행
            if(Settings.canDrawOverlays(this)){

                serviceIntent = Intent(this@MainActivity, Service::class.java)
                startService(serviceIntent)
                onBackPressed()

            }//2. 오버레이 권한 없다면 먼저 권한 신청
            else{
                askPermission()
                Toast.makeText(this, "Overlay Permission is needed", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun checkAccess(): Boolean {
        // accessibility svc 중 해당 app 켜졌는지 확인 후 true/false 반환
        val string = getString(R.string.accessibility_service_id)
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (id in list) {
            if (string == id.id) {
                return true
            }
        }
        return false
    }

    // 이 부분은 오버레이 권한 설정 후 돌아왔을때도 다시 체크되는 방식
  override fun onResume() {
        super.onResume()
        val hasPermission = checkAccess()
        if (!hasPermission) {
            // 접근성 권한설정
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        else if(!Settings.canDrawOverlays(this)){
            askPermission()
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


    private fun askPermission() {
        val permissionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"))
        startActivityForResult(permissionIntent,110) // 여기 request code 는 이게 맞나?
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

}
