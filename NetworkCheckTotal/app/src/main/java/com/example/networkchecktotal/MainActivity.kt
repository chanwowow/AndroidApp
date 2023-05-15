package com.example.networkchecktotal

import android.content.Context
import android.os.Bundle
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MainActivity : AppCompatActivity() {

    private var myTelephonyManager : TelephonyManager? = null
    private var timer : Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myTelephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val textOutput = findViewById<TextView>(R.id.textView1)

        timer = fixedRateTimer(period = 1000){
            runOnUiThread{
                textOutput.text = readNetworkStatus()
            }
        }

    }

    fun readNetworkStatus() : String{
        val allRatSignal = myTelephonyManager?.signalStrength
        val timeStamp = allRatSignal?.timestampMillis
        val activeSignalList = allRatSignal?.getCellSignalStrengths()

        var returnStr = "Insert SIM"
        if (activeSignalList != null){
            returnStr = ""
            for (rat in activeSignalList){
                when (rat.toString().getOrNull(18)){
                    'G' -> returnStr += "GSM :"
                    'W' -> returnStr += "WCDMA :"
                    'L' -> returnStr += "LTE :"
                    'N' -> returnStr += "NR :"
                    else -> returnStr += "etc :"
                }
                returnStr += "[RSRP : " + rat.dbm.toString() + "dBm] \n Time : $timeStamp \n" // 일단 그 신호 세기 알려주긴 함.
            }
        }

        return returnStr
    }

    override fun onDestroy() {
        super.onDestroy()
        timer = null
    }
}