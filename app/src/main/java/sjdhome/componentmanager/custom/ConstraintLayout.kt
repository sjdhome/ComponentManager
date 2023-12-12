package sjdhome.componentmanager.custom

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.MotionEvent

class ConstraintLayout(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    override fun onInterceptHoverEvent(event: MotionEvent?) = true

    override fun onInterceptTouchEvent(ev: MotionEvent?) = true
}