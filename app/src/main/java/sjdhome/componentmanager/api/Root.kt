package sjdhome.componentmanager.api

import android.content.ComponentName
import android.content.pm.PackageManager
import eu.chainfire.libsuperuser.Shell
import sjdhome.componentmanager.component.Component
import sjdhome.componentmanager.util.NoRootException

object Root {
    private const val SET_COMPONENT_ENABLED = "pm enable --user"
    private const val SET_COMPONENT_DISABLED = "pm disable --user"

    fun shellPm(componentName: ComponentName, state: Int, defaultState: Boolean, userId: Int) {
        val command = if (Component.value2Boolean(state, defaultState)) SET_COMPONENT_ENABLED else SET_COMPONENT_DISABLED
        val escapedComponentName = componentName.className.replace("""$""", """\$""")   // "$"需要转义，不然会当成shell变量忽略
        if (Shell.SU.available()) Shell.SU.run("$command $userId ${componentName.packageName}/$escapedComponentName")
        else throw NoRootException()
    }
}