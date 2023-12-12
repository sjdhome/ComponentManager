package sjdhome.componentmanager.util

import sjdhome.componentmanager.component.Component

class ComponentStateNotChangedException(component: Component) : Exception(component.fullName)