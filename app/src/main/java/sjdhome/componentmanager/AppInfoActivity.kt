package sjdhome.componentmanager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.util.Pair
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.miguelcatalan.materialsearchview.MaterialSearchView
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_app_info.*
import sjdhome.componentmanager.api.ComponentController
import sjdhome.componentmanager.app.App
import sjdhome.componentmanager.component.ComponentPage
import sjdhome.componentmanager.component.PagerAdapter
import sjdhome.componentmanager.util.ComponentStateNotChangedException
import sjdhome.componentmanager.util.NoRootException
import sjdhome.componentmanager.util.clearOldFragments

class AppInfoActivity : AppCompatActivity() {
    private var appPackageName = ""
    private lateinit var pagerAdapter: PagerAdapter
    private val currentPage: ComponentPage?
        get() = if (this::pagerAdapter.isInitialized) pagerAdapter.fragmentList[view_pager.currentItem] else null

    var isApplying: Boolean
        get() = AppInfoActivity.isApplying
        set(value) {
            AppInfoActivity.isApplying = value
            invalidateOptionsMenu()
            action_progress.max = 0
            action_progress.progress = 0
            action_progress.visibility = if (value) View.VISIBLE else View.GONE
            tab_component.visibility = if (value) View.INVISIBLE else View.VISIBLE
        }

    companion object {
        enum class SavedInstanceStateFlags { APP_PACKAGE_NAME }

        fun startActivityWithApp(fromActivity: Activity, app: App, view: View) {
            val intent = Intent(fromActivity, AppInfoActivity::class.java)
            // 放入资源
            intent.putExtra(SavedInstanceStateFlags.APP_PACKAGE_NAME.name, app.packageName)
            // 动画效果
            val iconPair = Pair.create(view.findViewById<View>(R.id.app_icon), fromActivity.resources.getString(R.string.app_icon_transition_name))
            // val namePair = Pair.create(view.findViewById<View>(R.id.app_name), fromActivity.resources.getString(R.string.app_name_transition_name))
            val animation = ActivityOptionsCompat.makeSceneTransitionAnimation(fromActivity, iconPair/*, namePair*/)
            ActivityCompat.startActivity(fromActivity, intent, animation.toBundle())
        }

        var isApplying = false
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_info)
        title = ""  // 清空标题
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // 拿出资源
        appPackageName = savedInstanceState?.getString(SavedInstanceStateFlags.APP_PACKAGE_NAME.name)
                ?: intent.getStringExtra(SavedInstanceStateFlags.APP_PACKAGE_NAME.name)
        // 设定图标和名字
        val appFlowable = Flowable.just(App(appPackageName))
        appFlowable
                .map { it.getIcon(this, currentUserId) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { app_icon.setImageBitmap(it) }
        appFlowable
                .map { it.getName(this, currentUserId) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { app_name.text = it }
        // 初始化组件列表
        refresh(true)
    }

    private fun refresh(firstRun: Boolean = false) {
        // 清理老旧的Fragments，防止控制失效
        clearOldFragments(supportFragmentManager)
        val titleList = resources.getStringArray(R.array.tab_item_component).toMutableList()
        // IFW模式无法操作内容提供器
        if (actionOptions == ActionOptions.IFW_MODE) titleList.remove(resources.getString(R.string.tab_item_component_content_provider))
        // 初始化组件列表
        pagerAdapter = PagerAdapter(supportFragmentManager, appPackageName, titleList.toTypedArray())
        view_pager.offscreenPageLimit = titleList.size  // 反正就4个页面就全部缓存了
        view_pager.adapter = pagerAdapter
        if (!firstRun) pagerAdapter.notifyDataSetChanged()
        // TabLayout与PagerView结合
        tab_component.setupWithViewPager(view_pager)
        tab_component.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let { pagerAdapter.fragmentList[tab.position].componentAdapter?.isSelectingAll = false }
                if (search_view.isSearchOpen) search_view.closeSearch()
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        search_view.closeSearch()
        super.onSaveInstanceState(outState)
        outState?.putString(SavedInstanceStateFlags.APP_PACKAGE_NAME.name, appPackageName)
    }

    override fun onDestroy() {
        AppInfoActivity.isApplying = false
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_info, menu)
        // 初始化搜索过滤器
        menu?.findItem(R.id.item_search)?.setOnMenuItemClickListener {
            search_view.showSearch(false)
            true
        }
        search_view.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                currentPage?.componentAdapter?.filter?.filter(newText)   // 过滤
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                onQueryTextChange(query)    // 同上
                return false
            }
        })
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        arrayListOf(R.id.item_search, R.id.item_refresh, R.id.item_select_all, R.id.item_apply_action, R.id.item_action_options)
                .forEach { menu?.findItem(it)?.isVisible = !AppInfoActivity.isApplying }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.item_refresh -> refresh()
            R.id.item_select_all -> {
                val state = currentPage?.componentAdapter?.isSelectingAll ?: true
                currentPage?.componentAdapter?.isSelectingAll = !state
            }
            R.id.item_apply_action -> try {
                ComponentController.apply(this, this, currentPage?.componentAdapter)
            } catch (e: ComponentStateNotChangedException) {
                Toast.makeText(this,"${e.localizedMessage}${resources.getString(R.string.component_state_not_changed_hint)}", Toast.LENGTH_LONG).show()
            } catch (e: NoRootException) {
                Toast.makeText(this, resources.getString(R.string.no_root_hint), Toast.LENGTH_LONG).show()
            }
        }
        return true
    }
}
