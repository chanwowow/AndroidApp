package com.example.clicker

import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.KeyEvent
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.text.Layout
import android.text.method.Touch
import android.view.*
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.view.KeyEventDispatcher
import androidx.core.view.KeyEventDispatcher.dispatchKeyEvent
import androidx.core.view.ViewCompat.OnUnhandledKeyEventListenerCompat
import androidx.core.view.isVisible
import java.security.Key
import java.util.*
import kotlin.concurrent.fixedRateTimer

class Service : Service() {
    // 서비스를 실행하고 , 서비스 안에서 accessibility servie 메소드들을 사용하는 방식으로 설계

    private lateinit var wm : WindowManager
    private lateinit var statusText : TextView
    private lateinit var floatingBtn: View
    private lateinit var infoView: View

    private lateinit var paramsForInfo : WindowManager.LayoutParams
    private lateinit var paramsForFloatingBtn : WindowManager.LayoutParams

    private var startDragDistance:Int =0
    private var xForRecord = 0
    private var yForRecord = 0
    private val xyLocation = IntArray(2)

    private lateinit var telephonyManager: TelephonyManager
    private var networkStateStr = "NO SVC"
    private var timer: Timer? = null
    private var cnt =0

    // For Screen Wake Lock
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock : WakeLock
    ////
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()
        startDragDistance = dp2px(10f)

        infoView=LayoutInflater.from(this).inflate(R.layout.view_in_service, null)
        floatingBtn = LayoutInflater.from(this).inflate(R.layout.floating_button, null)
        //  layout 불러오기

        statusText=infoView.findViewById(R.id.StatusText)

        // [0419] 2. 오버레이 뷰를 띄우자
        paramsForInfo = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT)
        paramsForInfo.gravity =Gravity.DISPLAY_CLIP_HORIZONTAL or Gravity.TOP  //INFO가 띄워지는 위치 지정

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

//        floatingBtn.setOnLongClickListener{
//            //
//        }


        // Screen Wake Lock
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Clicker::WakeLock><")
        ///

    }

    private var isOn =false
    private fun viewOnClick() {
        if (!isOn) {
            wm.addView(infoView, paramsForInfo)
            wakeLock.acquire() // WakeLock ON

            "Timer run".logd()
            timer = fixedRateTimer(initialDelay = 200,
                period = 30000){
                "Timer run".logd()
                if(networkStateStr=="NO SVC"){
                    noSvcHandler()
                }
            }
        } else {
            wakeLock.release() // WakeLock OFF
            wm.removeView(infoView)
            timer?.cancel()
        }
        isOn = !isOn
        (floatingBtn as TextView).text = if (isOn) "ON" else "OFF"
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Telephony 내용
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        registerTelephonyCallback(telephonyManager)

        return START_STICKY
    }


    @SuppressLint("RestrictedApi")
    private fun noSvcHandler(){
        Thread.sleep(500)

        // 1. 비행기모드 설정 열고
        val intentAirplane = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        intentAirplane.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intentAirplane)

        Thread.sleep(500)

        // 2. 터치 액션 실행
        floatingBtn.getLocationOnScreen(xyLocation)
        CLKSVC?.clickAction(
            xyLocation[0] + floatingBtn.right +10,
            xyLocation[1] + floatingBtn.bottom -50)

        // 비행기모드 켜졌는지 확인
        var isAirplaneModeOn = Settings.Global.getInt(
            this@Service.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0


        // 진짜 켜지면 빠져나옴
        while(!isAirplaneModeOn){
            Thread.sleep(1000)
            isAirplaneModeOn = Settings.Global.getInt(
                this@Service.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0
        }

        Thread.sleep(1000)

        //3. 2초 후 비행기모드 끄고 설정화면 닫기
        floatingBtn.getLocationOnScreen(xyLocation)
        CLKSVC?.clickAction(
            xyLocation[0] + floatingBtn.right +10,
            xyLocation[1] + floatingBtn.bottom -50)
        Thread.sleep(200)

        CLKSVC?.backPress()
    }

    // 이 부분 reuse 방법 생각해보자
    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback(telephonyManager: TelephonyManager){
        telephonyManager.registerTelephonyCallback(
            mainExecutor,
            object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                @SuppressLint("SuspiciousIndentation")
                override fun onServiceStateChanged(serviceState: ServiceState) {

                    when (serviceState.state) {
                        ServiceState.STATE_IN_SERVICE->{
                            networkStateStr= "IN SVC!"
                            //outOfServiceTime=null
                        }
                        ServiceState.STATE_OUT_OF_SERVICE->{
                            networkStateStr = "NO SVC"
                        }
                        ServiceState.STATE_EMERGENCY_ONLY-> {
                            networkStateStr = "EMERGENCY ONLY"
                            //outOfServiceTime=null
                        }
                        ServiceState.STATE_POWER_OFF->{
                            networkStateStr="비행기모드 상태"
                        }
                        else->{
                            networkStateStr="INSERT SIM"
                        }
                    }
                    statusText.text =networkTypeChecker() + "\n\n" + networkStateStr + cnt++
                }
            })

    }
    // Telephony Part
    private fun networkTypeChecker(): String {

        val networkType : Int = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) return "Needs Permission"
        // -> 권한 이미 얻었다면 아래 쭉 실행한다.
        else   telephonyManager.dataNetworkType

        // 위에서 읽은 NW 타입을 Str으로 변환 반환
        return when(networkType){
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"
            TelephonyManager.NETWORK_TYPE_UMTS -> "WCDMA"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_NR -> "NR"
            else -> "UKNWON"
        }
    }

    // 이거 필요없을수도 있다.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val x = paramsForFloatingBtn.x
        val y = paramsForFloatingBtn.y
        paramsForFloatingBtn.x = xForRecord
        paramsForFloatingBtn.y = yForRecord
        xForRecord = x
        yForRecord = y
        wm.updateViewLayout(floatingBtn, paramsForFloatingBtn)
    }

    override fun onDestroy() {
        super.onDestroy()
        "FloatingClickService onDestroy".logd()
        timer?.cancel()
        wm.removeView(floatingBtn)
        wm.removeView(infoView)
    }

}