package sjdhome.componentmanager.api

import android.app.Activity
import android.content.ComponentName
import android.util.Log
import android.widget.Toast
import com.tbruyelle.rxpermissions2.RxPermissions
import moe.shizuku.api.ShizukuClient
import moe.shizuku.api.ShizukuPackageManagerV26
import sjdhome.componentmanager.ActionOptions
import sjdhome.componentmanager.R
import sjdhome.componentmanager.custom.getComponentEnabledSetting
import sjdhome.componentmanager.setActionOptions

object Shizuku {
    fun request(activity: Activity, rxPermissions: RxPermissions) =
            if (ShizukuClient.isManagerInstalled(activity)) {
                if (!ShizukuClient.getState().isAuthorized) {
                    if (!ShizukuClient.checkSelfPermission(activity)) {
                        rxPermissions
                                .request(ShizukuClient.PERMISSION_V23)
                                .subscribe {
                                    if (it) ShizukuClient.requestAuthorization(activity)
                                    else {
                                        setActionOptions(activity, ActionOptions.ROOT_MODE)
                                        Toast.makeText(activity, activity.resources.getString(R.string.shizuku_permission_not_allowed_hint), Toast.LENGTH_LONG).show()
                                    }
                                }
                    } else ShizukuClient.requestAuthorization(activity)
                }
                true
            } else {
                Toast.makeText(activity, activity.resources.getString(R.string.shizuku_permission_not_allowed_hint), Toast.LENGTH_LONG).show()
                false
            }
}