package sjdhome.componentmanager

import android.content.Context
import android.util.Log
import sjdhome.componentmanager.api.Shizuku
import sjdhome.componentmanager.user.User
import sjdhome.componentmanager.util.SharedPreferencesHolder

enum class OptionNames { SORT_OPTIONS, SHOW_OPTIONS, ACTION_OPTIONS }

enum class ActionOptions { ROOT_MODE, SHIZUKU_MODE, IFW_MODE }

enum class ViewOptions {
    USER_APP, USER_AND_SYSTEM_APP,
    BY_APP_NAME, BY_APP_SETUP_TIME, BY_APP_UPDATE_TIME
}

var sortOptions = ViewOptions.BY_APP_NAME
    private set
fun setSortOptions(context: Context, value: ViewOptions) {
    SharedPreferencesHolder(context).write(OptionNames.SORT_OPTIONS.name, value.name)
    sortOptions = value
}

var showOptions = ViewOptions.USER_APP
    private set
fun setShowOptions(context: Context, value: ViewOptions) {
    SharedPreferencesHolder(context).write(OptionNames.SHOW_OPTIONS.name, value.name)
    showOptions = value
}

var actionOptions = ActionOptions.ROOT_MODE
    private set
fun setActionOptions(context: Context, value: ActionOptions) {
    SharedPreferencesHolder(context).write(OptionNames.ACTION_OPTIONS.name, value.name)
    actionOptions = value
}

//val USER_NAME = User::class.java.simpleName.toUpperCase()
var users = arrayListOf<User>()
var currentUserId = 0
/*fun setCurrentUserId(/*context: Context, */value: Int) {
    // SharedPreferencesHolder(context).write(USER_NAME, value) // 没有必要保存
    currentUserId = value
}*/

val needRefreshIds = arrayListOf(
        R.id.item_refresh,
        R.id.item_root_mode,
        R.id.item_shizuku_mode,
        R.id.item_ifw_mode,
        R.id.item_by_app_name,
        R.id.item_by_setup_time,
        R.id.item_by_update_time,
        R.id.item_show_system_app
)