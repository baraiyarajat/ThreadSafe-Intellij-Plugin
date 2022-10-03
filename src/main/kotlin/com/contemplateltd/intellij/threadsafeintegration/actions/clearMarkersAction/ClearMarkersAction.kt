package com.contemplateltd.intellij.threadsafeintegration.actions.clearMarkersAction

import com.contemplateltd.intellij.threadsafeintegration.extensions.LineMarker.LineMarkerProviderUtil
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.Icon

/*
    Action class that removes all ThreadSafe Analysis UI elements
 */
class ClearMarkersAction(actionName: String, actionDescription: String, actionIcon: Icon) : AnAction(actionName, actionDescription, actionIcon), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {


        val threadSafeFindingsTaskName = "ThreadSafe"
        val threadSafeAccessesAndLocksTaskName = "ThreadSafe:AccessesAndLocks"

        val project = e.project!!
        val toolWindowManager = ToolWindowManager.getInstance(project)

        //Remove Findings and AccessesAndLocks windows if they exist
        for(toolWindowId in toolWindowManager.toolWindowIds){
            if(toolWindowId == threadSafeFindingsTaskName){
                toolWindowManager.getToolWindow(threadSafeFindingsTaskName)!!.remove()
            }else if(toolWindowId == threadSafeAccessesAndLocksTaskName){
                toolWindowManager.getToolWindow("ThreadSafe:AccessesAndLocks")!!.remove()
            }

        }

        //Remove LineMarkers
        LineMarkerProviderUtil.setClearMarkersEnabled(true)

        //Update open files
        DaemonCodeAnalyzer.getInstance(project).restart()

    }

}