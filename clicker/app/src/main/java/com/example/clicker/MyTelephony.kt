package com.example.clicker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.concurrent.Executor
import java.util.concurrent.Executors


@RequiresApi(Build.VERSION_CODES.S)
class MyTelephony(context:Context) {
    // 별도 Thead에서 동작하도록. 이게 맞나?
    private val executor: Executor = Executors.newSingleThreadExecutor()

    private var myTelephonyManager: TelephonyManager
    private lateinit var myTelecomManager : TelecomManager
    private var networkStateStr = "NO SVC"
    private val context: Context = context
    private var svcTemp : String = " "

    init {
        myTelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    fun getState():String {
        val serviceState : ServiceState? =
            if (ActivityCompat.checkSelfPermission(
                    context,Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) return "Needs Permission"
            else myTelephonyManager.serviceState

        getSomething() //
        serviceState.toString().logd()
        svcTemp = serviceState.toString()

        return when (serviceState!!.state) {
            ServiceState.STATE_IN_SERVICE -> "IN SVC  "
            ServiceState.STATE_OUT_OF_SERVICE -> "NO SVC...  "
            ServiceState.STATE_EMERGENCY_ONLY -> "EMERGENCY ONLY  "
            ServiceState.STATE_POWER_OFF -> "AirplaneMode  "
            else -> "INSERT SIM  "
        }
    }

    fun getRAT(): String {
        val networkType: Int =
            if (ActivityCompat.checkSelfPermission(
                    context,Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) return "Needs Permission"
            else myTelephonyManager.dataNetworkType

        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
            TelephonyManager.NETWORK_TYPE_UMTS -> "WCDMA"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_NR -> "NR"
            else -> "Unknown"
        }
    }

    fun getSomething() : String{
        val operator = myTelephonyManager.networkOperator
        return operator.toString() + "\n" + svcTemp
    }

    fun registerAutoAnswer(){
        myTelecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        myTelephonyManager.registerTelephonyCallback(
            executor,
            object :TelephonyCallback(), TelephonyCallback.CallStateListener{
                override fun onCallStateChanged(state: Int) {
                    when(state){
                        TelephonyManager.CALL_STATE_RINGING->{
                            if (ActivityCompat.checkSelfPermission(
                                    context, Manifest.permission.ANSWER_PHONE_CALLS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) return
                            else {
                                Thread.sleep(2000)
                                myTelecomManager.acceptRingingCall()
                            }

                        }
                    }
                }
            }
        )
    }
    fun unRegisterAutoAnswer(){
        myTelephonyManager.unregisterTelephonyCallback(TelephonyCallback())
    }
    @RequiresApi(Build.VERSION_CODES.S)
    fun registerTelephonyCallback() {
        myTelephonyManager.registerTelephonyCallback(
            executor,
            object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                override fun onServiceStateChanged(serviceState: ServiceState) {
                    when (serviceState.state) {
                        ServiceState.STATE_IN_SERVICE -> {
                            networkStateStr = "IN SVC!"
                        }
                        ServiceState.STATE_OUT_OF_SERVICE -> {
                            networkStateStr = "NO SVC"
                        }
                        ServiceState.STATE_EMERGENCY_ONLY -> {
                            networkStateStr = "EMERGENCY ONLY"
                        }
                        ServiceState.STATE_POWER_OFF -> {
                            networkStateStr = "Airplane ON"
                        }
                        else -> {
                            networkStateStr = "INSERT SIM"
                        }
                    }

                }
            })
    }
    fun getServiceState() : String{
        return networkStateStr
    }
    fun unRegisterTelephonyCallback(){
        myTelephonyManager.unregisterTelephonyCallback(TelephonyCallback())
    }

}