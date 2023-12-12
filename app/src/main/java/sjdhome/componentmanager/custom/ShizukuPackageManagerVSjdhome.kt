package sjdhome.componentmanager.custom

import android.content.ComponentName
import android.content.pm.PackageInfo
import moe.shizuku.ShizukuConstants
import moe.shizuku.api.ShizukuClient
import moe.shizuku.io.ParcelInputStream
import moe.shizuku.io.ParcelOutputStream
import moe.shizuku.lang.ShizukuRemoteException
import java.io.IOException
import java.net.Socket

// TODO Shizuku V26 修订版 修复没有Close的问题
fun getPackageInfo(packageName: String, flags: Int, userId: Int): PackageInfo {
    var os: ParcelOutputStream? = null
    var `is`: ParcelInputStream? = null
    try {
        val client = Socket(ShizukuConstants.HOST, ShizukuConstants.PORT)
        client.soTimeout = ShizukuConstants.TIMEOUT
        os = ParcelOutputStream(client.getOutputStream())
        `is` = ParcelInputStream(client.getInputStream())
        os.writeString(ActionsVSjdhome.PackageManager_getPackageInfo)
        os.writeLong(ShizukuClient.getToken().mostSignificantBits)
        os.writeLong(ShizukuClient.getToken().leastSignificantBits)
        os.writeString(packageName)
        os.writeInt(flags)
        os.writeInt(userId)
        `is`.readException()
        return `is`.readParcelable(PackageInfo.CREATOR)
    } catch (e: IOException) {
        throw RuntimeException("Problem connect to shizuku server.", e)
    } catch (e: ShizukuRemoteException) {
        throw e.rethrowFromSystemServer()
    } finally {
        os?.close()
        `is`?.close()
    }
}

fun getComponentEnabledSetting(componentName: ComponentName, userId: Int): Int {
    var os: ParcelOutputStream? = null
    var `is`: ParcelInputStream? = null
    try {
        val client = Socket(ShizukuConstants.HOST, ShizukuConstants.PORT)
        client.soTimeout = ShizukuConstants.TIMEOUT
        os = ParcelOutputStream(client.getOutputStream())
        `is` = ParcelInputStream(client.getInputStream())
        os.writeString(ActionsVSjdhome.PackageManager_getComponentEnabledSetting)
        os.writeLong(ShizukuClient.getToken().mostSignificantBits)
        os.writeLong(ShizukuClient.getToken().leastSignificantBits)
        os.writeParcelable(componentName)
        os.writeInt(userId)
        `is`.readException()
        return `is`.readInt()
    } catch (e: IOException) {
        throw RuntimeException("Problem connect to shizuku server.", e)
    } catch (e: ShizukuRemoteException) {
        throw e.rethrowFromSystemServer()
    } finally {
        os?.close()
        `is`?.close()
    }

}

private object ActionsVSjdhome {
    const val PackageManager_getPackageInfo = "PackageManager_getPackageInfo"
    const val PackageManager_getComponentEnabledSetting = "PackageManager_getComponentEnabledSetting"
    const val PackageManager_setComponentEnabledSetting = "PackageManager_setComponentEnabledSetting"
}