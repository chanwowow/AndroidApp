package com.example.clicker

import android.app.*
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.os.PowerManager.WakeLock
import android.provider.Settings
import android.telephony.*
import android.view.*
import android.widget.TextView
import androidx.annotation.RequiresApi
import java.util.*
import kotlin.concurrent.fixedRateTimer

class Service : Service() {
    // 서비스를 실행하고 , 서비스 안에서 accessibility servie 메소드들을 사용하는 방식으로 설계

    private lateinit var wm : WindowManager
    lateinit var statusText : TextView
    lateinit var extraText : TextView
    private lateinit var infoView: View
    private lateinit var floatingBtn: View

    private lateinit var paramsForInfo : WindowManager.LayoutParams
    private lateinit var paramsForFloatingBtn : WindowManager.LayoutParams

    private var startDragDistance:Int =0
    private val xyLocation = IntArray(2)

    private var networkStateStr = "NO SVC"
    private var RAT = " "
    private var timer: Timer? = null
    private var checkTimer: Timer? = null
    private var cnt =0
    private var period : Int = 30
    private var autoAns : Boolean = false
    private var showState : Boolean = false

    // For Screen Wake Lock
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock : WakeLock
    ////

    @RequiresApi(Build.VERSION_CODES.S)
    private lateinit var Tel : MyTelephony

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()

        startDragDistance = dp2px(10f)

        // 1. InfoView Layout 및 WM Param 설정
        infoView=LayoutInflater.from(this).inflate(R.layout.view_in_service, null)
        statusText=infoView.findViewById(R.id.StatusText)
        extraText = infoView.findViewById(R.id.sigText)

        paramsForInfo = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // 터치 이벤트 수신x -> 다음 layer로 전달됨
            PixelFormat.TRANSLUCENT)
        paramsForInfo.gravity =Gravity.DISPLAY_CLIP_HORIZONTAL or Gravity.TOP  //INFO가 띄워지는 위치 지정

        // 2. Floating 버튼 Layout 및 WM Param 설정
        floatingBtn = LayoutInflater.from(this).inflate(R.layout.floating_button, null)

        paramsForFloatingBtn = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT)

        // 윈도우매니저 설정 및 버튼 표시 add
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.addView(floatingBtn, paramsForFloatingBtn )

        floatingBtn.setOnTouchListener(TouchDragListener(paramsForFloatingBtn,startDragDistance,
            {viewOnClick()},
            {wm.updateViewLayout(floatingBtn,paramsForFloatingBtn)}))

        // Screen Wake Lock (서비스 실행동안 화면꺼짐 방지)  => 얘넨 한번만 실행되어야 할거같은데 버튼누를때마다 생성지양
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Clicker::WakeLock><")
        ///

    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if(intent != null) {
            period  = intent.getIntExtra("period", 30)
            autoAns = intent.getBooleanExtra("AutoAnswer", false)
            showState = intent.getBooleanExtra("showState", false)
        }

        Tel = MyTelephony(this) // Telephony 객체 생성
        if(autoAns)
            Tel.registerAutoAnswer()
        //Tel.registerTelephonyCallback()  // Callback 방식사용시

        if (!showState) extraText.visibility = View.INVISIBLE

        return START_STICKY
    }

    private var isOn =false

    @RequiresApi(Build.VERSION_CODES.S)
    private fun viewOnClick() {
        if (!isOn) {
            wm.addView(infoView, paramsForInfo)
            wakeLock.acquire() // WakeLock ON

            checkTimer = fixedRateTimer(initialDelay = 200,
                period = 3000){
                networkStateStr = Tel.getState()
                RAT = Tel.getRAT()

                Handler(Looper.getMainLooper()).post {
                    statusText.text = RAT +"\n\n" + networkStateStr + cnt++
                    extraText.text = Tel.getSomething()
                }
            }

            timer = fixedRateTimer(initialDelay = 200,
                period = (period*1000 + 10000).toLong()
            ){

                if(networkStateStr=="NO SVC... "){
                    noSvcHandler()
                }
            }
        } else
            clearAndStopInfo()

        isOn = !isOn
        (floatingBtn as TextView).text = if (isOn) "ON" else "OFF"
    }

    private fun noSvcHandler(){
        // 1. 비행기모드 설정 열고
        val intentAirplane = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        intentAirplane.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intentAirplane)
        Thread.sleep(1000)

        // 비행기모드 진짜 켜지면 빠져나옴
        var isAirplaneModeOn =false
        while(!isAirplaneModeOn && isOn){
            floatingClick()
            Thread.sleep(1500)

            isAirplaneModeOn = Settings.Global.getInt(
                this@Service.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0
        }

        // 다시 비행기 끄고 빠져나옴
        floatingClick()
        Thread.sleep(1000)
        CLKSVC?.backPress()
    }

    private fun floatingClick(){
        floatingBtn.getLocationOnScreen(xyLocation)
        CLKSVC?.clickAction(
            xyLocation[0] + floatingBtn.right +10,
            xyLocation[1] + floatingBtn.bottom -50)
    }

    private fun clearAndStopInfo() {
        wakeLock.release() // WakeLock OFF
        wm.removeView(infoView)
        timer?.cancel()
        checkTimer?.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDestroy() {
        super.onDestroy()
        wm.removeView(floatingBtn)

        // ON 상태인 경우에는 다른것도 함께 지움
        if(isOn) {
            clearAndStopInfo()
            isOn=false
        }
        if(autoAns){
            Tel.unRegisterAutoAnswer()
            autoAns = false
        }
        //Tel.unRegisterTelephonyCallback() // Callback 방식 사용시
    }
}