package sjdhome.componentmanager.api.ifw

import android.content.Context
import sjdhome.componentmanager.app.App
import sjdhome.componentmanager.component.Component
import sjdhome.componentmanager.component.ComponentType
import sjdhome.componentmanager.util.copyFileToTemp
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private const val IFW_PATH = "/data/system/ifw"
private const val SERVICE_TAG = "service"
private const val BROADCAST_TAG = "broadcast"
private const val ACTIVITY_TAG = "activity"
private const val BLOCK_ATTR = "block"
private const val COMPONENT_NAME_TAG = "name"
private const val TRUE_VALUE = "true"
private const val COMPONENT_FILTER_NAME = "component-filter"

class IFW(context: Context, app: App) {
    private var componentFiltersName = ""
    private val componentFilters = arrayListOf<ComponentFilter>()

    init {
        readIFWFile(context, App(app.packageName))
    }

    private fun readIFWFile(context: Context, app: App) {
        val ifwFile = copyFileToTemp(context, File("$IFW_PATH/${app.packageName}.xml"))
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        val documentBuilder = documentBuilderFactory.newDocumentBuilder()
        val ifwDocument = documentBuilder.parse(ifwFile)
        val componentFilterNodeList = ifwDocument.getElementsByTagName(COMPONENT_FILTER_NAME)
        for (componentIndex in 0 until componentFilterNodeList.length) {
            val node = componentFilterNodeList.item(componentIndex)
            val nodeAttr = node.attributes
            var block = false
            for (parentAttrsIndex in 0 until node.parentNode.attributes.length) {
                val attr = node.parentNode.attributes.item(parentAttrsIndex)
                if (attr.nodeName == BLOCK_ATTR && attr.nodeValue == TRUE_VALUE) {
                    block = true
                    break
                }
            }
            if (block) {
                for (nodeAttrIndex in 0 until nodeAttr.length) {
                    if (nodeAttr.item(nodeAttrIndex).nodeName == COMPONENT_NAME_TAG) componentFilters += ComponentFilter(nodeAttr.item(nodeAttrIndex).nodeValue, when (node.parentNode.nodeName) {
                        SERVICE_TAG -> ComponentType.Service
                        BROADCAST_TAG -> ComponentType.BroadcastReceiver
                        ACTIVITY_TAG -> ComponentType.Activity
                        else -> null
                    }, app)
                }
            }
        }
    }

    fun getState(context: Context, component: Component): Int {
        if (componentFiltersName != component.packageName) {
            componentFilters.clear()
            componentFiltersName = component.packageName
        }
        componentFilters.forEach {
            if (it.name == component.fullName) return Component.boolean2Value(true)
        }
        return Component.boolean2Value(false)
    }

    fun write() {
        TODO("Write")
    }
}