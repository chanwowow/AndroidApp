package com.example.clicker

import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.app.Service
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
import android.provider.Settings
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
    private var timer: Timer? = null
    private var cnt =0

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
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.addView(floatingBtn, paramsForFloatingBtn )

        floatingBtn.setOnTouchListener(TouchDragListener(paramsForFloatingBtn,startDragDistance,
            {viewOnClick()},
            {wm.updateViewLayout(floatingBtn,paramsForFloatingBtn)}))

//        infoView.setOnTouchListener{ view, event ->
//            view.performClick()
//            false
//        }   => 0421  뭐 이런거로 현재 레이어 터치를 무시하면 다음레이어로 넘어간다나? 근데 동작 안하는데?
    }

    private var isOn =false
    private fun viewOnClick() {
        if (isOn) {
            wm.removeView(infoView)
            timer?.cancel()
        } else {
            wm.addView(infoView, paramsForInfo)
            noSvcTimer()
        }
        isOn = !isOn
        (floatingBtn as TextView).text = if (isOn) "ON" else "OFF"

    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // 테스트용 출력 메세지
        Toast.makeText(applicationContext, "Service onStartCommand()", Toast.LENGTH_SHORT).show()

        // Telephony 내용
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        registerTelephonyCallback(telephonyManager)
        return START_STICKY
    }

    // 이 부분 reuse 방법 생각해보자
    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback(telephonyManager: TelephonyManager){
        telephonyManager.registerTelephonyCallback(
            mainExecutor,
            object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                @SuppressLint("SuspiciousIndentation")
                override fun onServiceStateChanged(serviceState: ServiceState) {
                    val stateStr :String
                    when (serviceState.state) {
                        ServiceState.STATE_IN_SERVICE->{
                            stateStr= "IN SVC ..."
                            //outOfServiceTime=null
                        }
                        ServiceState.STATE_OUT_OF_SERVICE->{
                            stateStr = "OUT OF SVC"
                        }
                        ServiceState.STATE_EMERGENCY_ONLY-> {
                            stateStr = "EMERGENCY ONLY"
                            //outOfServiceTime=null
                        }
                        ServiceState.STATE_POWER_OFF->{
                            stateStr="비행기모드 상태"
                        }
                        else->{
                            stateStr="INSERT SIM"
                        }
                    }
                    statusText.text =networkTypeChecker() + "\n\n" + stateStr + cnt++
                }
            })

    }

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

    private fun noSvcTimer(){

        timer = fixedRateTimer(initialDelay = 0,period = 1000) {
            // 1. 비행기모드 설정 열고
            val intentAirplane = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            intentAirplane.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            //startActivity(intentAirplane)

            // 2. 터치 액션 실행
            floatingBtn.getLocationOnScreen(xyLocation)
            CLKSVC?.clickAction(
                xyLocation[0] + floatingBtn.right + 10,
                xyLocation[1] + floatingBtn.bottom + 10)


            //3. 1초 후 비행기모드 설정 닫기

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
        // 테스트용 출력 메세지
        Toast.makeText(applicationContext, "Service onDestroy()", Toast.LENGTH_SHORT).show()
        //manager.removeView(view)
    }

}