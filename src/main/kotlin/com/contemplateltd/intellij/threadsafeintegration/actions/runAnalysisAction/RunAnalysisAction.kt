package com.contemplateltd.intellij.threadsafeintegration.actions.runAnalysisAction

import com.contemplateltd.intellij.threadsafeintegration.actions.runAnalysisAction.UI.EnableRulesPrompt
import com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.PreferencesPageUtil
import com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow.FindingsWindowController
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl
import com.intellij.openapi.wm.WindowManager
import java.io.File
import java.util.*
import javax.swing.Icon


/*
    Run Analysis Action class to trigger the analysis
 */
class RunAnalysisAction(actionName: String, actionDescription: String, actionIcon: Icon) : AnAction(actionName, actionDescription, actionIcon),DumbAware {
    override fun actionPerformed(event: AnActionEvent) {

        val project = event.project
        //Project Check
        if(project == null){
            println("Project does not exist || The file is not part of a project")
            return
        }

        //Check if no rules are selected in preferences
        val preferencesPageUtil = PreferencesPageUtil()
        var enabledRulesLength = 0
        for(rule in   preferencesPageUtil.createPreferenceData("current").values){
            if(rule["enabled"]=="true"){
                enabledRulesLength+=1
                break
            }
        }

        if(enabledRulesLength==0){
            println("Please enable at least one rule in ThreadSafe Preferences Page.")
            val frame = WindowManager.getInstance().getFrame(project)
            EnableRulesPrompt.enableRulePrompt(project,frame)
            return
        }


        //Initiating Utility class
        val runAnalysisUtil = RunAnalysisUtil(project)
        val ruleProvider = runAnalysisUtil.getRuleProvider()
        val outPutDirectoryPath = runAnalysisUtil.getAnalysisResultsDirectoryPath()

        //Checks if valid RuleProvider is present
        if(ruleProvider==null){
            println("Rule Provider is empty")
            return
        }

        //Path to directories containing .class files
        val classPaths:Vector<String> = Vector<String>()

        for(module in ModuleManager.getInstance(project).modules){
            if(CompilerModuleExtension.getInstance(module)!!.compilerOutputPath!=null){
                classPaths.add(CompilerModuleExtension.getInstance(module)!!.compilerOutputPath!!.canonicalPath)
            }

        }

        //LibPaths
        val libPaths:Vector<String> = Vector<String>()


        //List of Canonical names of specified .java files to analyze
        val classesToAnalyze: Vector<String> = Vector<String>()
        var classesAnalysisString:String? = null
        val requestType = runAnalysisUtil.checkAnalysisRequestType()

        if(requestType =="class"){
            classesAnalysisString = runAnalysisUtil.getAnalysisProperty("ClassPaths")
        }else if(requestType==""){
            println("Request Type Not Specified")
            return
        }

        if (classesAnalysisString != null) {
            for(classPath in classesAnalysisString.split(";")){
                classesToAnalyze.add(classPath)
            }
        }
        //Path to source directory of project
        val sourceDirs: Vector<File> = Vector<File>()
        for(vf in ProjectRootManagerImpl.getInstance(project).contentSourceRoots){
            sourceDirs.add(File(vf.canonicalPath.toString()))
        }

        //CallBack function to trigger ThreadSafe Analysis if project compiled successfully
        val compileCallback = CompileStatusNotification { aborted, errors, _, _ ->
            if(!(aborted || errors>0)){
               println("Compilation Completed Successfully")
                //Triggering Analysis and Tracking Progress
                ProgressManager.getInstance().run(object :Task.Backgroundable(project,"ThreadSafe analysis",true){

                    //Create Analysis Controller and trigger analysis
//                    override fun run(indicator: ProgressIndicator) {
//
//                    }

                    override fun onFinished() {
                        super.onFinished()
                        println("Task Finished Successfully")
                        println("Show Analysis Results")
                        FindingsWindowController(project).displayAnalysisResults()
                    }
                })

            }else{
                println("Error in project Compilation")
            }
        }

        //Compile Project and Run ThreadSafe Analysis
        for(module in ModuleManager.getInstance(project).modules){
            CompilerManager.getInstance(project).compile(module, compileCallback)
        }

    }
}