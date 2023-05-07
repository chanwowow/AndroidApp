package com.example.calltesthelper

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

typealias Action = () -> Unit
fun Context.dp2px(dpValue: Float): Int {
    val scale = resources.displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}

class TouchDragListener(private val service : Service,
                        private val params: WindowManager.LayoutParams,
                        private val startDragDistance: Int = 10,
                        private val onTouch: Action?,
                        private val onDrag: Action?) : View.OnTouchListener {

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0.toFloat()
    private var initialTouchY: Float = 0.toFloat()
    private var isDrag = false
    private var startTime = 0L
    private val handler = Handler()

    private fun isDragging(event: MotionEvent): Boolean {
        val deltaX = event.rawX - initialTouchX
        val deltaY = event.rawY - initialTouchY
        return (deltaX * deltaX + deltaY * deltaY) > (startDragDistance * startDragDistance)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                isDrag = false
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                startTime = System.currentTimeMillis()
                handler.postDelayed({ service.stopSelf() }, 2000)
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDrag && isDragging(event)) {
                    isDrag = true
                }
                if (!isDrag) return true

                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime <= 2000)
                    handler.removeCallbacksAndMessages(null)

                params.x = initialX + (event.rawX - initialTouchX).toInt()
                params.y = initialY + (event.rawY - initialTouchY).toInt()
                onDrag?.invoke()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val elapsedTime = System.currentTimeMillis() - startTime
                if (!isDrag && elapsedTime <= 2000) {
                    handler.removeCallbacksAndMessages(null)
                    onTouch?.invoke()
                    return true
                }
            }
        }
        return false
    }
}