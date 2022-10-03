package com.contemplateltd.intellij.threadsafeintegration.actions.threadSafeActionGroup

import com.contemplateltd.intellij.threadsafeintegration.actions.runAnalysisAction.RunAnalysisUtil
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/*
    Util class that provides helper functions to ThreadSafeActionGroup class
 */
class ThreadSafeActionGroupUtil(project:Project) {

    //Creates empty analysisProperties file if it does not exist
    private var pluginName = ""
    private var runAnalysisUtil:RunAnalysisUtil? = null
    private var project: Project? = null
    private var analysisPathsPropertiesFile = ""
    init{
        pluginName =  getPluginName()
        this.project = project
        runAnalysisUtil = RunAnalysisUtil(this.project!!)
        runAnalysisUtil!!.createAnalysisDirectory()
        runAnalysisUtil!!.createProjectDirectory()
        runAnalysisUtil!!.createResultsDirectory()
        analysisPathsPropertiesFile = Paths.get(PathManager.getPluginsPath(),pluginName,"Analysis",project.name,"analysisPaths.properties").toAbsolutePath().toString()
        createAnalysisPathsPropertiesFile()
    }

    private fun getPluginName():String{
        val prop = Properties()
        var requiredPropertyValue = ""
        try {
            //load a properties file from class path, inside static method
            val propertiesInputStream = javaClass.classLoader.getResourceAsStream("plugin.Properties")
            prop.load(propertiesInputStream)
            propertiesInputStream?.close()
            //get the property value and print it out
            if(prop.containsKey("PluginName")){
                requiredPropertyValue =  prop.getProperty("PluginName").toString()
            }
        } catch (error:Exception) {
            println(error)
            println("Error in ThreadSafeActionGroupUtil")
        }
        return requiredPropertyValue
    }

    //Creates AnalysisPaths properties file if it does not exist
    private fun createAnalysisPathsPropertiesFile(){
        if(!Files.exists(Paths.get(analysisPathsPropertiesFile))){
            Files.createFile(Paths.get(analysisPathsPropertiesFile))
        }
    }

     fun addAnalysisProperty(propertyName:String,propertyValue:String){
        val prop = Properties()
        try {
            val propertiesInputStream = Files.newInputStream(Paths.get(analysisPathsPropertiesFile))
            prop.load(propertiesInputStream)
            propertiesInputStream.close()
            prop.clear()
            prop.setProperty(propertyName,propertyValue)
            val propertiesOutputStream = Files.newOutputStream(Paths.get(analysisPathsPropertiesFile))
            prop.store(propertiesOutputStream,"")

        } catch (error:Exception) {
            println(error)
            println("Error in ThreadSafeActionGroupUtil")
        }
    }
}