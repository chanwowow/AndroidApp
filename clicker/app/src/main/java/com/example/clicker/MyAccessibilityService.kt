package com.example.clicker

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
import android.widget.Toast
import androidx.annotation.RequiresApi



var CLKSVC : MyAccessibilityService? = null // 이게 객체를 생성하는 부분인가? 역할이 뭘까

class MyAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onInterrupt() {
        // 뭔가 잘못될때
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == ( AccessibilityEvent.TYPE_VIEW_CLICKED)) {
            // if (event?.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
//            val source = event.source ?: return
//            val bounds = Rect()
//            source.getBoundsInScreen(bounds)
//
//            val x = bounds.centerX()
//            val y = bounds.centerY()
//            val message = "Clicked at x:$x, y:$y"
            val message ="Clicked sth"

            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
//            handler.post {
//                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
//            }

            performClick(500F,1300F)

        }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onServiceConnected() {
        super.onServiceConnected()
        CLKSVC = this

        // user가 엑세서비스 설정에서 켜면 MainActivity실행함
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)

        //startActivity(Intent(this,MainActivity::class.java)) // 예제 깃헙 코드처럼 이렇게 바로는 안되나?

        val info =AccessibilityServiceInfo()
        info.apply {
        // Set the type of events that this service wants to listen to. Others
            // won't be passed to this service.
            //eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED
            // -> 이거 지정한것만 서비스로 가는거인데 나중에 써먹자
            eventTypes=AccessibilityEvent.TYPES_ALL_MASK

            // Set the type of feedback your service will provide.
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK


            notificationTimeout = 100
        }

        this.serviceInfo = info

//        performClick(500f, 1300f)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        CLKSVC = null
        return super.onUnbind(intent)
    }


    ////  4월 1일 클릭 관련 시도
    ///
    fun performClick(x: Float, y: Float) { // 530, 1330   / // 530 740
        val clickPoint = PointF(x, y)
        val clickDuration = 100L

        val clickPath = Path()
        clickPath.moveTo(clickPoint.x, clickPoint.y)
        val clickStroke = GestureDescription.StrokeDescription(clickPath, 0, clickDuration)

        val clickGesture = GestureDescription.Builder()
            .addStroke(clickStroke)
            .build()
        Log.d("Test", "test");

        dispatchGesture(clickGesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                // Gesture completed successfully
                val cc=1
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                // Gesture cancelled
                val bb=1
            }
        }, null)
    }


}

