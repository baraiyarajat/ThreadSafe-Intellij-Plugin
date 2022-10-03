package com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage

import com.intellij.openapi.application.PathManager
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult




/*
    Provides helper functions for PreferencesPageController
    Deals with all file read/write operations
 */
class PreferencesPageUtil {


    private var pluginName = ""
    private var defaultRuleConfigPath: String = ""
    private var currentRuleConfigPath: String = ""



    init {
        pluginName = getPreferencesPageProperty("PluginName")
        defaultRuleConfigPath = Paths.get(PathManager.getPluginsPath(), pluginName, "Rules", "defaultRuleConfigurations.xml").toAbsolutePath().toString()
        currentRuleConfigPath = Paths.get(PathManager.getPluginsPath(), pluginName, "Rules", "ruleConfigurations.xml").toAbsolutePath().toString()
    }


    /*
        Creates defaultRuleConfigurations.xml file in plugin installation directory if it does not exist
        This file is required to initialize ruleConfigurations.xml file and to restore default rule configurations
     */
    private fun createDefaultRuleConfigurations(defaultPath: Path) {

        //Read .xml file from plugin package
        val defaultRuleConfigurationsXmlText = this::class.java.getResource("/ruleConfigurations/defaultRuleConfigurations.xml")!!.readText()

        //Create Xml file from string to path
        val factory = DocumentBuilderFactory.newInstance()
        val builder: DocumentBuilder = factory.newDocumentBuilder()
        val doc: Document = builder.parse(InputSource(StringReader(defaultRuleConfigurationsXmlText)))
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        val source = DOMSource(doc)

        val result = StreamResult(File(defaultPath.toAbsolutePath().toString()))
        transformer.transform(source, result)
    }


    /*
        Creates ruleConfigurations.xml file in the plugin installation directory if it does not exist
        If the directory to store these .xml files is not present, it creates one.
        This file will be modified when user interacts
        returns true if created successfully
     */

     fun createRuleConfigurations(): Boolean {

        val rulesDirectoryPath = Paths.get(PathManager.getPluginsPath(), pluginName, "Rules")
        val defaultPath = Paths.get(defaultRuleConfigPath)
        var ruleConfigurationFileCreated = false
        try {

            //Create the Rules directory if it does not exist
            if (!Files.exists(rulesDirectoryPath)) {
                Files.createDirectory(rulesDirectoryPath)
            }

            //create defaultRuleConfigurations.xml file
            if(Files.exists(defaultPath)){
                Files.delete(defaultPath)
            }
            createDefaultRuleConfigurations(defaultPath)


            //Initialize ruleConfigurations.xml if it does not exist
            if (!Files.exists(Paths.get(currentRuleConfigPath))) {
                val src = Paths.get(defaultRuleConfigPath)
                val dest = Paths.get(currentRuleConfigPath)
                Files.copy(src, dest)
            }

            ruleConfigurationFileCreated = true

        }catch (error:Exception){
            println(error)
        }
        return ruleConfigurationFileCreated
    }


    /*
        Gets specified property value from preferencesPage.properties file
        If Property does not exist, returns an empty string
     */

