package com.example.calltesthelper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.telephony.*
import androidx.core.app.ActivityCompat
import java.util.concurrent.Executors

class MyTelephony(context : Context) {
    private var myTelephonyManager: TelephonyManager
    private var myTelecomManager : TelecomManager
    private val context = context
    private val myExecutor = Executors.newSingleThreadExecutor()

    init {
        myTelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        myTelecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    }

    fun getSvcState():String {
        val serviceState : ServiceState? =
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) return "Permission is required"
            else myTelephonyManager.serviceState

        if (serviceState == null) return "INSERT SIM"

        return when (serviceState.state) {
            ServiceState.STATE_IN_SERVICE -> "IN Service"
            ServiceState.STATE_OUT_OF_SERVICE -> "NO Service"
            ServiceState.STATE_EMERGENCY_ONLY -> "Emergency Only"
            ServiceState.STATE_POWER_OFF -> "Airplane Mode"
            else -> "INSERT SIM"
        }
    }

    fun getRAT(): String { // RadioAccessTechnology kind checker
        val networkType: Int =
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) return "Permission is requireed"
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

    fun callCheckAndTake(){
        val callStatus: Int =
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ANSWER_PHONE_CALLS
                ) != PackageManager.PERMISSION_GRANTED
            ) 0
            else myTelephonyManager.callState
        if(callStatus == TelephonyManager.CALL_STATE_RINGING){
            Thread{
                Thread.sleep(2000)
                myTelecomManager.acceptRingingCall() // auto voiceCall take
            }.start()
        }
    }

    fun checkEndc() : Boolean{
        val allRatSignal = myTelephonyManager?.signalStrength
        val activeSignalList = allRatSignal?.getCellSignalStrengths()

        if (activeSignalList != null) {
            for (rat in activeSignalList) {
                when (rat.toString().getOrNull(18)) {
                    'N' -> return true // NR Cell Connected
                }
            }
        }
        return false
    }

    fun checkEndcLsi() : Boolean{
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return false
        else
            myTelephonyManager?.requestCellInfoUpdate( myExecutor, cellInfoCallback)

        return lsiNr
    }

    private var lsiNr = false
    private val cellInfoCallback = object : TelephonyManager.CellInfoCallback() {
        override fun onCellInfo(cellInfoList: MutableList<CellInfo>) {
            if (cellInfoList != null) {
                for (info in cellInfoList) {
                    if (info is CellInfoNr) {
                        lsiNr = true
                        return
                    }
                    lsiNr = false
                }
            }
        }
    }

//    fun checkEndcLsi_0521() : Boolean{
//        if (ActivityCompat.checkSelfPermission(
//                context,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) return false
//
//        val cellInfoList = myTelephonyManager.allCellInfo
//        if (cellInfoList != null) {
//            for (info in cellInfoList) {
//                if (info is CellInfoNr) return true
//            }
//        }
//        return false
//    }

//    fun getNrState() : String{
//        val cellInfoList =
//            if (ActivityCompat.checkSelfPermission(
//                    context, Manifest.permission.ACCESS_FINE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) return "Needs Permission"
//            else  myTelephonyManager.allCellInfo
//
//        for (cellInfo in cellInfoList) {
//            if (cellInfo is CellInfoNr) {
//                val nrCellIdentity = cellInfo.cellIdentity
//
//                return nrCellIdentity.toString()
//            }
//        }
//        return "NR noService"
//    }
}