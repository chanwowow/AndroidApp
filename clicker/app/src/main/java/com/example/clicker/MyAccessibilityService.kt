package com.example.clicker

import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.PointF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.annotation.RequiresApi


var CLKSVC : MyAccessibilityService? = null // 이게 객체를 생성하는 부분인가? 역할이 뭘까

class MyAccessibilityService : AccessibilityService() {

    override fun onInterrupt() {
        // 뭔가 잘못될때
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        "Event type: ${event.eventType} Package name: ${event.packageName}".logd()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        CLKSVC = this
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun clickAction(x:Int, y:Int){
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val builder = GestureDescription.Builder()
        val gestureDescription = builder
            .addStroke(GestureDescription.StrokeDescription(path, 10, 10))
            .build()
        dispatchGesture(gestureDescription, null, null)
        "dispatchGesture() $x, $y ".logd()
    }

    fun backPress(){
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun onGesture(gestureEvent: AccessibilityGestureEvent): Boolean {
        "onGesture Catch!".logd()
        return true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        CLKSVC = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        CLKSVC =null
        super.onDestroy()
        //on Destroy
    }

//    fun expandStatusBar() {
//        val handler = Handler(Looper.getMainLooper())
//        val path = Path()
//        val displayMetrics = resources.displayMetrics
//        val x = displayMetrics.widthPixels / 2f
//        val startY = 0f
//        val endY = displayMetrics.heightPixels / 5f
//        path.moveTo(x, startY)
//        path.lineTo(x, endY)
//        val gestureBuilder = GestureDescription.Builder()
//        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0L, 200L))
//        val gesture = gestureBuilder.build()
//        dispatchGesture(gesture, object : GestureResultCallback() {
//            override fun onCompleted(gestureDescription: GestureDescription) {
//                super.onCompleted(gestureDescription)
//                handler.postDelayed({
//                    val rootNode = rootInActiveWindow
//                    val notificationPanelView = findNotificationPanelView(rootNode)
//                    notificationPanelView?.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
//                }, 300L)
//            }
//        }, null)
//    }


}

