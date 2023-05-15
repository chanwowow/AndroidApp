package com.example.calltesthelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.*
import kotlin.concurrent.fixedRateTimer

class ForegroundService : Service() {

    companion object{
        const val CHANNEL_ID = "ForegroundServiceChannel"
    }

    //1. 보여지는것들 관련
    private lateinit var overlay : WindowManager
    private lateinit var networkInfoView : View
    private lateinit var svcStateView : TextView
    private lateinit var floatingBtn : View

    private var startDragDistance:Int = 0
    private val xyLocation = IntArray(2)

    private lateinit var infoViewParams : WindowManager.LayoutParams
    private lateinit var floatingBtnViewParams : WindowManager.LayoutParams

    //2. 네트워크 관련
    private lateinit var telephony : MyTelephony
    private var svcStateStr = "NO Service"
    private var RATStr = "Unknown"

    private var svcCheckTimer : Timer? = null
    private var callCheckTimer : Timer? = null
    private var periodTimer : Timer? = null

    private var period = 30
    private var ratType = "Legacy"

    //3. 기타 설정
    private lateinit var wakeLock : WakeLock
    private var isOn =false

    override fun onCreate() {
        super.onCreate()

        // 1. 플로팅 뷰 설정
        networkInfoView = LayoutInflater.from(this).inflate(R.layout.foreground_service, null)
        svcStateView = networkInfoView.findViewById(R.id.stateText)
        floatingBtn = LayoutInflater.from(this).inflate(R.layout.floating_button, null)

        infoViewParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // 터치 이벤트 수신x -> 다음 layer로 전달됨
            PixelFormat.TRANSLUCENT)
        floatingBtnViewParams =WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT)

        overlay = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlay.addView(floatingBtn, floatingBtnViewParams)

        // 3. 기타 설정
        // 플로팅 버튼 터치 리스너 등록
        startDragDistance = dp2px(10f)
        floatingBtn.setOnTouchListener(
            TouchDragListener(this,
            floatingBtnViewParams,startDragDistance,
            {viewOnClick()}, {overlay.updateViewLayout(floatingBtn,floatingBtnViewParams)}))

        // Screen Wake Lock (서비스 실행 중 화면꺼짐 방지)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "CallTestHelper:WL")

        // Telephony객체 생성
        telephony = MyTelephony(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ForegroundService", "onStartCommand")

        // 1. notification Channel 생성과 시작
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Test Helper")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        startForeground(1, notification)

        // 2. intent 값 복사
        ratType = intent?.getStringExtra("RAT")!!
        period = intent.getIntExtra("Period", 30)

        val autoAns = intent.getBooleanExtra("AutoAnswer", false )
        callCheckTimer = if (autoAns){
            fixedRateTimer(period = 3000){
                telephony.callCheckAndTake()
            }
        } else null

        return super.onStartCommand(intent, flags, startId)
    }

    private fun viewOnClick() {
        if (!isOn) {
            overlay.addView(networkInfoView,infoViewParams)
            wakeLock.acquire() // WakeLock ON

            when (ratType){
                "Legacy" -> legacyRatRoutine()
                "NR (NSA)" -> NSARoutine()
                "NR (SA)" -> SARoutine()
            }

            periodTimer = fixedRateTimer(initialDelay = 200, period = (period*1000).toLong()){
                if (svcStateStr == "NO Service"){
                    noSvcHandler()
                }
            }

        } else
            clearAndStopService()

        isOn = !isOn
        (floatingBtn as TextView).text = if (isOn) "ON " else "OFF"
        (floatingBtn as TextView).setTextColor( if (isOn) Color.CYAN else Color.GRAY )
    }

    private fun legacyRatRoutine(){
        var cnt = 0
        svcCheckTimer = fixedRateTimer(period = 3000){
            svcStateStr = telephony.getSvcState()
            RATStr = telephony.getRAT()
            Handler(Looper.getMainLooper()).post {
                svcStateView.text = RATStr + "\n\n" + svcStateStr + " "+ cnt++
            }
        }
    }
    private fun NSARoutine(){
        svcCheckTimer = fixedRateTimer(period = 3000){
            RATStr = "NR (NSA)"
            svcStateStr = if (telephony.checkEndc()) "IN Service" else "NO Service"
            Handler(Looper.getMainLooper()).post {
                svcStateView.text = RATStr + "\n\n" + svcStateStr
            }
        }
    }
    private fun SARoutine(){
        svcCheckTimer = fixedRateTimer(period = 3000){
            RATStr = telephony.getRAT()
            svcStateStr = if(RATStr == "NR") "IN Service" else "NO Service"
            Handler(Looper.getMainLooper()).post {
                svcStateView.text = RATStr + "\n\n" + svcStateStr
            }
        }
    }

    private fun noSvcHandler(){
        Thread{
            CLKSVC?.swipeAction(200,20,200, 700)
            Thread.sleep(1000)

            var isAirplaneModeOn = Settings.Global.getInt(
                this@ForegroundService.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,0
            ) != 0
            while(!isAirplaneModeOn && isOn){
                floatingClick()
                Thread.sleep(1500)

                // 비행기 모드 켜졌는지 체크
                isAirplaneModeOn = Settings.Global.getInt(
                    this@ForegroundService.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,0
                ) != 0
            }
            floatingClick()
            Thread.sleep(1000)
            CLKSVC?.backPress()
        }.start()
    }

    private fun floatingClick(){
        if(!isOn) return
        floatingBtn.getLocationOnScreen(xyLocation)
        CLKSVC?.clickAction(
            xyLocation[0] + floatingBtn.left -1,
            xyLocation[1] + floatingBtn.bottom -80)
    }

    private fun clearAndStopService(){
        overlay.removeView(networkInfoView)
        wakeLock.release()
        svcCheckTimer?.cancel()
        periodTimer?.cancel()
        callCheckTimer?.cancel()
    }

    override fun onDestroy() {
        showToastInService("Call Test Helper is terminated!")
        super.onDestroy()
        overlay.removeView(floatingBtn)
        if(isOn) clearAndStopService()
    }

    private fun showToastInService(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}