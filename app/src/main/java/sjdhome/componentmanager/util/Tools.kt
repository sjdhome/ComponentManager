package sjdhome.componentmanager.util

import android.content.Context
import android.content.pm.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.support.v4.app.FragmentManager
import android.util.Log
import com.github.promeg.pinyinhelper.Pinyin
import eu.chainfire.libsuperuser.Shell
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import moe.shizuku.api.ShizukuUserManagerV26
import sjdhome.componentmanager.component.Component
import sjdhome.componentmanager.component.ComponentType
import sjdhome.componentmanager.user.User
import sjdhome.componentmanager.users
import java.io.File

fun Drawable.toBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp
}

fun Bitmap.getNewSize(width: Float, height: Float = width): Bitmap {
    val scaleWidth = (width / this.width)
    val scaleHeight = (height / this.height)
    val matrix = Matrix()
    matrix.postScale(scaleWidth, scaleHeight)
    return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}

fun dp2px(context: Context, value: Int) = value * context.resources.displayMetrics.density + 0.5f

// 重载方法以防止强制转换
fun Array<ServiceInfo>.toComponentList(): ArrayList<Component> {
    val componentList = arrayListOf<Component>()
    forEach { componentList += Component(it) }
    return componentList
}

fun Array<ActivityInfo>.toComponentList(): ArrayList<Component> {
    val componentList = arrayListOf<Component>()
    forEach { componentList += Component(it) }
    return componentList
}

fun Array<ProviderInfo>.toComponentList(): ArrayList<Component> {
    val componentList = arrayListOf<Component>()
    forEach { componentList += Component(it) }
    return componentList
}

fun log(any: Any, anyString: Any) = Log.d(any::class.java.simpleName, anyString.toString())

fun comparePinyin(str0: String, str1: String): Int = Pinyin.toPinyin(str0[0]).toLowerCase().compareTo(Pinyin.toPinyin(str1[0]).toLowerCase())

fun clearOldFragments(supportFragmentManager: FragmentManager) {
    val ft = supportFragmentManager.beginTransaction()
    supportFragmentManager.fragments.forEach { ft.remove(it) }
    ft.commit()
    supportFragmentManager.executePendingTransactions()
}

fun initUsers(): Disposable = Flowable
        .just(true) // 排除死亡用户
        .map {
            users.clear()
            it
        }
        .flatMap { Flowable.fromArray(*ShizukuUserManagerV26.getUsers(it).toTypedArray()) }
        .subscribeOn(Schedulers.newThread())
        .subscribe { users.add(User(it.name, it.id)) }

private const val COPY_COMMAND = "cp"
fun copyFileToOwnPlaceWithSU(filePath: String, placePath: String): File? {
    val command = "$COPY_COMMAND $filePath $placePath/"
    return if (Shell.SU.available()) {
        Shell.SU.run(command)
        val fileName = filePath.split('/').last()
        val file = File("$placePath/$fileName")
        if (file.exists()) file else null
    } else null
}

const val COPY_CMD = "cp"
fun copyFileToTemp(context: Context, file: File): File? {
    val time0 = System.currentTimeMillis()
    val tempPath = "${context.externalCacheDir.absolutePath}/"
    val command = "$COPY_CMD ${file.absolutePath} $tempPath"
    if (Shell.SU.available() && file.exists()) {
        Shell.SU.run(command)
        val newFile = File("$tempPath/${file.name}")
        Log.d("CopyToTemp", "${(System.currentTimeMillis() - time0) / 1000}")
        if (newFile.exists()) return newFile
    }
    return null
}

fun copyFileTo(context: Context, file: File, newFile: File) {
    TODO("复制回去")
}

// 即使是!类型也可能是null...
fun PackageInfo.getComponentList(type: ComponentType): ArrayList<Component> = when (type) {
    ComponentType.Service -> if (services != null) services.toComponentList() else arrayListOf()
    ComponentType.BroadcastReceiver -> if (receivers != null) receivers.toComponentList() else arrayListOf()
    ComponentType.Activity -> if (activities != null) activities.toComponentList() else arrayListOf()
    ComponentType.ContentProvider -> if (providers != null) providers.toComponentList() else arrayListOf()
}

// 暂时用不到
/*fun Int.getPackageManagerFlagType() = when (this) {
    PackageManager.GET_SERVICES -> ComponentType.Service
    PackageManager.GET_RECEIVERS -> ComponentType.BroadcastReceiver
    PackageManager.GET_ACTIVITIES -> ComponentType.Activity
    PackageManager.GET_PROVIDERS -> ComponentType.ContentProvider
    else -> null
}*/

fun <T> MutableList<T>.toIntHashMap(): HashMap<Int, T> {
    val map = hashMapOf<Int, T>()
    for (x in 0 until size) map[x] = this[x]
    return map
}

fun <T> HashMap<Int, T>.toArrayList(): ArrayList<T> {
    val list = arrayListOf<T>()
    forEach { _, any ->
        list += any
    }
    return list
}