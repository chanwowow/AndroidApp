package com.example.clicker

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.TextView
import android.widget.Toast
import java.util.*
import kotlin.concurrent.fixedRateTimer

class Service : Service() {
    // 서비스를 실행하고 , 서비스 안에서 accessibility servie 메소드들을 사용하는 방식으로 설계

    private var timer: Timer? = null


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()

        // 테스트용 출력 메세지
        val message ="SVC On Create"
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

    }

    override fun onDestroy() {
        super.onDestroy()
        "FloatingClickService onDestroy".logd()
        timer?.cancel()
        //manager.removeView(view)
    }

//    private fun viewOnClick() {
//        if (isOn) {
//            timer?.cancel()
//        } else {
//            timer = fixedRateTimer(initialDelay = 0,
//                period = 200) {
//                view.getLocationOnScreen(location)
//                autoClickService?.click(location[0] + view.right + 10,
//                    location[1] + view.bottom + 10)
//            }
//        }
//        isOn = !isOn
//        (view as TextView).text = if (isOn) "ON" else "OFF"
//
//    }


}