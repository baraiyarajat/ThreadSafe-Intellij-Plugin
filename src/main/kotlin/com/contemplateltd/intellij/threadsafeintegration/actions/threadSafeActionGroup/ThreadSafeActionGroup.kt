package com.contemplateltd.intellij.threadsafeintegration.actions.threadSafeActionGroup

import com.contemplateltd.intellij.threadsafeintegration.actions.clearMarkersAction.ClearMarkersAction
import com.contemplateltd.intellij.threadsafeintegration.actions.runAnalysisAction.RunAnalysisAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl
import com.intellij.psi.impl.source.PsiClassImpl
import java.util.*

/*
    -ActionGroup class required to display runAnalysis Action and clearMarkers Action
    -Actions are displayed when a project folder or .java source files are selected
    -Actions are displayed only if a valid project is open
 */

class ThreadSafeActionGroup : ActionGroup(), DumbAware {

    //Add logic to see if ThreadSafe group to be displayed
    override fun getChildren(event: AnActionEvent?): Array<AnAction> {

        // If no project is open return empty array
        val project = event!!.project ?: return arrayOf()

        //Show only if ProjectSDK is of type Java
        val projectSDKName = ProjectRootManager.getInstance(project).projectSdkTypeName
        if(projectSDKName != "JavaSDK"){
            return arrayOf()
        }
        //Project Directory Path
        val projectDirectory = project.basePath

        //Array returning actions
        var threadSafeActionGroup : Array<AnAction> = arrayOf()

        //Boolean Indicating whether to show action group or not
        var toShow = false

        //Boolean Indicating whether projectDirectory was selected or not
        var isProjectDirectorySelected = false

        //Get SelectedElements
        val selectedItems = event.getData(PlatformCoreDataKeys.SELECTED_ITEMS)

        //Store selected .java files path
        val selectedSourceFilePaths = Vector<String>()

        //Checking whether the selected elements include only projectDirectory and(or) Java files
        //If JavaFiles are selected, their qualified names are added to Vector to specify which classes to analyze
        //If Project Directory is selected, all files are analyzed
        if(selectedItems!=null){
            for(element in selectedItems){
                if(element.javaClass == PsiClassImpl::class.java){
                    val selectedElement: PsiClassImpl = element as PsiClassImpl
                    if(selectedElement.qualifiedName!=null){
                        selectedSourceFilePaths.add(selectedElement.qualifiedName!!.replace(".","/"))
                    }

                    toShow = true
                }else if(element.javaClass == PsiJavaDirectoryImpl::class.java){
                    val selectedElement: PsiJavaDirectoryImpl = element as PsiJavaDirectoryImpl
                    if(selectedElement.virtualFile.canonicalPath == projectDirectory){
                        isProjectDirectorySelected = true
                        toShow = true
                    }else{
                        isProjectDirectorySelected = false
                        toShow = false
                        break
                    }
                }else{
                    isProjectDirectorySelected = false
                    toShow = false
                    break
                }
            }
        }

        //Write class files and project directory if selected
        if(toShow){
            val threadSafeActionGroupUtil = ThreadSafeActionGroupUtil(project)

            if(isProjectDirectorySelected){
                threadSafeActionGroupUtil.addAnalysisProperty("ProjectPath",projectDirectory!!)
            }else{
                val classPaths = selectedSourceFilePaths.joinToString(";")
                threadSafeActionGroupUtil.addAnalysisProperty("ClassPaths",classPaths)
            }

            //If valid elements, return analysis options
            threadSafeActionGroup = arrayOf(
                RunAnalysisAction("Run Analysis",
                    "Run the ThreadSafe Analysis on this project",
                    AllIcons.Actions.Execute),
                ClearMarkersAction("Clear Markers",
                                 "Clears all Findings data from UI",
                                    AllIcons.Actions.CloseDarkGrey)
            )
        }

        return threadSafeActionGroup
    }
}