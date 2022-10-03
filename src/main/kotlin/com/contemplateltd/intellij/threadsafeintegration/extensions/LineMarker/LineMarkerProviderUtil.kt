package com.contemplateltd.intellij.threadsafeintegration.extensions.LineMarker

import com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.PreferencesPageUtil
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import org.json.JSONArray
import org.json.JSONObject
import org.json.XML
import org.w3c.dom.Document
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.HashMap

class LineMarkerProviderUtil {


    private var findingsFilePath = ""
    private var preferencesFilePath = ""
    private var analysisProjectPropertiesFilePath = ""

    private var errorsMap = HashMap<Int, HashMap<String, String>>()
    private var eachFileErrorLocation: HashMap<String, Vector<HashMap<String, String>>> = HashMap()
    private val findingsLocationInfo = HashMap<Int, Vector<HashMap<String, String>>>()
    private val preferenceData = HashMap<String, HashMap<String, String>>()


    private var filesCreated = false


    private var filesCreationTime: FileTime? = null

    private var project:Project? = null


    companion object{
        private var clearMarkersEnabled = false
        fun setClearMarkersEnabled(booleanValue:Boolean){
            clearMarkersEnabled = booleanValue
        }

        fun getClearMarkersEnabled():Boolean{
            return clearMarkersEnabled
        }
    }


    fun setProject(project:Project){
        this.project = project
    }

    //To Add markers if analysis is run and all files are present
     fun checkIfFilesPresent(projectName:String):Boolean {

        //Setup Required Paths
        findingsFilePath  = Paths.get(PathManager.getPluginsPath(),getPluginName(),"Analysis", projectName,"results","findings.xml").toAbsolutePath().toString()
        preferencesFilePath = Paths.get(PathManager.getPluginsPath(),getPluginName(),"Rules","ruleConfigurations.xml").toAbsolutePath().toString()
        analysisProjectPropertiesFilePath = Paths.get(PathManager.getPluginsPath(),getPluginName(),"Analysis",projectName,"LineMarker","lineMarkerProperties.properties").toAbsolutePath().toString()

        if (!Files.exists(Paths.get(findingsFilePath)) || !Files.exists(Paths.get(preferencesFilePath)) ||!Files.exists(Paths.get(analysisProjectPropertiesFilePath)) ) {
            return false
        }


        return true
    }