    fun getPreferencesPageProperty(propertyName:String):String{
        val prop = Properties()
        var requiredPropertyValue = ""
        try {
            //load a properties file from class path, inside static method
            val propertiesInputStream = javaClass.classLoader.getResourceAsStream("preferencesPage.properties")
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

    /*
        Parses specified XML file and creates HashMap object containing ruleConfiguration data
     */

     fun createPreferenceData(configurationType: String, newFilePath:String? = null): HashMap<String, HashMap<String, String>> {

         var filePath = ""

         //Set file path as per configuration type. Either "default" or "current"
         when(configurationType){
             "default"-> filePath = defaultRuleConfigPath
             "current" -> filePath = currentRuleConfigPath
             "imported" -> filePath = newFilePath!!
         }


        //Tries creating specified configuration. If it cannot create, creates one from default configuration
        var xmlDoc: Document? = null
         try{
            val xmlFile = File(filePath)
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            xmlDoc = dBuilder.parse(xmlFile)
            xmlDoc.documentElement.normalize()
        } catch (error:Error){
            filePath = defaultRuleConfigPath
            val xmlFile = File(filePath)
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            xmlDoc = dBuilder.parse(xmlFile)
            xmlDoc.documentElement.normalize()
        }




        //finding elements
        val rules = xmlDoc!!.getElementsByTagName("rule")
        val rulePreferencesMap = HashMap<String, HashMap<String, String>>()


        //Populating enabled and severity Hashmap
        for (idx in 0 until rules.length) {

            //Getting all rule attributes as node List
            val attributes = rules.item(idx).attributes

            //Extracting all attribute values
            val category = attributes.getNamedItem("category").nodeValue
            val enabled = attributes.getNamedItem("enabled").nodeValue
            val key = attributes.getNamedItem("key").nodeValue
            val name = attributes.getNamedItem("name").nodeValue
            val severity = attributes.getNamedItem("severity").nodeValue

            //HashMap of all attributes for a given rule
            val attributesMap = HashMap<String, String>()
            attributesMap["enabled"] = enabled
            attributesMap["name"] = name
            attributesMap["severity"] = severity
            attributesMap["category"] = category

            //Checking for additional parameters
            if (rules.item(idx).childNodes.length > 0) {
                for (idx2 in 0 until rules.item(idx).childNodes.length) {
                    val childNode = rules.item(idx).childNodes.item(idx2)
                    if (childNode.nodeName == "param") {

                        val paramName = childNode.attributes.getNamedItem("name").nodeValue
                        val paramValue = childNode.attributes.getNamedItem("value").nodeValue
                        attributesMap["paramName"] = paramName
                        attributesMap["paramValue"] = paramValue
                        break   //Ensures that only one param is added
                    }
                }
            }

            //Putting rule attributes for a given key in final HashMap
            rulePreferencesMap[key] = attributesMap.clone() as HashMap<String, String>
        }
        return rulePreferencesMap
    }

    //Preferences Data Writer
     fun xmlDocModifier(modifiedEnabledValuesMap : HashMap<String, Boolean>,
                               modifiedSeverityValuesMap : HashMap<String, String>,
                               modifiedParamValuesMap : HashMap<String, Int>) {

        val xmlFile = File(currentRuleConfigPath)
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlDoc = dBuilder.parse(xmlFile)
        xmlDoc.documentElement.normalize()

        //finding elements
        val rules = xmlDoc.getElementsByTagName("rule")

        for (idx in 0 until rules.length) {
            //Getting all rule attributes as node List
            val attributes = rules.item(idx).attributes

            //Extracting all attribute values
            val name = attributes.getNamedItem("name").nodeValue
            //Adding modified enabled values
            attributes.getNamedItem("enabled").nodeValue = modifiedEnabledValuesMap[name].toString()
            //Adding modified severity values
            attributes.getNamedItem("severity").nodeValue = modifiedSeverityValuesMap[name].toString()

            if (modifiedParamValuesMap.containsKey(name)) {

                val paramChildNodes = rules.item(idx).childNodes

                for (idx2 in 0 until paramChildNodes.length) {
                    if (paramChildNodes.item(idx2).nodeName.equals("param")) {
                        paramChildNodes.item(idx2).attributes.getNamedItem("value").nodeValue = modifiedParamValuesMap[name].toString()
                    }
                }


            }
        }

        xmlWriter(xmlDoc, currentRuleConfigPath)

    }

    //Preferences Data Writer
    private fun xmlWriter(myDocument: Document, savePath: String) {
        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        val output = StreamResult(File(savePath))
        val input: Source = DOMSource(myDocument)
        transformer.transform(input, output)

    }

    fun getCurrentRuleConfigPath():String{
        return currentRuleConfigPath
    }


}