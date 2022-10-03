package com.contemplateltd.intellij.threadsafeintegration.actions.runAnalysisAction

import com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.PreferencesPageUtil
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import org.w3c.dom.Document
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


/*
    Util class that provided helper functions for RunAnalysisAction class
 */
class RunAnalysisUtil(project: Project) {


    private var projectAnalysisDirectoryPath = ""
    private var project:Project? = null
    private var pluginName = ""
    private var currentRuleConfigPath = ""
    //Needed for ruleConfigurations file
    private val preferencesPageUtil = PreferencesPageUtil()
    private var rulesConfigFilePathForAnalysis = ""
    private var analysisResultsDirectoryPath = ""
    private var analysisPathsPropertiesFile = ""


    init{
        this.project = project
        pluginName = getPluginName()
        currentRuleConfigPath = Paths.get(PathManager.getPluginsPath(), pluginName, "Rules", "ruleConfigurations.xml").toAbsolutePath().toString()
        rulesConfigFilePathForAnalysis = Paths.get(PathManager.getPluginsPath(),pluginName,"Rules","rulesConfigFileForAnalysis.xml").toAbsolutePath().toString()

        createAnalysisDirectory()
        createProjectDirectory()
        createResultsDirectory()

        //Create Rule Configurations file if they do not exist
        preferencesPageUtil.createRuleConfigurations()

        //Creating RulesConfigFileForAnalysis
        rulesConfigFileGenerator()

        analysisPathsPropertiesFile = Paths.get(PathManager.getPluginsPath(),pluginName,"Analysis",project.name,"analysisPaths.properties").toAbsolutePath().toString()
    }


    //Create Analysis directory to store analysis results if it does not exist
     fun createAnalysisDirectory(){
        val analysisDirectoryPath = Paths.get(PathManager.getPluginsPath(),pluginName,"Analysis")
        if(!Files.exists(analysisDirectoryPath)){
            Files.createDirectory(analysisDirectoryPath)
        }
    }

    //Create project directory in analysis to store results if not exists
     fun createProjectDirectory(){
        projectAnalysisDirectoryPath = Paths.get(PathManager.getPluginsPath(),pluginName,"Analysis",project!!.name).toAbsolutePath().toString()
        if(!Files.exists(Paths.get(projectAnalysisDirectoryPath))){
            Files.createDirectory(Paths.get(projectAnalysisDirectoryPath))
        }
    }

    fun createResultsDirectory(){
        analysisResultsDirectoryPath = Paths.get(PathManager.getPluginsPath(),pluginName,"Analysis", project!!.name,"results").toAbsolutePath().toString()
        if(!Files.exists(Paths.get(analysisResultsDirectoryPath))){
            Files.createDirectory(Paths.get(analysisResultsDirectoryPath))
        }
    }

    private fun getPluginName(): String {
        val prop = Properties()
        var requiredPropertyValue = ""
        try {
            //load a properties file from class path, inside static method
            val propertiesInputStream = javaClass.classLoader.getResourceAsStream("plugin.Properties")
            prop.load(propertiesInputStream)
            propertiesInputStream?.close()
            if(prop.containsKey("PluginName")){
                requiredPropertyValue =  prop.getProperty("PluginName").toString()
            }
        } catch (error:Exception) {
            println(error)
            println("In Run Analysis Util")
        }
        return requiredPropertyValue
    }

    fun getCurrentRuleConfigPath() : String {
        return preferencesPageUtil.getCurrentRuleConfigPath()
    }


    private fun rulesConfigFileGenerator(){

        val xmlFile = File(currentRuleConfigPath)
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlDoc = dBuilder.parse(xmlFile)
        xmlDoc.documentElement.normalize()

        //finding elements
        val rules = xmlDoc.getElementsByTagName("rule")

        for(idx in 0 until rules.length){
            rules.item(idx).attributes.removeNamedItem("category")
            rules.item(idx).attributes.removeNamedItem("name")
        }


        xmlWriter(xmlDoc,rulesConfigFilePathForAnalysis)

    }

    private fun xmlWriter(myDocument : Document, savePath : String){
        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        val output = StreamResult(File(savePath))
        val input: Source = DOMSource(myDocument)
        transformer.transform(input, output)

    }

    fun getAnalysisResultsDirectoryPath(): String {
        return analysisResultsDirectoryPath
    }

     fun getAnalysisProperty(propertyName:String):String{

        var propertyValue = ""

        val prop = Properties()
        try {
            val propertiesInputStream = Files.newInputStream(Paths.get(analysisPathsPropertiesFile))
            prop.load(propertiesInputStream)
            propertiesInputStream.close()
            propertyValue = prop.getProperty(propertyName)

        } catch (error:Exception) {
            println(error)
        }

        return propertyValue
    }

     fun checkAnalysisRequestType(): String {
        val prop = Properties()
        var propertyType = ""
        try {
            val propertiesInputStream = Files.newInputStream(Paths.get(analysisPathsPropertiesFile))
            prop.load(propertiesInputStream)
            propertiesInputStream.close()
            if(prop.containsKey("ProjectPath")){
                propertyType = "project"
            }else if (prop.containsKey("ClassPaths")){
                propertyType = "class"
            }

        } catch (error:Exception) {
            println(error)
            println("Error in RunAnalysisUtil")
        }
        return propertyType
    }
}