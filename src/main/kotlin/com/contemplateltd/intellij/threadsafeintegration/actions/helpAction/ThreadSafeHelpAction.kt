package com.contemplateltd.intellij.threadsafeintegration.actions.helpAction

import com.contemplateltd.intellij.threadsafeintegration.util.HelpPageProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/*
    Action class to open ThreadSafe help from Help menu
    Relies on HelpPageProvider Util class
 */
class ThreadSafeHelpAction:  AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val helpPageProvider = HelpPageProvider()
        helpPageProvider.openHtmlEditor("#")
    }
}