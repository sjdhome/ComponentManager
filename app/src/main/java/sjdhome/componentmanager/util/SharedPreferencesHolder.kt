package sjdhome.componentmanager.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class SharedPreferencesHolder(context: Context) {
    val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun <T> write(name: String, content: T) {
        val editor = sharedPreferences.edit()
        when (content) {
            is Boolean -> editor.putBoolean(name, content)
            is Float -> editor.putFloat(name, content)
            is Int -> editor.putInt(name, content)
            is Long -> editor.putLong(name, content)
            is String -> editor.putString(name, content)
            is Set<*> /* Set<String> */ -> {
                val set = mutableSetOf<String>()
                content.forEach { set.add(it as String) }
                editor.putStringSet(name, set)
            }
        }
        editor.apply()
    }
}