    private fun setFileCreationTime(){
        filesCreationTime = Files.getLastModifiedTime(Paths.get(findingsFilePath))
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



    private fun readAnalysisResults()  {

        val xmlFile: File?
        val dbFactory: DocumentBuilderFactory?
        val dBuilder: DocumentBuilder?
        val xmlDoc: Document?

        try{
            xmlFile = File(findingsFilePath)
            dbFactory = DocumentBuilderFactory.newInstance()
            dBuilder = dbFactory.newDocumentBuilder()
            xmlDoc = dBuilder.parse(xmlFile)
        }catch (error:Error){
            println("Error Reading Findings File")
            return
        }

        xmlDoc.documentElement.normalize()

        //finding elements
        val findings = xmlDoc.getElementsByTagName("finding")

        //Create errorDetail Map for each finding present in findings.xml and store in errorsMap.
        //errorsMap is used to display findings in Findings table
        for(idx in 0 until findings.length){
            val errorKey = findings.item(idx).attributes.item(0).nodeValue
            val childNodes = findings.item(idx).childNodes
            var messageVal = ""
            val errorDetail = HashMap<String,String>()
            errorDetail["type"] = errorKey
            //Error Name
            for( idx2 in 0 until childNodes.length){

                if(childNodes.item(idx2).nodeName == "info"){
                    val infoChildNodes = childNodes.item(idx2).childNodes
                    for(idx3 in 0 until infoChildNodes.length){
                        if (infoChildNodes.item(idx3).nodeName =="message"){
                            messageVal = infoChildNodes.item(idx3).attributes.getNamedItem("value").nodeValue

                        }
                    }
                }
            }


            //ResourceName and ClassName
            for( idx2 in 0 until childNodes.length){
                if(childNodes.item(idx2).nodeName == "locations"){
                    val locationNodes =  childNodes.item(idx2).childNodes
                    for(idx3 in 0 until locationNodes.length){
                        if(locationNodes.item(idx3).nodeName == "field" || locationNodes.item(idx3).nodeName == "instruction"  ){
                            val resource = locationNodes.item(idx3).attributes.getNamedItem("filename").nodeValue
                            val className =  locationNodes.item(idx3).attributes.getNamedItem("className").nodeValue
                            errorDetail["resource"] = resource
                            errorDetail["className"] = className
                            break
                        }

                    }
                }
            }
            errorDetail["name"] = messageVal
            errorsMap[idx] = errorDetail.clone() as HashMap<String, String>

        }
    }

    private fun parseFindingsFile(projectName: String){

        //Parse only if a finding is present
        if(errorsMap.size==0){
            return
        }


        val prettyPrinterIndentFactor = 2

        val xmlStr: String?

        try{
            xmlStr = File(findingsFilePath).readText()
        }catch (error:Error){
            println("Error Reading Findings File")
            return
        }

        //Prepare JSON Findings file
        val jsonObj = XML.toJSONObject(xmlStr)
        val jsonFile = Paths.get(PathManager.getPluginsPath(),getPluginName(),"Analysis",projectName,"results","findings.json").toAbsolutePath().toString()

        File(jsonFile).writeText(jsonObj.toString(prettyPrinterIndentFactor))


        var findingsArray: JSONArray? = null

        if(jsonObj.getJSONObject("findings").get("finding").javaClass == JSONObject().javaClass){
            val findingsObject = jsonObj.getJSONObject("findings").getJSONObject("finding")
            findingsArray = JSONArray(arrayOf(findingsObject))

        }else{
            findingsArray = jsonObj.getJSONObject("findings").getJSONArray("finding")
        }

        for (idx in 0 until findingsArray!!.length()){
            val finding = findingsArray.get(idx) as JSONObject
            val locations = finding.get("locations") as JSONObject
            val findingKey = finding.getJSONObject("info").getJSONObject("message").get("value").toString()

            var messageArray: JSONArray?
            var messageObject: JSONObject?

            val messageKeyMap = kotlin.collections.HashMap<String,String>()

            if(finding.getJSONObject("info").getJSONObject("message").get("location").javaClass == JSONArray().javaClass ){
                messageArray = finding.getJSONObject("info").getJSONObject("message").getJSONArray("location")

                for(messageIdx in 0 until messageArray.length()){
                    if(messageArray.getJSONObject(messageIdx).toMap().containsKey("message")){
                        messageKeyMap[messageArray.getJSONObject(messageIdx).get("key").toString()] = messageArray.getJSONObject(messageIdx).get("message") as String
                    }
                }
            }else{
                messageObject = finding.getJSONObject("info").getJSONObject("message").getJSONObject("location")
                if(messageObject.toMap().containsKey("message")){
                    messageKeyMap[messageObject.get("key").toString()] = messageObject.get("message") as String
                }
            }

            findingsLocationInfo[idx] = Vector<HashMap<String, String>>()

            for(key in locations.toMap().keys){

                var locationInfoObject: JSONObject?
                var locationInfoArray: JSONArray?
                val locationsForEach =  kotlin.collections.HashMap<String,String>()

                if(locations.get(key)!!.javaClass == JSONArray().javaClass){
                    locationInfoArray = locations.get(key) as JSONArray

                    for(idx2 in 0 until locationInfoArray.length()){

                        if ( (locationInfoArray.getJSONObject(idx2).toMap().keys.contains("line")) ){
                            locationsForEach["filename"] = locationInfoArray.getJSONObject(idx2).get("filename") as String
                            locationsForEach["line"] = locationInfoArray.getJSONObject(idx2).get("line").toString()
                            locationsForEach["className"] = locationInfoArray.getJSONObject(idx2).get("className").toString()
                            locationsForEach["key"] = locationInfoArray.getJSONObject(idx2).get("key").toString()
                            locationsForEach["locationMessage"] = messageKeyMap[locationsForEach["key"].toString()].toString()
                            locationsForEach["findingKey"] = findingKey

                            //Add if method present
                            if(locationInfoArray.getJSONObject(idx2).toMap().containsKey("method")){
                                locationsForEach["method"] = locationInfoArray.getJSONObject(idx2).get("method").toString()
                            }

                            //Add if name present
                            if(locationInfoArray.getJSONObject(idx2).toMap().containsKey("name")){
                                locationsForEach["name"] = locationInfoArray.getJSONObject(idx2).get("name").toString()
                            }

                            //Add if desc present
                            if(locationInfoArray.getJSONObject(idx2).toMap().containsKey("desc")){
                                locationsForEach["desc"] = locationInfoArray.getJSONObject(idx2).get("desc").toString()
                            }

                            //Add type if present
                            if(locationInfoArray.getJSONObject(idx2).toMap().containsKey("type")){
                                locationsForEach["type"] = locationInfoArray.getJSONObject(idx2).get("type").toString()
                            }

                            findingsLocationInfo[idx]!!.add(locationsForEach.clone() as HashMap<String, String>?)
                        }
                    }
                }else{
                    locationInfoObject = locations.get(key) as JSONObject

                    if(locationInfoObject.toMap().keys.contains("line")){
                        locationsForEach["filename"] = locationInfoObject.get("filename") as String
                        locationsForEach["line"] = locationInfoObject.get("line").toString()
                        locationsForEach["className"] = locationInfoObject.get("className").toString()
                        locationsForEach["key"] = locationInfoObject.get("key").toString()
                        locationsForEach["locationMessage"] = messageKeyMap[locationsForEach["key"].toString()].toString()
                        locationsForEach["findingKey"] = findingKey

                        //Add method if present
                        if(locationInfoObject.toMap().containsKey("method")){
                            locationsForEach["method"] = locationInfoObject.get("method").toString()
                        }

                        //Add name if present
                        if(locationInfoObject.toMap().containsKey("name")){
                            locationsForEach["name"] = locationInfoObject.get("name").toString()
                        }

                        //Add desc if present
                        if(locationInfoObject.toMap().containsKey("desc")){
                            locationsForEach["desc"] = locationInfoObject.get("desc").toString()
                        }

                        //Add type if present
                        if(locationInfoObject.toMap().containsKey("type")){
                            locationsForEach["type"] = locationInfoObject.get("type").toString()
                        }


                        findingsLocationInfo[idx]!!.add(locationsForEach.clone() as HashMap<String, String>?)
                    }
                }
            }
        }

    }



    private fun createMarkerProviderInformation() {

//        for (findingKey in findingsLocationInfo.keys) {
        for (idx in 0 until  findingsLocationInfo.size) {
            //
            val findingInfo = errorsMap[idx]!!
//            val findingKey:String = findingInfo["findingKey"]!!
            val errorType = findingInfo["type"]
            val errorTypeName = preferenceData[errorType]!!["name"]
            val severityVal = preferenceData[errorType]!!["severity"]

            var objectName = ""
            val findingLocations = findingsLocationInfo[idx]!!
            for (eachFindingLocation in findingLocations ) {
                val fileName = eachFindingLocation["filename"]!!
                if (!eachFileErrorLocation.containsKey(fileName)) {
                    eachFileErrorLocation[fileName] = Vector<HashMap<String, String>>()
                }

                if(eachFindingLocation["name"]!=null){
                    objectName = eachFindingLocation["name"].toString()
                }

                eachFindingLocation["severity"] = severityVal!!

                if(eachFindingLocation["locationMessage"] == "Problem location"){
                    eachFindingLocation["toolTipText"] = eachFindingLocation["findingKey"]!!
                }else{
                    eachFindingLocation["toolTipText"] = errorTypeName!! + ": " + eachFindingLocation["locationMessage"]
                    if(objectName!=""){
                        if(eachFindingLocation["locationMessage"].toString().lowercase().contains("read") ){
                            eachFindingLocation["toolTipText"]+= " from field $objectName"
                        }else if(eachFindingLocation["locationMessage"].toString().lowercase().contains("write")) {
                            eachFindingLocation["toolTipText"]+= " to field $objectName"
                        }
                    }
                }
                eachFileErrorLocation[fileName]!!.add(eachFindingLocation)
            }
        }
    }

    private fun createPreferenceData()  {

        val xmlFile = File(preferencesFilePath)
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlDoc = dBuilder.parse(xmlFile)
        xmlDoc.documentElement.normalize()

        //finding elements
        val rules = xmlDoc.getElementsByTagName("rule")

        //Populating enabled and severity Hashmap
        for(idx in 0 until rules.length) {

            //Getting all rule attributes as node List
            val attributes = rules.item(idx).attributes

            //Extracting all attribute values
            val category = attributes.getNamedItem("category").nodeValue
            val enabled = attributes.getNamedItem("enabled").nodeValue
            val key =  attributes.getNamedItem("key").nodeValue
            val name = attributes.getNamedItem("name").nodeValue
            val severity = attributes.getNamedItem("severity").nodeValue

            //HashMap of all attributes for a given rule
            val attributesMap = java.util.HashMap<String, String>()
            attributesMap["enabled"] = enabled
            attributesMap["name"] = name
            attributesMap["severity"] = severity
            attributesMap["category"] = category
            val paramsMap = java.util.HashMap<String, String>()


            //Checking for additional parameters
            if( rules.item(idx).childNodes.length > 0){
                for(idx2 in 0 until rules.item(idx).childNodes.length){
                    val childNode = rules.item(idx).childNodes.item(idx2)
                    if(childNode.nodeName =="param" ){
                        val paramName  = childNode.attributes.getNamedItem("name").nodeValue
                        val paramValue = childNode.attributes.getNamedItem("value").nodeValue
                        paramsMap[paramName] = paramValue
                        break   //Ensures that only one param is added
                    }
                }
            }
            //Putting rule attributes for a given key in final HashMap
            this.preferenceData[key] = attributesMap.clone() as HashMap<String, String>
        }

    }


    fun getErrorsMap(): HashMap<Int, HashMap<String, String>> {
        return errorsMap
    }

    fun getEachFileErrorLocationEachError(): HashMap<String, Vector<HashMap<String, String>>> {
        return eachFileErrorLocation
    }

    fun createLineMarkerInformation(projectName:String) {

        val newFileCreationTime = Files.getLastModifiedTime(Paths.get(findingsFilePath))

        if(clearMarkersEnabled){
            //Clear existing data
            errorsMap.clear()
            eachFileErrorLocation.clear()
            findingsLocationInfo.clear()
            preferenceData.clear()
            return
        }


        if(newFileCreationTime!=filesCreationTime){
            setFileCreationTime()
            filesCreated = false

        }
        if(!filesCreated && checkIfFilesPresent(projectName)){
            filesCreated = true
            val preferencesPageUtil = PreferencesPageUtil()
            preferencesPageUtil.createRuleConfigurations()

            //Clear existing data
            errorsMap.clear()
            eachFileErrorLocation.clear()
            findingsLocationInfo.clear()
            preferenceData.clear()


            //Get Analysis Results
            createPreferenceData()
            readAnalysisResults()
            parseFindingsFile(projectName)
            createMarkerProviderInformation()



            DaemonCodeAnalyzer.getInstance(project!!).restart()


        }else if(!checkIfFilesPresent(projectName)){
            filesCreated = false
        }
    }

}