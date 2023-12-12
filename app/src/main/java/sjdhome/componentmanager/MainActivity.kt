package sjdhome.componentmanager

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import moe.shizuku.api.ShizukuClient
import sjdhome.componentmanager.api.ComponentController
import sjdhome.componentmanager.api.Shizuku
import sjdhome.componentmanager.app.App
import sjdhome.componentmanager.app.AppAdapter
import sjdhome.componentmanager.util.*

class MainActivity : AppCompatActivity(), Searchable {
    private lateinit var appListAdapter: AppAdapter
    private lateinit var rxPermissions: RxPermissions
    override var currentQuery = ""

    private var loadingDisposable: Disposable? = null
    private var loading: Boolean    // 通过变量设定切换刷新状态
        get() = loading_layout.visibility == View.VISIBLE
        set(value) {
            if (value) {
                app_list.visibility = View.INVISIBLE
                loading_layout.visibility = View.VISIBLE
                loadingDisposable?.let {
                    if (!it.isDisposed) it.dispose()
                }
                loadingDisposable = Flowable
                        .just(0)    // flags
                        .flatMap {
                            val appList = ComponentController.getInstalledPackages(packageManager, it, currentUserId)
                            Flowable.fromArray(*appList.toTypedArray())
                        }
                        .filter {
                            when (showOptions) {
                                ViewOptions.USER_APP -> it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                                ViewOptions.USER_AND_SYSTEM_APP -> true
                                else -> false
                            }
                        }
                        .map { App(it.packageName, it) }    // 制成App成品
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .toSortedList { app0, app1 ->
                            val defaultWay = { p0: App, p1: App -> comparePinyin(p0.getName(this, currentUserId), p1.getName(this, currentUserId)) }
                            val i0 = app0.getPackageInfo(packageManager, currentUserId)
                            val i1 = app1.getPackageInfo(packageManager, currentUserId)
                            when (sortOptions) {
                                ViewOptions.BY_APP_NAME -> defaultWay(app0, app1)
                                ViewOptions.BY_APP_SETUP_TIME -> -i0.firstInstallTime.compareTo(i1.firstInstallTime) // 负号使最新的在前
                                ViewOptions.BY_APP_UPDATE_TIME -> -i0.lastUpdateTime.compareTo(i1.lastUpdateTime) // 负号使最新的在前
                                else -> defaultWay(app0, app1)
                            }
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { appList, _ ->
                            appListAdapter = AppAdapter(this, appList)
                            app_list.adapter = appListAdapter
                            search_view.setQuery(currentQuery, true)
                            loading = false // 关闭刷新状态
                        }
            } else {
                loading_layout.visibility = View.GONE
                app_list.visibility = View.VISIBLE
                loading_hint.text = resources.getString(R.string.loading_hint)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        // 初始化选项读取器
        val sharedPreferencesHolder = SharedPreferencesHolder(this)
        // 初始化应用列表
        app_list.layoutManager = LinearLayoutManager(this)
        // 初始化视图选项
        val getValue = fun(key: String, defValue: String?) = ViewOptions.valueOf(sharedPreferencesHolder.sharedPreferences.getString(key, defValue))
        setSortOptions(this, getValue(OptionNames.SORT_OPTIONS.name, sortOptions.name))
        setShowOptions(this, getValue(OptionNames.SHOW_OPTIONS.name, showOptions.name))
        // 初始化权限系统
        rxPermissions = RxPermissions(this)
        // 初始化行动选项
        val actionOptionNames = sharedPreferencesHolder.sharedPreferences.getString(OptionNames.ACTION_OPTIONS.name, actionOptions.name)
        when (actionOptionNames) {
            ActionOptions.SHIZUKU_MODE.name -> Shizuku.request(this, rxPermissions)
            else -> setActionOptions(this, ActionOptions.valueOf(actionOptionNames))
        }
        // 初始化搜索数据
        currentQuery = savedInstanceState?.getString(currentQueryName) ?: currentQuery
        // 初始化Shizuku
        ShizukuClient.initialize(applicationContext)
        // 初始化菜单数据
        invalidateOptionsMenu()
        // 开始刷新
        if (actionOptions != ActionOptions.SHIZUKU_MODE) loading = true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        // 初始化搜索过滤器
        val searchItem = menu?.findItem(R.id.item_search)
        searchItem?.setOnMenuItemClickListener {
            search_view.showSearch(false)
            true
        }
        search_view.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: currentQuery
                if (this@MainActivity::appListAdapter.isInitialized) appListAdapter.filter.filter(newText)   // 过滤
                return true
            }

            override fun onQueryTextSubmit(query: String?) = onQueryTextChange(query)
        })
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu?.findItem(when (sortOptions) {
            ViewOptions.BY_APP_NAME -> R.id.item_by_app_name
            ViewOptions.BY_APP_SETUP_TIME -> R.id.item_by_setup_time
            ViewOptions.BY_APP_UPDATE_TIME -> R.id.item_by_update_time
            else -> -1
        })?.isChecked = true
        menu?.findItem(when (actionOptions) {
            ActionOptions.ROOT_MODE -> R.id.item_root_mode
            ActionOptions.SHIZUKU_MODE -> R.id.item_shizuku_mode
            ActionOptions.IFW_MODE -> R.id.item_ifw_mode
        })?.isChecked = true
        menu?.findItem(R.id.item_show_system_app)?.isChecked = showOptions == ViewOptions.USER_AND_SYSTEM_APP
        menu?.findItem(R.id.item_user_options)?.isVisible = actionOptions == ActionOptions.SHIZUKU_MODE // 如果是 Shizuku 模式才启用多用户设定
        if (actionOptions != ActionOptions.SHIZUKU_MODE) {
            // 反射区域 危险
            val mainActivityClass = Class.forName("sjdhome.componentmanager.MainActivity")
            val usrId = mainActivityClass.getField("userId")
            currentUserId = usrId.getInt(this)
        }
        if (loadingDisposable == null) {
            if (actionOptions != ActionOptions.SHIZUKU_MODE) loading = true
            else if (!ShizukuClient.getState().isAuthorized) Shizuku.request(this, rxPermissions)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        setSortOptions(this, when (item?.itemId) {
            R.id.item_by_app_name -> ViewOptions.BY_APP_NAME
            R.id.item_by_setup_time -> ViewOptions.BY_APP_SETUP_TIME
            R.id.item_by_update_time -> ViewOptions.BY_APP_UPDATE_TIME
            else -> sortOptions
        })
        setShowOptions(this, when (item?.itemId) {
            R.id.item_show_system_app -> if (item.isChecked) ViewOptions.USER_APP else ViewOptions.USER_AND_SYSTEM_APP
            else -> showOptions
        })
        setActionOptions(this, when (item?.itemId) {
            R.id.item_root_mode -> ActionOptions.ROOT_MODE
            R.id.item_shizuku_mode -> ActionOptions.SHIZUKU_MODE
            R.id.item_ifw_mode -> ActionOptions.IFW_MODE
            else -> actionOptions
        })
        when (item?.itemId) {
            R.id.item_user_options -> {
                val userNameList = arrayListOf<String>()
                var tempUserId = 0
                users.forEach { userNameList.add(it.name) }
                AlertDialog
                        .Builder(this)
                        .setTitle(R.string.title_user_options)
                        .setSingleChoiceItems(userNameList.toTypedArray(), users.indexOf(users.find { it.id == currentUserId }))
                        { _, which -> tempUserId = users[which].id }
                        .setPositiveButton(R.string.button_ok) { _, _ ->
                            currentUserId = tempUserId
                            loading = true
                        }
                        .setOnCancelListener { loading = false }
                        .create()
                        .show()
            }
            in needRefreshIds -> {
                loadingDisposable?.let { if (!it.isDisposed) it.dispose() }
                loadingDisposable = null
            }
        }
        invalidateOptionsMenu()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ShizukuClient.REQUEST_CODE_AUTHORIZATION -> if (resultCode == ShizukuClient.AUTH_RESULT_OK) {
                ShizukuClient.setToken(data)
                setActionOptions(this, ActionOptions.SHIZUKU_MODE)
                initUsers() // 初始化用户
                loading = true
                invalidateOptionsMenu()
            } else {
                setActionOptions(this, ActionOptions.ROOT_MODE)
                Toast.makeText(this, resources.getString(R.string.shizuku_permission_not_allowed_hint), Toast.LENGTH_LONG).show()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(currentQueryName, currentQuery)
    }

    override fun onBackPressed() = if (search_view.isSearchOpen) search_view.closeSearch() else super.onBackPressed()
}
