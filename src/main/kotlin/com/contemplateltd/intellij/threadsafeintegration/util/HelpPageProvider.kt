package com.contemplateltd.intellij.threadsafeintegration.util

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.StreamUtil
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/*
    Provides API for ThreadSafe Help HTML page
*/
class HelpPageProvider {


    private val pluginName = getPluginProperty("PluginName")

    //Opens Help HTML page in the Editor for specified ID
    fun openHtmlEditor(elementID: String){

        //Get Open Project
        val project = ProjectManager.getInstance().openProjects[0]

        //Get help file content
        val html = this::class.java.getResourceAsStream("/help/index.html")
        var htmlContent = String(StreamUtil.readBytes(html!!), StandardCharsets.UTF_8)


        //Create Help Directory
        if (!Files.exists(Paths.get(PathManager.getPluginsPath(), pluginName, "Help"))){
            Files.createDirectory(Paths.get(PathManager.getPluginsPath(), pluginName, "Help"))
        }

        //Create helper.js file
        if (!Files.exists(Paths.get(PathManager.getPluginsPath(), pluginName, "Help","helper.js"))){
            Files.createFile(Paths.get(PathManager.getPluginsPath(), pluginName, "Help","helper.js"))
        }

        val imagePathMap = createHelpImagesInPluginFolder()

        //replace image src values with actual paths
        for(key in imagePathMap.keys){
            htmlContent = htmlContent.replace("./images/$key",imagePathMap[key]!!.toAbsolutePath().toString())
        }


        val ruleName = getRuleNameFromElementID(elementID)

        //Edit Helper.js to scroll to particular id
        val jsHelpText = "document.getElementById('$ruleName').scrollIntoView();"
        val helperJsPath = Paths.get(PathManager.getPluginsPath(), pluginName, "Help","helper.js")
        Files.writeString(helperJsPath,jsHelpText)


        htmlContent = htmlContent.replace("helper.js",helperJsPath.toAbsolutePath().toString())

        //Close the help file if open
        for( file in FileEditorManagerImpl.getInstance(project).openFiles){
            if(file.name == "ThreadSafe Help"){
                FileEditorManager.getInstance(project).closeFile(file)
                break
            }
        }

        //Open the help HTML page
        HTMLEditorProvider.openEditor(project, "ThreadSafe Help", htmlContent)

    }

    //Get specified property value from plugin.Properties file
    private fun getPluginProperty(propertyName:String):String{
        val prop = Properties()
        var requiredPropertyValue = ""
        try {
            //load a properties file from class path, inside static method
            val propertiesInputStream = javaClass.classLoader.getResourceAsStream("plugin.Properties")
            prop.load(propertiesInputStream)
            propertiesInputStream?.close()
            //get the property value and print it out
            if(prop.containsKey(propertyName)){
                requiredPropertyValue =  prop.getProperty(propertyName).toString()
            }
        } catch (error:Exception) {
            println(error)
        }
        return requiredPropertyValue
    }

