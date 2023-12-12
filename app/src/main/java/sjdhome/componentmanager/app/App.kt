package sjdhome.componentmanager.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.UserHandle
import sjdhome.componentmanager.R
import sjdhome.componentmanager.api.ComponentController
import sjdhome.componentmanager.util.dp2px
import sjdhome.componentmanager.util.getNewSize
import sjdhome.componentmanager.util.toBitmap

class App(var packageName: String, private var packageInfo: PackageInfo? = null) {
    fun getPackageInfo(packageManager: PackageManager, userId: Int): PackageInfo {
        packageInfo?.let { return it }
        return ComponentController.getPackageInfo(packageManager, packageName, 0, userId)
    }

    fun getApplicationInfo(packageManager: PackageManager, userId: Int): ApplicationInfo {
        packageInfo?.let {
            return it.applicationInfo
        }
        return ComponentController.getApplicationInfo(packageManager, packageName, 0, userId)
    }

    fun getIcon(context: Context, userId: Int): Bitmap {
        val appInfo = getApplicationInfo(context.packageManager, userId)
        val appIconBackup = appInfo.loadIcon(context.packageManager)
        val appIcon = context.packageManager.getUserBadgedIcon(appIconBackup, UserHandle(userId))    // 拿到对应用户的应用图标
        val appIconBitmap = appIcon.toBitmap()
        val appIconSize = dp2px(context, context.resources.getInteger(R.integer.default_app_icon_size))
        return appIconBitmap.getNewSize(appIconSize)
    }

    fun getName(context: Context, userId: Int): String {
        val applicationInfo = getApplicationInfo(context.packageManager, userId)
        return context.packageManager.getUserBadgedLabel(applicationInfo.loadLabel(context.packageManager), UserHandle(userId)).toString()
    }

    override fun equals(other: Any?): Boolean = if (other is App) other.packageName == packageName else super.equals(other)

    override fun hashCode() = packageName.hashCode()
}