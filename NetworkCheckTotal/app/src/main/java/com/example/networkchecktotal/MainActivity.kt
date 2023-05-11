package com.example.networkchecktotal

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.TextView
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MainActivity : AppCompatActivity() {

    var myTelephonyManager : TelephonyManager? = null
    var timer : Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myTelephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val textOutput = findViewById<TextView>(R.id.textView1)

        timer = fixedRateTimer(period = 3000){
            runOnUiThread{
                textOutput.text = readNetworkStatus()
            }
        }

    }

    fun readNetworkStatus() : String{
        myTelephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var str = myTelephonyManager?.signalStrength

        return str.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer = null
    }
}