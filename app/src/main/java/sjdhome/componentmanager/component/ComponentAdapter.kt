package sjdhome.componentmanager.component

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.Switch
import android.widget.TextView
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_component.view.*
import sjdhome.componentmanager.AppInfoActivity
import sjdhome.componentmanager.R
import sjdhome.componentmanager.api.ComponentController
import sjdhome.componentmanager.currentUserId
import sjdhome.componentmanager.util.log

class ComponentAdapter(private val context: Context, private val componentList: List<Component>) : RecyclerView.Adapter<ComponentAdapter.ViewHolder>(), Filterable {
    // 列表
    val filteredList = componentList.toMutableList()   // 过滤后的List，主用
    val selectedComponents = arrayListOf<Component>()

    var isCheckedDisposable: Disposable? = null

    var isSelectingAll: Boolean // 判断当前是否全部选中
        get() = selectedComponents.size == filteredList.size
        set(value) {
            if (value) {
                selectedComponents.clear()
                for (x in 0 until filteredList.size) {
                    selectedComponents += filteredList[x]
                    notifyItemChanged(x, Payloads.UPDATE)
                }
            } else {
                val temp = arrayListOf(*selectedComponents.toTypedArray())
                selectedComponents.clear()
                temp.forEach { notifyItemChanged(filteredList.indexOf(it), Payloads.UPDATE) }
            }
        }

    enum class Payloads { UPDATE }

    // 正题
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layout: ConstraintLayout = itemView.component_layout
        val simpleName: TextView = itemView.component_simple_name
        val fullName: TextView = itemView.component_full_name
        val switch: Switch = itemView.component_switch
    }

    private fun clickViewHolderLayout(viewHolder: ViewHolder) {
        if (!AppInfoActivity.isApplying) {
            selectedComponents.apply {
                val component = filteredList[viewHolder.adapterPosition]
                if (!contains(component)) this += component else this -= component
            }
            notifyItemChanged(viewHolder.adapterPosition, Payloads.UPDATE)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_component, parent, false))
        viewHolder.layout.setOnClickListener { clickViewHolderLayout(viewHolder) }
        return viewHolder
    }

    // 统一实现
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = onBindViewHolder(holder, position, arrayListOf())

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        val component = filteredList[position]
        if (payloads.isEmpty()) {
            holder.simpleName.text = component.simpleName
            holder.fullName.text = component.fullName
            isCheckedDisposable = Flowable
                    .just(component)
                    .map { Component.value2Boolean(ComponentController.getComponentEnabledSetting(it, context, currentUserId), it.selfEnabledInfo) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (holder.adapterPosition == position) {
                            holder.switch.isChecked = it
                            if (!AppInfoActivity.isApplying) holder.switch.jumpDrawablesToCurrentState()    // 提前结束动画
                        }
                    }
        }
        holder.layout.setBackgroundColor(ContextCompat.getColor(context,
                if (selectedComponents.contains(component)) R.color.item_background_checked else R.color.item_background_unchecked))
        holder.switch.isEnabled = if (AppInfoActivity.isApplying) !selectedComponents.contains(component) else true
    }

    override fun getItemCount() = filteredList.size

    // 搜索过滤
    override fun getFilter() = object : Filter() {
        override fun performFiltering(p0: CharSequence?): FilterResults {
            val filteredList = arrayListOf<Component>()
            if (p0 != null && p0 != "") componentList.forEach { if (it.fullName.contains(p0, true)) filteredList.add(it) }
            else componentList.forEach { filteredList.add(it) }
            val filterResults = FilterResults()
            filterResults.values = filteredList
            return filterResults
        }

        override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
            filteredList.clear()
            (p1?.values as ArrayList<*>).forEach { filteredList.add(it as Component) }
            notifyDataSetChanged()
        }
    }
}