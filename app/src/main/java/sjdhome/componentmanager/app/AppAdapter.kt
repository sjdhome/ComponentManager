package sjdhome.componentmanager.app

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_app.view.*
import sjdhome.componentmanager.AppInfoActivity
import sjdhome.componentmanager.R
import sjdhome.componentmanager.currentUserId

class AppAdapter(private val activity: Activity, private val appList: List<App>) : RecyclerView.Adapter<AppAdapter.ViewHolder>(), Filterable {
    val filteredList = appList.toMutableList() // 转为可编写用于过滤

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var appIcon: ImageView = itemView.app_icon
        var appName: TextView = itemView.app_name
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_app, parent, false)
        val viewHolder = ViewHolder(view)
        view.setOnClickListener { AppInfoActivity.startActivityWithApp(activity, filteredList[viewHolder.adapterPosition], it) }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appObservable = Observable.fromArray(filteredList[position])
        appObservable
                .map { it.getIcon(activity, currentUserId) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { if (holder.adapterPosition == position) holder.appIcon.setImageBitmap(it) }
        appObservable
                .map { it.getName(activity, currentUserId) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { if (holder.adapterPosition == position) holder.appName.text = it }
    }

    override fun getItemCount() = filteredList.size

    override fun getFilter() = object : Filter() {
        override fun performFiltering(p0: CharSequence?): FilterResults {
            val filteredList = arrayListOf<App>()
            if (p0 != null && p0 != "") appList.forEach { if (it.getName(activity, currentUserId).contains(p0, true)) filteredList += it }
            else filteredList += appList
            val filterResults = FilterResults()
            filterResults.values = filteredList
            return filterResults
        }

        override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
            filteredList.clear()
            (p1?.values as ArrayList<*>).forEach { filteredList += it as App }
            notifyDataSetChanged()
        }
    }
}