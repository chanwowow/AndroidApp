package com.example.clicker

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.*
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.util.*


class Service : Service() {
    // 서비스를 실행하고 , 서비스 안에서 accessibility servie 메소드들을 사용하는 방식으로 설계

    private lateinit var statusText : TextView
    private lateinit var telephonyManager: TelephonyManager
    private var timer: Timer? = null

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()

        // 테스트용 출력 메세지
        val message ="Service onCreate() 호출"
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

        // [0419] 1. 일단 노티를 만들자

        // [0419] 2. 오버레이 뷰를 띄우자
            // Inflater를 사용해 layout 가져옴
        val inflate = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        // 윈도우매니저 설정
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        //OverlayView 가 띄워지는 위치 지정
        params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL

        // 위치 지정
        val mView: View = inflate.inflate(R.layout.view_in_service, null)
        // view_in_service.xml layout 불러오기

        wm.addView(mView, params) // 윈도우에 layout 을 추가 한다.
        /// ^오버레이 뷰에 관한 내용

        statusText=mView.findViewById(R.id.StatusText)

        // Telephony 내용
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        registerTelephonyCallback(telephonyManager)
        ////

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
                    statusText.text =stateStr + networkTypeChecker()
                }
            })

    }


    // 이 함수를 통해서 RAT 정보도 파악해서 출력할거다
    // 오 이렇게 함수 이름 옆에 형식 쓰는게 리턴 타입이구나
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