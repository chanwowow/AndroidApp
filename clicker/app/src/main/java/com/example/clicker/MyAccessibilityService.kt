package com.example.clicker

import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent


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


}