    //Creates Images required for help page in local directory if they do not exist
    private fun createHelpImagesInPluginFolder() : HashMap<String, Path>{
        //Copy all Images to help folder in plugin
        //Create Help Directory
        if (!Files.exists(Paths.get(PathManager.getPluginsPath(), pluginName, "Help"))){
            Files.createDirectory(Paths.get(PathManager.getPluginsPath(), pluginName, "Help"))
        }

        //Create helper.js file
        if (!Files.exists(Paths.get(PathManager.getPluginsPath(), pluginName, "Help","images"))){
            Files.createDirectory(Paths.get(PathManager.getPluginsPath(), pluginName, "Help","images"))
        }

        val imagesName = Vector<String>()

        imagesName.add("accesses-invoke.png")
        imagesName.add("blocker.png")
        imagesName.add("critical.png")
        imagesName.add("delete.gif")
        imagesName.add("filter_ps.gif")
        imagesName.add("flat.gif")
        imagesName.add("groupdetail.png")
        imagesName.add("guards-button-select.png")
        imagesName.add("guards-different-guard-chosen.png")
        imagesName.add("guards-inconsistent.png")
        imagesName.add("guards-instance.png")
        imagesName.add("guards-maybeheld.png")
        imagesName.add("guards-mixed.png")
        imagesName.add("guards-static.png")
        imagesName.add("guards-unknown.png")
        imagesName.add("hierarchical.gif")
        imagesName.add("info.png")
        imagesName.add("location.png")
        imagesName.add("major.png")
        imagesName.add("markers.png")
        imagesName.add("minor.png")
        imagesName.add("preferences.png")
        imagesName.add("runAll.png")
        imagesName.add("runproject.png")
        imagesName.add("runSelection.png")
        imagesName.add("settings.png")
        imagesName.add("suppressions1.png")
        imagesName.add("suppressions2.png")
        imagesName.add("view.png")


        val imagePathMap = HashMap<String, Path>()
        //Image creation if image do not exist
        for(imageName in imagesName){
            val imagePath = Paths.get(PathManager.getPluginsPath(), pluginName, "Help","images",imageName)
            val image = HelpPageProvider::class.java.getResourceAsStream("/help/images/$imageName")
            Files.write(imagePath,StreamUtil.readBytes(image!!))
            imagePathMap[imageName] = imagePath
        }

        return imagePathMap
    }


    // Returns dictionary mapping Rule name and html link for navigation
    // Required to navigate to particular rule description in help page
    //If not found, returns empty string
    private fun getRuleNameFromElementID(elementID:String) : String{

        val ruleMap = kotlin.collections.HashMap<String,String>()

        ruleMap["Call to blocking method while holding lock"] = "Rule1"
        ruleMap["ConcurrentModificationException caught"] = "Rule2"
        ruleMap["Deadlock due to circularity in lock dependencies"] = "Rule3"
        ruleMap["Field reassigned while holding a lock on its value"] = "Rule4"
        ruleMap["Get/check/put used rather than putIfAbsent"] = "Rule5"
        ruleMap["Guard expression is not valid"] = "Rule6"
        ruleMap["Guard is not final"] = "Rule7"
        ruleMap["GuardedBy annotation violated"] = "Rule8"
        ruleMap["Inconsistent synchronization of accesses to a collection"] = "Rule9"
        ruleMap["Inconsistent synchronization of accesses to a field"] = "Rule10"
        ruleMap["Iterating over a synchronized collection view while holding a lock on the view"] = "Rule11"
        ruleMap["Iteration over collection view while not locking on the backing collection"] = "Rule12"
        ruleMap["Lock not released when method throws an exception"] = "Rule13"
        ruleMap["Mixed synchronization of accesses to a collection stored in a field"] = "Rule14"
        ruleMap["Mixed synchronization of accesses to a field"] = "Rule15"
        ruleMap["No lock held while iterating on a synchronized collection view"] = "Rule16"
        ruleMap["Non atomic Check/Put on threadsafe collection"] = "Rule17"
        ruleMap["Non atomic use of Get/Check/Put"] = "Rule18"
        ruleMap["Shared non threadsafe content"] = "Rule19"
        ruleMap["Synchronizing on a collection view"] = "Rule20"
        ruleMap["Synchronizing on reusable objects"] = "Rule21"
        ruleMap["Threadsafe collection consistently guarded"] = "Rule22"
        ruleMap["Threadsafe collection replaced by potentially unsafe collection"] = "Rule23"
        ruleMap["Unsafe iteration over synchronized collection"] = "Rule24"
        ruleMap["Unsynchronized access to field from asynchronously invoked method"] = "Rule25"
        ruleMap["Use of isLocked() and lock() rather tryLock()"] = "Rule26"
        ruleMap["Volatile field only written during initialization"] = "Rule27"

        if(!ruleMap.containsKey(elementID.trim()))      {
            return ""
        }
        return ruleMap[elementID.trim()]!!
    }
}