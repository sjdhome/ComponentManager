package sjdhome.componentmanager.api

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import io.reactivex.Flowable
import io.reactivex.FlowableSubscriber
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_app_info.*
import moe.shizuku.api.ShizukuPackageManagerV26
import org.reactivestreams.Subscription
import sjdhome.componentmanager.ActionOptions
import sjdhome.componentmanager.AppInfoActivity
import sjdhome.componentmanager.actionOptions
import sjdhome.componentmanager.component.Component
import sjdhome.componentmanager.component.ComponentAdapter
import sjdhome.componentmanager.currentUserId
import sjdhome.componentmanager.custom.getComponentEnabledSetting
import sjdhome.componentmanager.custom.getPackageInfo
import sjdhome.componentmanager.util.ComponentStateNotChangedException
import sjdhome.componentmanager.util.comparePinyin

// 中间层,尽量只用这里的
object ComponentController {
    fun getComponentEnabledSetting(component: Component, context: Context, userId: Int) = when (actionOptions) {
        ActionOptions.ROOT_MODE -> context.packageManager.getComponentEnabledSetting(ComponentName(component.packageName, component.fullName))
        ActionOptions.SHIZUKU_MODE -> getComponentEnabledSetting(ComponentName(component.packageName, component.fullName), userId)
        ActionOptions.IFW_MODE -> TODO()//IFW.getState(context, component)
    }

    private fun setComponentEnabledSetting(component: Component, state: Int, userId: Int, defaultState: Boolean = true) {
        val componentName = ComponentName(component.packageName, component.fullName)
        when (actionOptions) {
            ActionOptions.ROOT_MODE -> Root.shellPm(componentName, state, defaultState, userId)
            ActionOptions.SHIZUKU_MODE -> ShizukuPackageManagerV26.setComponentEnabledSetting(componentName, state, 0, userId)
            ActionOptions.IFW_MODE -> TODO()//IFW.write()
        }
    }

    fun getInstalledPackages(packageManager: PackageManager, flags: Int, userId: Int): List<PackageInfo> = when (actionOptions) {
        ActionOptions.SHIZUKU_MODE -> ShizukuPackageManagerV26.getInstalledPackages(flags, userId)
        else -> packageManager.getInstalledPackages(flags)
    }

    fun getPackageInfo(packageManager: PackageManager, packageName: String, flags: Int, userId: Int): PackageInfo = when (actionOptions) {
        ActionOptions.SHIZUKU_MODE -> getPackageInfo(packageName, flags, userId)   //getPackageInfo(packageName, flags, userId)
        else -> packageManager.getPackageInfo(packageName, flags)
    }

    fun getApplicationInfo(packageManager: PackageManager, packageName: String, flags: Int, userId: Int): ApplicationInfo = when (actionOptions) {
        ActionOptions.SHIZUKU_MODE -> ShizukuPackageManagerV26.getApplicationInfo(packageName, flags, userId)
        else -> packageManager.getApplicationInfo(packageName, flags)
    }

    // 关键，执行反转组件状态
    fun apply(context: Context, activity: AppInfoActivity? = null, componentAdapter: ComponentAdapter? = null, componentList: MutableList<Component> = ArrayList()) {
        activity?.isApplying = true
        val actionProgress = activity?.action_progress
        val components = componentAdapter?.selectedComponents ?: componentList
        componentAdapter?.let { components.forEach { component -> it.notifyItemChanged(it.filteredList.indexOf(component), ComponentAdapter.Payloads.UPDATE) } }
        activity?.action_progress?.max = components.size
        Flowable
                .fromArray(*components.toTypedArray())
                .sorted { component0, component1 -> comparePinyin(component0.simpleName, component1.simpleName) }
                .subscribeOn(Schedulers.io())
                .map {
                    if (AppInfoActivity.isApplying) {
                        val getState = {
                            val state = ComponentController.getComponentEnabledSetting(it, context, currentUserId)
                            Component.value2Boolean(state, it.selfEnabledInfo)
                        }
                        val oldState = getState()
                        val state = Component.boolean2Value(!oldState)
                        ComponentController.setComponentEnabledSetting(it, state, currentUserId)
                        val newState = getState()
                        if (newState != oldState) componentAdapter?.let { ca -> ca.selectedComponents -= it }  // 判断是否成功执行
                        else throw ComponentStateNotChangedException(it)    // 如果未变化则抛出异常
                    }
                    componentAdapter?.filteredList?.indexOf(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : FlowableSubscriber<Int> {
                    private lateinit var s: Subscription
                    override fun onSubscribe(s: Subscription) {
                        this.s = s
                        s.request(Long.MAX_VALUE)
                    }

                    override fun onNext(t: Int) {
                        actionProgress?.let { ++it.progress }
                        componentAdapter?.notifyItemChanged(t)  // 更新Switch状态
                    }

                    override fun onError(e: Throwable) = throw e // 把意大利炮扔出去

                    override fun onComplete() {
                        activity?.isApplying = false
                    }
                })
    }
}