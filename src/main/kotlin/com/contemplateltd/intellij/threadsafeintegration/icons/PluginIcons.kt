package com.contemplateltd.intellij.threadsafeintegration.icons

import com.intellij.openapi.util.IconLoader

/*
    Custom Icons required for plugin are constructed from here.
    Static .png images are kept in icons folder present in resources root folder
 */
object PluginIcons {
    @JvmField
    val ContemplateIcon = IconLoader.getIcon("/icons/contemplate.png", javaClass)

    @JvmField
    val majorSeverity = IconLoader.getIcon("/icons/severity/major.png", javaClass)

    @JvmField
    val minorSeverity = IconLoader.getIcon("/icons/severity/minor.png", javaClass)

    @JvmField
    val criticalSeverity = IconLoader.getIcon("/icons/severity/critical.png", javaClass)

    @JvmField
    val blockerSeverity = IconLoader.getIcon("/icons/severity/blocker.png", javaClass)

    @JvmField
    val infoSeverity = IconLoader.getIcon("/icons/severity/info.png", javaClass)


}