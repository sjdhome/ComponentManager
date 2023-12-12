package sjdhome.componentmanager.api.ifw

import sjdhome.componentmanager.app.App
import sjdhome.componentmanager.component.ComponentType

data class ComponentFilter(val name: String, val type: ComponentType?, val app: App)