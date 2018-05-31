package it.cameralevel.roberto.cameralevel

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.view.LayoutInflater
import android.view.Gravity
import android.content.Context
import android.view.WindowManager
import android.graphics.PixelFormat
import android.view.View

import android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import android.view.WindowManager.LayoutParams.WRAP_CONTENT


class LevelAccessibilityService : AccessibilityService() {
    var mLayout: FrameLayout? = null

    var indicator: View? = null

    private fun prepareView() {
        // Create an overlay and display the action bar
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mLayout = FrameLayout(this)

        val lp = WindowManager.LayoutParams()
        lp.type = TYPE_ACCESSIBILITY_OVERLAY
        lp.format = PixelFormat.TRANSLUCENT
        lp.flags = lp.flags or FLAG_NOT_FOCUSABLE
        lp.width = WRAP_CONTENT
        lp.height = WRAP_CONTENT
        lp.gravity = Gravity.BOTTOM

        val inflater = LayoutInflater.from(this)
        inflater.inflate(R.layout.action_bar, mLayout)

        indicator = mLayout!!.findViewById(R.id.indicator) as View
        wm.addView(mLayout, lp)
    }

    override fun onServiceConnected() {
        //super.onServiceConnected();

        prepareView()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onInterrupt() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}