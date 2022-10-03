package com.contemplateltd.intellij.threadsafeintegration.actions.threadSafeRunMenuActionGroup

import com.contemplateltd.intellij.threadsafeintegration.actions.clearMarkersAction.ClearMarkersAction
import com.contemplateltd.intellij.threadsafeintegration.actions.runAnalysisAction.RunAnalysisAction
import com.contemplateltd.intellij.threadsafeintegration.actions.threadSafeActionGroup.ThreadSafeActionGroupUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager

/*
    ActionGroup Class that shows RunAnalysis and clearMarkers action from Run Menu
 */

class ThreadSafeRunMenuActionGroup : ActionGroup(), DumbAware {
    /*
        Returns actions if a project is open
     */
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {

        val project: Project? = e?.project
        var toShow = false

        //Show only if ProjectSDK is of type Java
        val projectSDKName = ProjectRootManager.getInstance(project!!).projectSdkTypeName
        if(projectSDKName == "JavaSDK"){
            toShow = true
        }

        //Show options only if a project is open
        return if(toShow){
            //Set Project Directory path in analysisPaths file
            val projectDirectory = project.basePath
            val threadSafeActionGroupUtil = ThreadSafeActionGroupUtil(project)
            threadSafeActionGroupUtil.addAnalysisProperty("ProjectPath",projectDirectory!!)
            //Actions to show
            arrayOf(
                RunAnalysisAction("Run Analysis",
                    "Run the ThreadSafe Analysis on this project",
                    AllIcons.Actions.Execute),
                ClearMarkersAction("Clear Markers",
                    "Clears all Findings data from UI",
                    AllIcons.Actions.CloseDarkGrey)
            )

        }else{
            arrayOf()
        }
    }
}


