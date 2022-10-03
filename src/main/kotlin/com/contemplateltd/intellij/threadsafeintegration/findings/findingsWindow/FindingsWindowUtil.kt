package com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import org.json.JSONArray
import org.json.JSONObject
import org.json.XML
import org.w3c.dom.Document
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class FindingsWindowUtil(project: Project) {

    //Request Project
    private var project:Project? = null
    //FilePath to analysis results
    private var findingsFilePath = ""
    private var analysisProjectPropertiesFilePath = ""
    //Stores Errors in a map with key as error number
    private var errorsMap = HashMap<Int, HashMap<String, String>>()
    //Boolean to check if findings.xml is parsed
    private var analysisFileParsed = false
    //Rule Preferences data
    private var preferencesFilePath:String? = null
    private val preferenceData = HashMap<String, HashMap<String, String>>()

    private val findingsLocationInfo = HashMap<String, Vector<HashMap<String, String>>>()
    private val findingsAccessAndLocksInfo = HashMap<String, Vector<HashMap<String, String>>>()
    private val guardsInfo = HashMap<String, Vector<HashMap<String, String>>>()
    private var accessesAndLocksMethodData : java.util.HashMap<String, Vector<HashMap<String, String>>>? = null

    init{
        this.project = project

        //Setup Required Paths
        findingsFilePath  = Paths.get(PathManager.getPluginsPath(),getPluginName(),"Analysis", project.name,"results","findings.xml").toAbsolutePath().toString()
        preferencesFilePath = Paths.get(PathManager.getPluginsPath(),getPluginName(),"Rules","ruleConfigurations.xml").toAbsolutePath().toString()
        analysisProjectPropertiesFilePath = Paths.get(PathManager.getPluginsPath(),getPluginName(),"Analysis",project.name,"LineMarker","lineMarkerProperties.properties").toAbsolutePath().toString()
        //Create Preferences Data
        createPreferenceData()
        storeProjectName()
    }

    private fun storeProjectName(){


        val lineMarkerDirectoryPath = Paths.get(PathManager.getPluginsPath(),getPluginName(),"Analysis",project!!.name,"LineMarker").toAbsolutePath().toString()

        if(!Files.exists(Paths.get(lineMarkerDirectoryPath))){
            Files.createDirectory(Paths.get(lineMarkerDirectoryPath))
            println("Directory Created Successfully")
        }

        if(!Files.exists(Paths.get(analysisProjectPropertiesFilePath))){
            Files.createFile(Paths.get(analysisProjectPropertiesFilePath))
            println("File Created Successfully")
        }

        //Read  Properties File
        val prop = Properties()
        try {
            //load a properties file
            prop["ProjectName"] = project!!.name
            val propertiesOutputStream = Files.newOutputStream(Paths.get(analysisProjectPropertiesFilePath))
            prop.store(propertiesOutputStream,"")
            propertiesOutputStream.close()
            println("ProjectNameStored")
        }catch (error:Exception){
            println(error)
        }
    }

    //Reads Analysis results xml file and populates
     fun readAnalysisResults()  {

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
        //If parsed successfully
        analysisFileParsed = true
    }

    fun getErrorsMap(): HashMap<Int, HashMap<String, String>> {
        return errorsMap
    }

    fun findingsFileParsed(): Boolean {
        return analysisFileParsed
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

    /*
     *  Prepares HashMap for constructing FindingsTable by grouping errors by error type
     */
    fun getErrorGroupedByType(): HashMap<String,Vector<HashMap<String,String>>> {

       val errorGroupedByType = HashMap<String,Vector<HashMap<String,String>>>()

       for (errorIdx in errorsMap.keys){
           val errorDisplayDetails = HashMap<String,String>()
           errorDisplayDetails["idx"] = errorIdx.toString()
           errorDisplayDetails["description"] = errorsMap[errorIdx]!!["name"]!!
           errorDisplayDetails["resource"] = errorsMap[errorIdx]!!["resource"]!!
           errorDisplayDetails["path"] = errorsMap[errorIdx]!!["className"]!!

           if(!errorGroupedByType.containsKey(errorsMap[errorIdx]!!["type"]!!)){
               errorGroupedByType[errorsMap[errorIdx]!!["type"]!!] = Vector<HashMap<String,String>>()
           }

           errorGroupedByType[errorsMap[errorIdx]!!["type"]!!]!!.add(errorDisplayDetails.clone() as HashMap<String,String>)

       }
       return errorGroupedByType
   }

    /*
     *  Prepares HashMap for constructing FindingsTable by grouping errors by error severity based on error type
     */
    fun getErrorGroupedBySeverity(): HashMap<String,Vector<HashMap<String,String>>> {

        val errorGroupedBySeverity = HashMap<String,Vector<HashMap<String,String>>>()

        for (errorIdx in errorsMap.keys){
            val errorDisplayDetails = HashMap<String,String>()
            errorDisplayDetails["idx"] = errorIdx.toString()
            errorDisplayDetails["description"] = errorsMap[errorIdx]!!["name"]!!
            errorDisplayDetails["resource"] = errorsMap[errorIdx]!!["resource"]!!
            errorDisplayDetails["path"] = errorsMap[errorIdx]!!["className"]!!

            //Get Severity based on errorType
            val severity = preferenceData[errorsMap[errorIdx]!!["type"]!!]!!["severity"]!!.toString()

            if(!errorGroupedBySeverity.containsKey(severity)){
                errorGroupedBySeverity[severity] = Vector<HashMap<String,String>>()
            }
            errorGroupedBySeverity[severity]!!.add(errorDisplayDetails.clone() as HashMap<String,String>)
        }
        return errorGroupedBySeverity

    }

    /*
     *  Prepares HashMap for constructing FindingsTable by grouping errors by error resource
     */
    fun getErrorGroupedByResource(): HashMap<String,Vector<HashMap<String,String>>> {

        val errorGroupedByResource = HashMap<String,Vector<HashMap<String,String>>>()

        for (errorIdx in errorsMap.keys){
            val errorDisplayDetails = HashMap<String,String>()
            errorDisplayDetails["idx"] = errorIdx.toString()
            errorDisplayDetails["description"] = errorsMap[errorIdx]!!["name"]!!
            errorDisplayDetails["resource"] = errorsMap[errorIdx]!!["resource"]!!
            errorDisplayDetails["path"] = errorsMap[errorIdx]!!["className"]!!
            errorDisplayDetails["type"] = errorsMap[errorIdx]!!["type"]!!

            if(!errorGroupedByResource.containsKey(errorsMap[errorIdx]!!["resource"]!!)){
                errorGroupedByResource[errorsMap[errorIdx]!!["resource"]!!] = Vector<HashMap<String,String>>()
            }

            errorGroupedByResource[errorsMap[errorIdx]!!["resource"]!!]!!.add(errorDisplayDetails.clone() as HashMap<String,String>)

        }
        return errorGroupedByResource

    }

    /*
     *  Prepares HashMap for constructing FindingsTable without grouping errors
     *  It is achieved by putting all errors in one vector and storing it with key "all"
     */
    fun getErrorsUngrouped(): HashMap<String, Vector<HashMap<String, String>>> {
        val errorGroupedByResource = HashMap<String,Vector<HashMap<String,String>>>()

        for (errorIdx in errorsMap.keys){
            val errorDisplayDetails = HashMap<String,String>()
            errorDisplayDetails["idx"] = errorIdx.toString()
            errorDisplayDetails["description"] = errorsMap[errorIdx]!!["name"]!!
            errorDisplayDetails["resource"] = errorsMap[errorIdx]!!["resource"]!!
            errorDisplayDetails["path"] = errorsMap[errorIdx]!!["className"]!!
            errorDisplayDetails["type"] = errorsMap[errorIdx]!!["type"]!!

            if(!errorGroupedByResource.containsKey("all")){
                errorGroupedByResource["all"] = Vector<HashMap<String,String>>()
            }

            errorGroupedByResource["all"]!!.add(errorDisplayDetails.clone() as HashMap<String,String>)
        }
        return errorGroupedByResource
    }

    private fun createPreferenceData()  {

        val xmlFile = File(preferencesFilePath!!)
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

    /*
        Prepares LocationInformation
        Locks and Accesses Information
        Guards Information
     */
    fun parseFindingsFile(){

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
        val jsonFile = Paths.get(PathManager.getPluginsPath(),getPluginName(),"Analysis",project!!.name,"results","findings.json").toAbsolutePath().toString()
        File(jsonFile).writeText(jsonObj.toString(prettyPrinterIndentFactor))


        var findingsArray:JSONArray? = null

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

            if(finding.getJSONObject("info").getJSONObject("message").get("location").javaClass ==JSONArray().javaClass ){
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

            findingsLocationInfo[findingKey] = Vector<HashMap<String, String>>()

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

                            findingsLocationInfo[findingKey]!!.add(locationsForEach.clone() as HashMap<String, String>?)
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


                        findingsLocationInfo[findingKey]!!.add(locationsForEach.clone() as HashMap<String, String>?)
                    }
                }
            }

            findingsAccessAndLocksInfo[findingKey] = Vector<java.util.HashMap<String, String>>()
            guardsInfo[findingKey] = Vector<java.util.HashMap<String, String>>()

            val eachFindingLocksAndAccessesData = java.util.HashMap<String, String>()
            val eachFindingGuardData = java.util.HashMap<String, String>()

            //Guards Data Preparation
            if( finding.getJSONObject("info").toMap().containsKey("guards") && ( finding.getJSONObject("info").get("guards").javaClass == JSONObject().javaClass )){
                val guards = finding.getJSONObject("info").getJSONObject("guards")

                var guardsArray: JSONArray
                var guardsObject: JSONObject

                if(guards.has("guardRelative")){
                    if(guards.get("guardRelative").javaClass == JSONArray().javaClass){
                        guardsArray = guards.getJSONArray("guardRelative")
                        for(eachObjectIdx in 0 until guardsArray.length()){
                            val guardObject = guardsArray.getJSONObject(eachObjectIdx)
                            eachFindingGuardData["intrinsic"] = guardObject.get("intrinsic").toString()
                            eachFindingGuardData["key"] = guardObject.get("key").toString()
                            eachFindingGuardData["typeRef"] = guardObject.get("typeRef").toString()
                            eachFindingGuardData["targetPath"] = guardObject.get("targetPath").toString()

                            if(guardObject.get("guardPath").javaClass ==JSONObject().javaClass){
                                eachFindingGuardData["guardPath.locationRef.key"] = guardObject.getJSONObject("guardPath").getJSONObject("locationRef").get("key").toString()
                            }else{
                                eachFindingGuardData["guardPath.locationRef.key"] = ""
                            }

                            if(eachFindingGuardData["intrinsic"]=="false"){
                                for(locationObject in findingsLocationInfo[findingKey]!!){
                                    if(locationObject["key"]==eachFindingGuardData["guardPath.locationRef.key"]){
                                        eachFindingGuardData["name"] = locationObject["name"].toString()
                                    }
                                }
                            }else{
                                eachFindingGuardData["name"] = ""
                            }
                            guardsInfo[findingKey]!!.add(eachFindingGuardData.clone() as HashMap<String, String>?)
                        }
                    }else if(guards.get("guardRelative").javaClass == JSONObject().javaClass){
                        guardsObject = guards.getJSONObject("guardRelative")
                        eachFindingGuardData["intrinsic"] = guardsObject.get("intrinsic").toString()
                        eachFindingGuardData["key"] = guardsObject.get("key").toString()
                        eachFindingGuardData["typeRef"] = guardsObject.get("typeRef").toString()
                        eachFindingGuardData["targetPath"] = guardsObject.get("targetPath").toString()
                        if(guardsObject.get("guardPath").javaClass ==JSONObject().javaClass){
                            eachFindingGuardData["guardPath.locationRef.key"] = guardsObject.getJSONObject("guardPath").getJSONObject("locationRef").get("key").toString()
                        }else{
                            eachFindingGuardData["guardPath.locationRef.key"] = ""
                        }


                        if(eachFindingGuardData["intrinsic"]=="false"){
                            for(locationObject in findingsLocationInfo[findingKey]!!){
                                if(locationObject["key"]==eachFindingGuardData["guardPath.locationRef.key"]){
                                    eachFindingGuardData["name"] = locationObject["name"].toString()
                                }
                            }
                        }else{
                            eachFindingGuardData["name"] = ""
                        }

                        guardsInfo[findingKey]!!.add(eachFindingGuardData.clone() as HashMap<String, String>?)
                    }
                }else if(guards.has("guardAbsolute")){
                    if(guards.get("guardAbsolute").javaClass == JSONArray().javaClass){
                        guardsArray = guards.getJSONArray("guardAbsolute")
                        for(eachObjectIdx in 0 until guardsArray.length()){
                            val guardObject = guardsArray.getJSONObject(eachObjectIdx)
                            eachFindingGuardData["intrinsic"] = guardObject.get("intrinsic").toString()
                            eachFindingGuardData["key"] = guardObject.get("key").toString()
                            eachFindingGuardData["typeRef"] = ""
                            eachFindingGuardData["targetPath"] = ""

                            if(guardObject.get("guardPath").javaClass ==JSONObject().javaClass){
                                eachFindingGuardData["guardPath.locationRef.key"] = guardObject.getJSONObject("guardPath").getJSONObject("locationRef").get("key").toString()
                            }else{
                                eachFindingGuardData["guardPath.locationRef.key"] = ""
                            }

                            if(eachFindingGuardData["intrinsic"]=="false"){
                                for(locationObject in findingsLocationInfo[findingKey]!!){
                                    if(locationObject["key"]==eachFindingGuardData["guardPath.locationRef.key"]){
                                        eachFindingGuardData["name"] = locationObject["name"].toString()
                                    }
                                }
                            }else{
                                eachFindingGuardData["name"] = ""
                            }
                            guardsInfo[findingKey]!!.add(eachFindingGuardData.clone() as HashMap<String, String>?)
                        }
                    }else if(guards.get("guardAbsolute").javaClass == JSONObject().javaClass){
                        guardsObject = guards.getJSONObject("guardAbsolute")
                        eachFindingGuardData["intrinsic"] = guardsObject.get("intrinsic").toString()
                        eachFindingGuardData["key"] = guardsObject.get("key").toString()
                        eachFindingGuardData["typeRef"] = ""
                        eachFindingGuardData["targetPath"] = ""

                        if(guardsObject.get("guardPath").javaClass ==JSONObject().javaClass){
                            eachFindingGuardData["guardPath.locationRef.key"] = guardsObject.getJSONObject("guardPath").getJSONObject("locationRef").get("key").toString()
                        }else{
                            eachFindingGuardData["guardPath.locationRef.key"] = ""
                        }


                        if(eachFindingGuardData["intrinsic"]=="false"){
                            for(locationObject in findingsLocationInfo[findingKey]!!){
                                if(locationObject["key"]==eachFindingGuardData["guardPath.locationRef.key"]){
                                    eachFindingGuardData["name"] = locationObject["name"].toString()
                                }
                            }
                        }else{
                            eachFindingGuardData["name"] = ""
                        }

                        guardsInfo[findingKey]!!.add(eachFindingGuardData.clone() as HashMap<String, String>?)


                    }

                }




            }


            //Accesses and Locks Info Preparation
            if(finding.getJSONObject("info").toMap().containsKey("accesses") and finding.getJSONObject("info").toMap().containsKey("guards")){

                //As it is present, Populate the required Data
                val eachFindingLocations = findingsLocationInfo[findingKey]
                val accesses = finding.getJSONObject("info").getJSONObject("accesses")

                var accessArray: JSONArray
                var accessObject: JSONObject

                if(accesses.get("access").javaClass == JSONArray().javaClass){
                    accessArray = accesses.getJSONArray("access")

                    for(eachObjectIdx in 0 until accessArray.length()){
                        val accessObject = accessArray.getJSONObject(eachObjectIdx)
                        val location = accessObject.get("location")
                        var status = ""
                        var guardRefKey = ""
                        if(accessObject.toMap().containsKey("accessGuards") && accessObject.get("accessGuards").javaClass == JSONObject().javaClass){

                            status =  accessObject.getJSONObject("accessGuards").getJSONObject("guardRef").get("status").toString()
                            guardRefKey = accessObject.getJSONObject("accessGuards").getJSONObject("guardRef").get("key").toString()
                        }

                        for(eachFindingLocationIdx in 0 until eachFindingLocations!!.size){
                            if(eachFindingLocations.elementAt(eachFindingLocationIdx)["key"].equals(location.toString()) ){
                                eachFindingLocksAndAccessesData["line"] = eachFindingLocations.elementAt(eachFindingLocationIdx)["line"].toString()
                                eachFindingLocksAndAccessesData["method"] = eachFindingLocations.elementAt(eachFindingLocationIdx)["method"].toString()
                                eachFindingLocksAndAccessesData["type"] = accessObject.get("type").toString()
                                eachFindingLocksAndAccessesData["desc"] = eachFindingLocations.elementAt(eachFindingLocationIdx)["desc"].toString()
                                eachFindingLocksAndAccessesData["className"] = eachFindingLocations.elementAt(eachFindingLocationIdx)["className"].toString()
                                eachFindingLocksAndAccessesData["status"] = status
                                eachFindingLocksAndAccessesData["location"] = location.toString()
                                eachFindingLocksAndAccessesData["guardRefKey"] = guardRefKey
                                break
                            }
                        }

                        //Add to main HashMap
                        if(eachFindingLocksAndAccessesData.keys.size!=0){
                            findingsAccessAndLocksInfo[findingKey]!!.add(eachFindingLocksAndAccessesData.clone() as HashMap<String, String>?)
                        }
                    }

                }else{

                    accessObject = accesses.getJSONObject("access")

                    var status = ""
                    var guardRefKey = ""
                    if(accessObject.toMap().containsKey("accessGuards") && accessObject.get("accessGuards").javaClass == JSONObject().javaClass){
                        status =  accessObject.getJSONObject("accessGuards").getJSONObject("guardRef").get("status").toString()
                        guardRefKey = accessObject.getJSONObject("accessGuards").getJSONObject("guardRef").get("key").toString()
                    }

                    val location = accessObject.get("location")

                    for(eachFindingLocationIdx in 0 until eachFindingLocations!!.size){
                        if(eachFindingLocations.elementAt(eachFindingLocationIdx)["key"].equals(location.toString()) ){
                            eachFindingLocksAndAccessesData["line"] = eachFindingLocations.elementAt(eachFindingLocationIdx)["line"].toString()
                            eachFindingLocksAndAccessesData["method"] = eachFindingLocations.elementAt(eachFindingLocationIdx)["method"].toString()
                            eachFindingLocksAndAccessesData["type"] = accessObject.get("type").toString()
                            eachFindingLocksAndAccessesData["desc"] = eachFindingLocations.elementAt(eachFindingLocationIdx)["desc"].toString()
                            eachFindingLocksAndAccessesData["className"] = eachFindingLocations.elementAt(eachFindingLocationIdx)["className"].toString()
                            eachFindingLocksAndAccessesData["status"] = status
                            eachFindingLocksAndAccessesData["location"] = location.toString()
                            eachFindingLocksAndAccessesData["guardRefKey"] = guardRefKey
                            break
                        }
                    }

                    //Add to main HashMap
                    if(eachFindingLocksAndAccessesData.keys.size!=0){
                        findingsAccessAndLocksInfo[findingKey]!!.add(eachFindingLocksAndAccessesData.clone() as HashMap<String, String>?)
                    }
                }
            }
        }


    }

    fun getPreferencesData(): HashMap<String, HashMap<String, String>> {
        return preferenceData
    }

    fun updatePreferencesData(){
        createPreferenceData()
    }

    fun getFindingsLocationInfo(): HashMap<String, Vector<HashMap<String, String>>> {
        return findingsLocationInfo
    }

    fun getFindingsAccessAndLocksInfo(): HashMap<String, Vector<HashMap<String, String>>> {
        return findingsAccessAndLocksInfo
    }

    fun getGuardsInfo(): HashMap<String, Vector<HashMap<String, String>>> {
        return guardsInfo
    }
}