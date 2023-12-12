package sjdhome.componentmanager.component

import android.content.pm.ComponentInfo
import android.content.pm.PackageManager

enum class ComponentType {
    Service, BroadcastReceiver, Activity, ContentProvider;

    val packageManagerFlag: Int
            get() = when (this) {
                    ComponentType.Service -> PackageManager.GET_SERVICES
                    ComponentType.BroadcastReceiver -> PackageManager.GET_RECEIVERS
                    ComponentType.Activity -> PackageManager.GET_ACTIVITIES
                    ComponentType.ContentProvider -> PackageManager.GET_PROVIDERS
                }

}

data class Component(private val componentInfo: ComponentInfo) {
    val packageName: String
        get() = componentInfo.packageName

    val simpleName: String
        get() = fullName.split('.').last()  // 简单用分隔符切割并取最后一块

    val fullName: String
        get() = componentInfo.name

    val selfEnabledInfo: Boolean    // 默认值代表这个
        get() = componentInfo.enabled

    companion object {
        fun value2Boolean(state: Int, defaultValue: Boolean): Boolean = when (state) {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> defaultValue
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> defaultValue   // 玄学
        }

        fun boolean2Value(state: Boolean) = if (state) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    override fun equals(other: Any?): Boolean = if (other is Component) componentInfo == other.componentInfo else super.equals(other)

    override fun hashCode(): Int = componentInfo.hashCode()
}
