package sjdhome.componentmanager.custom

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent
import sjdhome.componentmanager.AppInfoActivity

class ViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {
    override fun scrollTo(x: Int, y: Int) {
        if (!AppInfoActivity.isApplying) super.scrollTo(x, y)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?) = if (!AppInfoActivity.isApplying) super.onInterceptTouchEvent(ev) else false
}