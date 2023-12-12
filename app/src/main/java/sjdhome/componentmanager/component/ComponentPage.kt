package sjdhome.componentmanager.component

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_component.*
import sjdhome.componentmanager.R
import sjdhome.componentmanager.api.ComponentController
import sjdhome.componentmanager.currentUserId
import sjdhome.componentmanager.util.UnknownComponentTypeException
import sjdhome.componentmanager.util.comparePinyin
import sjdhome.componentmanager.util.getComponentList
import sjdhome.componentmanager.util.log
import java.util.concurrent.TimeUnit

class ComponentPage : Fragment() {
    var packageName = ""
    var title = "" // 当前对应标题
    var componentAdapter: ComponentAdapter? = null
    private lateinit var componentList: List<Component>
    private var componentDisposable: Disposable? = null

    enum class SavedInstanceStateFlags { PACKAGE_NAME, TITLE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 恢复包信息和标题
        packageName = savedInstanceState?.getString(SavedInstanceStateFlags.PACKAGE_NAME.name) ?: packageName
        title = savedInstanceState?.getString(SavedInstanceStateFlags.TITLE.name) ?: title
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(R.layout.fragment_component, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 处理组件列表
        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.recycleChildrenOnDetach = true
        rv_component.layoutManager = linearLayoutManager
        // 填充数据
        componentDisposable = Flowable
                .just(packageName)
                .flatMap {
                    // 根据标题取出数据
                    val packageManager = requireContext().packageManager
                    @Suppress("DEPRECATION")    // 防止废弃API提示
                    val otherFlags = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) PackageManager.MATCH_DISABLED_COMPONENTS
                    else PackageManager.GET_DISABLED_COMPONENTS)
                    val type = when (title) {
                        resources.getString(R.string.tab_item_component_service) -> ComponentType.Service
                        resources.getString(R.string.tab_item_component_broadcast_receiver) -> ComponentType.BroadcastReceiver
                        resources.getString(R.string.tab_item_component_activity) -> ComponentType.Activity
                        resources.getString(R.string.tab_item_component_content_provider) -> ComponentType.ContentProvider
                        else -> throw UnknownComponentTypeException(title)
                    }
                    ComponentController.getPackageInfo(packageManager, packageName,
                            type.packageManagerFlag or otherFlags, currentUserId)
                            .getComponentList(type)
                            .toTypedArray()
                            .toFlowable()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .toSortedList { component0, component1 -> comparePinyin(component0.simpleName, component1.simpleName) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { componentList, _ ->
                    this.componentList = componentList
                    componentAdapter = ComponentAdapter(requireContext(), this.componentList)
                    rv_component.adapter = componentAdapter
                }
    }

    override fun onStop() {
        super.onStop()
        componentDisposable?.let { if (!it.isDisposed) it.dispose() }
        componentAdapter?.isCheckedDisposable?.let { if (!it.isDisposed) it.dispose() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SavedInstanceStateFlags.PACKAGE_NAME.name, packageName)
        outState.putString(SavedInstanceStateFlags.TITLE.name, title)
    }
}
