package com.example.calltesthelper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

var CLKSVC : MyAccessibilityService? = null

class MyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()

        CLKSVC = this
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // 입력 좌표 Click Action 수행 메소드
    fun clickAction(x:Int, y:Int){
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val builder = GestureDescription.Builder()
        val gestureDescription = builder
            .addStroke(GestureDescription.StrokeDescription(path, 10, 10))
            .build()
        dispatchGesture(gestureDescription, null, null)
    }

    // 입력 2개 좌표 사이를 Swipe Action 수행 메소드
    fun swipeAction(startX: Int, startY: Int, endX: Int, endY: Int){
        val swipePath = Path()
        swipePath.moveTo(startX.toFloat(), startY.toFloat())
        swipePath.lineTo(endX.toFloat(), endY.toFloat())
        val builder = GestureDescription.Builder()
        val gestureDescription = builder
            .addStroke(GestureDescription.StrokeDescription(swipePath, 10,100))
            .build()
        dispatchGesture(gestureDescription, null,null)
    }

    // 뒤로가기 버튼 1회 수행 메소드
    fun backPress(){
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun onInterrupt() {
        // no op
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // no op
    }

    override fun onUnbind(intent: Intent?): Boolean {
        CLKSVC = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        CLKSVC =null
        super.onDestroy()
    }
}