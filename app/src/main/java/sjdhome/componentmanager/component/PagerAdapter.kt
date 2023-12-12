package sjdhome.componentmanager.component

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class PagerAdapter(fm: FragmentManager?, private val packageName: String, titleList: Array<String>) : FragmentPagerAdapter(fm) {
    val fragmentList = arrayListOf<ComponentPage>()

    init {
        titleList.forEach {
            val componentPage = ComponentPage()
            componentPage.title = it
            componentPage.packageName = packageName
            fragmentList += componentPage

        }
    }

    override fun getItem(position: Int): Fragment = fragmentList[position]

    override fun getPageTitle(position: Int): CharSequence? = fragmentList[position].title

    override fun getCount(): Int = fragmentList.size
}