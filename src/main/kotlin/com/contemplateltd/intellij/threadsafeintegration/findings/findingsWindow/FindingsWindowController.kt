package com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow

import com.contemplateltd.intellij.threadsafeintegration.actions.runAnalysisAction.RunAnalysisUtil
import com.contemplateltd.intellij.threadsafeintegration.actions.runAnalysisAction.UI.EnableRulesPrompt
import com.contemplateltd.intellij.threadsafeintegration.extensions.LineMarker.LineMarkerProviderUtil
import com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.PreferencesPageUtil
import com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow.UI.DialogPromptClass
import com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow.UI.FindingsWindowUI
import com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow.UI.Node
import com.contemplateltd.intellij.threadsafeintegration.icons.PluginIcons
import com.contemplateltd.intellij.threadsafeintegration.util.HelpPageProvider
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.find.impl.FindManagerImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import java.awt.event.ActionListener
import java.io.File
import java.util.*
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import javax.swing.event.TreeSelectionListener
import kotlin.collections.HashMap

/**
 * Controller class to display analysis results
 * Triggered from RunAnalysisAction
 */

class FindingsWindowController(project: Project):DumbAware {

    private var project:Project?
    private var findingsWindowUtil:FindingsWindowUtil?
    private var findingsWindowUI: FindingsWindowUI? = null
    private var toolWindowManager: ToolWindowManager? = null
    private val threadSafeContentName = "ThreadSafe Findings"
    private val threadSafeFindingsTaskName = "ThreadSafe"

    init{
        this.project = project
        findingsWindowUtil = FindingsWindowUtil(this.project!!)
        findingsWindowUI = FindingsWindowUI()
    }

    /*
        Triggered from RunAnalysis action
        Constructs FindingsWindow and adds to ToolsWindow
     */
    fun displayAnalysisResults(){
        //Parsing findings XML file
        findingsWindowUtil!!.readAnalysisResults()
        findingsWindowUtil!!.parseFindingsFile()
        //If error while parsing
        if(!findingsWindowUtil!!.findingsFileParsed()){
            val frame = WindowManager.getInstance().getFrame(project)
            val promptTitle = "ThreadSafe Analysis Result"
            val promptMessage = "Some error occurred during ThreadSafe Analysis"
            DialogPromptClass.show(project!!,frame,promptTitle,promptMessage)
            return
        }

        //If no errors, return
        if(findingsWindowUtil!!.getErrorsMap().size==0){
            val frame = WindowManager.getInstance().getFrame(project)
            val promptTitle = "ThreadSafe Analysis Result"
            val promptMessage = "No errors found during ThreadSafe Analysis"
            DialogPromptClass.show(project!!,frame,promptTitle,promptMessage)
            return
        }

        //Get Error Details and Rule Preferences
        val errorDetailsMap = findingsWindowUtil!!.getErrorGroupedByType()
        val preferencesData = findingsWindowUtil!!.getPreferencesData()

        //Construct FindingsWindowPanel
        findingsWindowUI!!.constructFindingsWindow(errorDetailsMap,preferencesData)

        //Add Button Listeners
        addButtonListeners()

        //Add Findings Table Listener
        findingsWindowUI!!.getFindingsTableUI().getFindingsTable()!!.addTreeSelectionListener(findingsTableListener())

        //Add ErrorDetailsTextArea Hyperlink Listener
        findingsWindowUI!!.getErrorDetailsAreaUI().getErrorDetailsTextPane().addHyperlinkListener(errorDetailsTextAreaHyperlinkListener())

        //Initiating toolWindowManager
        toolWindowManager = ToolWindowManager.getInstance(project!!)

        //Registering toolWindowTask and adding content
        ApplicationManager.getApplication().invokeLater {
            //Checking if toolWindow already exists
            if(toolWindowManager!!.getToolWindow(threadSafeFindingsTaskName)==null){

                //Creating ToolWindow task
                val threadSafeAnalysisTask =  RegisterToolWindowTask(threadSafeFindingsTaskName,
                    ToolWindowAnchor.BOTTOM,
                    sideTool = false,
                    canCloseContent = true,
                    canWorkInDumbMode = true,
                    shouldBeAvailable = true,
                    icon = com.contemplateltd.intellij.threadsafeintegration.icons.PluginIcons.ContemplateIcon)

                //Registering Task
                toolWindowManager!!.registerToolWindow(threadSafeAnalysisTask)

                //Adding Content
                val threadSafeFindingsToolWindow = toolWindowManager!!.getToolWindow(threadSafeFindingsTaskName)
                val findingsContent = threadSafeFindingsToolWindow!!.contentManager.factory.createContent(findingsWindowUI!!.getFindingsWindow(),threadSafeContentName,false)
                threadSafeFindingsToolWindow.contentManager.addContent(findingsContent)
                threadSafeFindingsToolWindow.show()
                LineMarkerProviderUtil.setClearMarkersEnabled(false)


            }else{
                val threadSafeFindingsToolWindow = toolWindowManager!!.getToolWindow(threadSafeFindingsTaskName)
                threadSafeFindingsToolWindow!!.contentManager.removeAllContents(true)
                val findingsContent = threadSafeFindingsToolWindow.contentManager.factory.createContent(findingsWindowUI!!.getFindingsWindow(),threadSafeContentName,false)
                threadSafeFindingsToolWindow.contentManager.addContent(findingsContent)
                threadSafeFindingsToolWindow.show()
                LineMarkerProviderUtil.setClearMarkersEnabled(false)
            }
        }
    }

    /*
        Adds ButtonsWindow Button Listeners
     */
    private fun addButtonListeners() {
        findingsWindowUI!!.getFindingsWindowButtonsPanel().getGroupFindingsButton()!!.addActionListener(groupFindingsButtonListener())
        findingsWindowUI!!.getFindingsWindowButtonsPanel().getExpandAllButton().addActionListener(expandAllButtonListener())
        findingsWindowUI!!.getFindingsWindowButtonsPanel().getCollapseAllButton().addActionListener(collapseAllButtonListener())
        findingsWindowUI!!.getFindingsWindowButtonsPanel().getRunAnalysisButton().addActionListener(runAnalysisButtonListener())
        findingsWindowUI!!.getFindingsWindowButtonsPanel().getClearMarkersButton().addActionListener(clearMarkersButtonListener())
    }

    private fun clearMarkersButtonListener(): ActionListener {
        return ActionListener {
            val threadSafeFindingsTaskName = "ThreadSafe"
            val threadSafeAccessesAndLocksTaskName = "ThreadSafe:AccessesAndLocks"
            val toolWindowManager = ToolWindowManager.getInstance(project!!)
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


    private fun expandAllButtonListener(): ActionListener {
        return ActionListener {
            findingsWindowUI!!.getFindingsTableUI().getFindingsTable()!!.expandAll()
        }
    }

    private fun collapseAllButtonListener(): ActionListener {
        return ActionListener {
            findingsWindowUI!!.getFindingsTableUI().getFindingsTable()!!.collapseAll()
        }
    }


    //Changes FindingsTable in ToolsWindow based on selected grouping parameter
    private fun changeFindingsTable(groupingParam:String){

        var errorDetailsMap: HashMap<String, Vector<HashMap<String, String>>>? = null
        val preferencesData = findingsWindowUtil!!.getPreferencesData()

        when(groupingParam){
            "Severity" -> errorDetailsMap = findingsWindowUtil!!.getErrorGroupedBySeverity()
            "Type" -> errorDetailsMap = findingsWindowUtil!!.getErrorGroupedByType()
            "Resource" -> errorDetailsMap = findingsWindowUtil!!.getErrorGroupedByResource()
            "No Grouping" -> errorDetailsMap = findingsWindowUtil!!.getErrorsUngrouped()
        }

        //Change Table
        if (errorDetailsMap != null) {
            findingsWindowUI!!.getFindingsTableUI().createFindingsTable(errorDetailsMap,preferencesData,groupingParam)
        }
    }

    private fun groupFindingsButtonListener(): ActionListener {
        return ActionListener {
            if(it.actionCommand == "comboBoxChanged"){
                //Remove Table Selection Listener
                findingsWindowUI!!.getFindingsTableUI().getFindingsTable()!!.clearSelection()

                //Based on selectedGroupingParam value, change the FindingsTable
                val selectedGroupingParam = findingsWindowUI!!.getFindingsWindowButtonsPanel().getGroupFindingsButton()!!.selectedItem!!.toString()
                changeFindingsTable(selectedGroupingParam)

            }
        }
    }

    private fun runAnalysisButtonListener(): ActionListener {
        return ActionListener {

            //PreferencesData
            val preferencesPageUtil = PreferencesPageUtil()
            var enabledRulesLength = 0
            for(rule in   preferencesPageUtil.createPreferenceData("current").values){
                if(rule["enabled"]=="true"){
                    enabledRulesLength+=1
                    break
                }
            }



            if(enabledRulesLength==0){
                //Check if a rule is enabled
                println("Please enable at least one rule in ThreadSafe Preferences Page.")
                val frame = WindowManager.getInstance().getFrame(project)
                EnableRulesPrompt.enableRulePrompt(project!!,frame)
            }
            else {

                //Update Preferences Data
                findingsWindowUtil!!.updatePreferencesData()


                //Get RuleProvider
                val runAnalysisUtil = RunAnalysisUtil(project!!)
                val ruleProvider = runAnalysisUtil.getRuleProvider()
                val outPutDirectoryPath = runAnalysisUtil.getAnalysisResultsDirectoryPath()

                //Checks if valid RuleProvider is present
                if (ruleProvider == null) {
                    println("Rule Provider is empty")
                } else {

                    //Path to directories containing .class files
                    val classPaths: Vector<String> = Vector<String>()

                    for (module in ModuleManager.getInstance(project!!).modules) {
                        if (CompilerModuleExtension.getInstance(module)!!.compilerOutputPath != null) {
                            classPaths.add(CompilerModuleExtension.getInstance(module)!!.compilerOutputPath!!.canonicalPath)
                        }

                    }

                    //LibPaths -> TODO
                    val libPaths: Vector<String> = Vector<String>()

                    //List of Canonical names of specified .java files to analyze - Always be empty
                    val classesToAnalyze: Vector<String> = Vector<String>()

                    //Path to source directory of project
                    val sourceDirs: Vector<File> = Vector<File>()
                    for (vf in ProjectRootManagerImpl.getInstance(project!!).contentSourceRoots) {
                        sourceDirs.add(File(vf.canonicalPath.toString()))
                    }


                    //CallBack function to trigger ThreadSafe Analysis if project compiled successfully
                    val compileCallback = CompileStatusNotification { aborted, errors, _, _ ->
                        if (!(aborted || errors > 0)) {
                            //Triggering Analysis and Tracking Progress
                            ProgressManager.getInstance().run(object :
                                Task.Backgroundable(project, "ThreadSafe analysis", true) {

                                //Create Analysis Controller and trigger analysis
                                override fun run(indicator: ProgressIndicator) {
                                    val triggerAnalysisController = TriggerAnalysisController(
                                        project.name, ruleProvider, outPutDirectoryPath,
                                        classPaths, libPaths, classesToAnalyze, sourceDirs, indicator
                                    )
                                    triggerAnalysisController.runAnalyses()
                                }

                                override fun onFinished() {
                                    super.onFinished()

                                    //Change Findings Table
                                    //Parsing findings XML file
                                    findingsWindowUtil!!.readAnalysisResults()
                                    findingsWindowUtil!!.parseFindingsFile()

                                    //If error while parsing
                                    if (!findingsWindowUtil!!.findingsFileParsed()) {
                                        val frame = WindowManager.getInstance().getFrame(project)
                                        val promptTitle = "ThreadSafe Analysis Result"
                                        val promptMessage = "Some error occurred during ThreadSafe Analysis"
                                        DialogPromptClass.show(project!!,frame,promptTitle,promptMessage)
                                        return
                                    }

                                    //If no errors, return
                                    if (findingsWindowUtil!!.getErrorsMap().size == 0) {
                                        val frame = WindowManager.getInstance().getFrame(project)
                                        val promptTitle = "ThreadSafe Analysis Result"
                                        val promptMessage = "No errors found during ThreadSafe Analysis"
                                        DialogPromptClass.show(project!!,frame,promptTitle,promptMessage)
                                        return
                                    }

                                    //Update Preferences Data
                                    findingsWindowUtil!!.updatePreferencesData()

                                    val selectedGroupingParam = findingsWindowUI!!.getFindingsWindowButtonsPanel()
                                        .getGroupFindingsButton()!!.selectedItem!!.toString()
                                    changeFindingsTable(selectedGroupingParam)
                                    LineMarkerProviderUtil.setClearMarkersEnabled(false)
                                    DaemonCodeAnalyzer.getInstance(project).restart()

                                }
                            })

                        } else {
                            println("Error in project Compilation")
                        }
                    }

                    //Compile Project and Run ThreadSafe Analysis
                    for (module in ModuleManager.getInstance(project!!).modules) {
                        CompilerManager.getInstance(project!!).compile(module, compileCallback)
                    }
                }
            }
        }
    }



    private fun findingsTableListener(): TreeSelectionListener {
        return TreeSelectionListener {
            if(findingsWindowUI!!.getFindingsTableUI().getFindingsTable()!!.selectedRows.isNotEmpty()){
                val path =  findingsWindowUI!!.getFindingsTableUI().getFindingsTable()!!.treeSelectionModel.selectionPath
                val selectedNode = path.lastPathComponent as Node

                //Check Node Type
                if(selectedNode.getNodeType() == Node.ROOTNODETYPE){

                    val selectionStatistics = prepareSelectionStatisticsData(selectedNode.getGroupKey()!!)
                    val selectionStatisticsAreaText = prepareSelectionStatisticsAreaText(selectionStatistics)
                    println(selectionStatistics)
                    println("Show selection statistics")
                    findingsWindowUI!!.getErrorDetailsAreaUI().getErrorDetailsTextPane().text = selectionStatisticsAreaText
                }else if (selectedNode.getNodeType()==Node.CHILDNODETYPE){
                    val individualErrorDetailsText =   prepareIndividualErrorDetailsText(selectedNode.getErrorIdx())
                    findingsWindowUI!!.getErrorDetailsAreaUI().getErrorDetailsTextPane().text = individualErrorDetailsText
                }
            }
        }
    }

    private fun errorDetailsTextAreaHyperlinkListener(): HyperlinkListener {
        return HyperlinkListener { e->
            if (HyperlinkEvent.EventType.ACTIVATED == e.eventType) {
                val hyperLink = e.description.toString()
                //Open Rule Description
                if(hyperLink.startsWith("displayRule:")){
                    val ruleName = hyperLink.split(":")[1].trim()
                    HelpPageProvider().openHtmlEditor(ruleName)
                }
                //Open Call Hierarchy
                else if(hyperLink.startsWith("callHierarchy:")){
                    callHierarchyProvider(hyperLink)
                }else if(hyperLink.startsWith("openLocation:")){
                    openErrorLocation(hyperLink)
                }else if(hyperLink.startsWith("accessesAndLocks:")){
                    showAccessAndLocksWindow(hyperLink)
                }

            }
        }

    }

    private fun showAccessAndLocksWindow(hyperLink: String) {

        val errorIdx = hyperLink.split(":")[1].toInt()

        //Prepare AccessesAndLocks Tables
        val accessesAndLocksController = AccessesAndLocksController(errorIdx,
            findingsWindowUtil!!.getErrorsMap(),
            findingsWindowUtil!!.getFindingsLocationInfo(),
            findingsWindowUtil!!.getFindingsAccessAndLocksInfo(),
            findingsWindowUtil!!.getGuardsInfo())

        accessesAndLocksController.prepareTables()

        //Create Accesses and Locks Table Panel
        val accessAndLocksPanel = accessesAndLocksController.createAccessesAndLocksSection()


        //Remove Existing ThreadSafe Locks and Accesses Task if exists or register new if non exists
        if(toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks") != null){
            toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.contentManager.removeAllContents(true)
        }else{
            val task = RegisterToolWindowTask("ThreadSafe:AccessesAndLocks", ToolWindowAnchor.RIGHT,icon = PluginIcons.ContemplateIcon)
            toolWindowManager?.registerToolWindow(task)

        }

        toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.title = "Accesses and Locks"

//                    Remove Existing Accesses and Locks Content
        if(toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.contentManager.findContent("AccessesAndLocks") != null){
            val existingContent = toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.contentManager.findContent("AccessesAndLocks")
            toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.contentManager.removeContent(existingContent,true)
        }
        //Adding Content to toolWindow
        val accessesAndLocksContent = toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.contentManager.factory.createContent(accessAndLocksPanel,"AccessesAndLocks",false)
        toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.contentManager.addContent(accessesAndLocksContent)

        //Focus on AccessesAndLocks
        val switchContent = toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.contentManager.findContent("AccessesAndLocks")
        toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.contentManager.setSelectedContent(switchContent)
        toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.contentManager.findContent("AccessesAndLocks")

        //Adding Buttons
        toolWindowManager!!.getToolWindow("ThreadSafe:AccessesAndLocks")!!.show()
    }

    private fun openErrorLocation(hyperLink: String) {

        val parameters = hyperLink.split(":")
        var line = parameters[1].toInt()
//                    val className = parameters[1]
        val fileName = parameters[3]
        var objectName = ""
        if(parameters.size>4){
            objectName = parameters[4]
        }

        val requiredGlobalScope = GlobalSearchScope.projectScope(project!!)
        val foundFileItems  = FilenameIndex.getVirtualFilesByName(fileName,requiredGlobalScope)
        val virtualFile = foundFileItems.toList()[0]
        val psiFile = PsiManager.getInstance(project!!).findFile(virtualFile!!)
        //Get PSI File to get proper offset
        val startOffset = PsiDocumentManager.getInstance(project!!).getDocument(psiFile!!)!!.getLineStartOffset(line)
        val endOffset = PsiDocumentManager.getInstance(project!!).getDocument(psiFile)!!.getLineEndOffset(line)
        var offset = 0
        val substring = psiFile.viewProvider.document!!.text.slice(IntRange(startOffset, endOffset))


        if(objectName!=""){
            for(item in substring.split(" ")){
                if( item.trim().contains(objectName) ){
                    offset = psiFile.viewProvider.document!!.text.slice(IntRange(startOffset, endOffset)).indexOf(objectName)
                    break
                }
            }
        }

        offset += startOffset
        line = PsiDocumentManager.getInstance(project!!).getDocument(psiFile)!!.getLineNumber(offset)
        //Try
        val temp = psiFile.findElementAt(offset)
        println(temp!!.javaClass.toString())
        println(PsiWhiteSpaceImpl::class.java)
        if(temp.javaClass.toString() == PsiWhiteSpaceImpl::class.java.toString()){
            OpenFileDescriptor(project!!, virtualFile, line - 1 ,0 ).navigate(true)
        }else{
            OpenFileDescriptor(project!!,virtualFile,offset).navigate(true)
        }
    }


    private fun callHierarchyProvider(hyperLink:String){

        try{
            val errorIdx = hyperLink.split(":")[1].trim().toInt()
            val errorDetails = findingsWindowUtil!!.getErrorsMap()[errorIdx]
            val errorName = errorDetails!!["name"].toString().trim()
            var fileName:String? = null
            var objectName:String? = null
            var lineForUsage:String? = null
            var problemLocationFound = false


            for ( eachFinding in findingsWindowUtil!!.getFindingsLocationInfo()[errorName]!!){
                if(eachFinding["locationMessage"].toString().trim().lowercase().startsWith("problem location")){
                    fileName = eachFinding["filename"]
                    objectName = eachFinding["name"]
                    lineForUsage = eachFinding["line"]
                    problemLocationFound = true
                    break
                }
            }

            //If no Problem Location
            if(!problemLocationFound && findingsWindowUtil!!.getFindingsLocationInfo()[errorName]!!.size>0 ){
                val requiredFinding = findingsWindowUtil!!.getFindingsLocationInfo()[errorName]!![0]
                fileName = requiredFinding["filename"]
                objectName = requiredFinding["name"]
                lineForUsage = requiredFinding["line"]
            }

            val lineNumber = lineForUsage!!.toInt() -1
            val requiredGlobalScope = GlobalSearchScope.projectScope(project!!)
            val foundFileItems  = FilenameIndex.getVirtualFilesByName(fileName!!,requiredGlobalScope)
            val virtualFile = foundFileItems.toList()[0]
            val psiFile = PsiManager.getInstance(project!!).findFile(virtualFile!!)

            //Get PsiFile
            val startOffset = PsiDocumentManager.getInstance(project!!).getDocument(psiFile!!)!!.getLineStartOffset(lineNumber)
            val endOffset = PsiDocumentManager.getInstance(project!!).getDocument(psiFile)!!.getLineEndOffset(lineNumber)

            var offset = startOffset

            if(objectName!=null){
                offset+= psiFile.viewProvider.document!!.text.slice(IntRange(startOffset,endOffset)).indexOf(objectName)
            }

            val globalSearchScope =  GlobalSearchScope.allScope(project!!)

            //Close FindUsage Dialog if open
            closeFindUsageWindow()

            val toolWindowManager = ToolWindowManager.getInstance(project!!)

            for(element in psiFile.children){
                if(element.elementType.toString() == "CLASS"){
                    for(classElement in element.children){
                        if(classElement.elementType.toString() == "FIELD" || classElement.elementType.toString() == "METHOD"){
                            if(offset>=classElement.startOffset && offset<=classElement.endOffset){
                                FindManagerImpl.getInstance(project!!).findUsagesInScope(classElement!!,globalSearchScope)
                                val activeToolWindowId = ToolWindowManager.getInstance(project!!).activeToolWindowId
                                if(activeToolWindowId!=null){
                                    toolWindowManager.getToolWindow(activeToolWindowId)!!.hide()
                                }

                                ToolWindowManager.getInstance(project!!).getToolWindow("Find")!!.show()
                                //If No Usages Found
                                if(!ToolWindowManager.getInstance(project!!).getToolWindow("Find")!!.isVisible || ToolWindowManager.getInstance(project!!).getToolWindow("Find")!!.contentManager.isEmpty ){
                                    toolWindowManager.getToolWindow(threadSafeFindingsTaskName)!!.show()
                                }
                                break
                            }
                        }
                    }
                }
            }

        } catch (exception:Exception){
            println(exception.stackTrace)
        }
    }

    private fun closeFindUsageWindow() {

        val findToolWindow = ToolWindowManager.getInstance(project!!).getToolWindow("Find")
        if(findToolWindow !=null){
            ToolWindowManager.getInstance(project!!).getToolWindow("Find")!!.contentManager.removeAllContents(true)
            ToolWindowManager.getInstance(project!!).getToolWindow("Find")!!.hide()

        }
    }


    private fun prepareIndividualErrorDetailsText(errorIdx: Int?): String {


        var individualErrorText = ""

        val errorType = findingsWindowUtil!!.getErrorsMap()[errorIdx]!!["type"]
        if( errorType!=null && findingsWindowUtil!!.getPreferencesData().containsKey(errorType)){

            val errorName = findingsWindowUtil!!.getPreferencesData()[errorType]!!["name"]

            //Title
            individualErrorText+="<p>"
            individualErrorText+= "<h2>$errorName</h2>"
            //Error Description
            val errorDescription = findingsWindowUtil!!.getErrorsMap()[errorIdx]!!["name"]
            individualErrorText+="$errorDescription"
            individualErrorText+="</p>"

            //Error Resource and locations
            individualErrorText+="<p>"
            //resource
            val errorResource = findingsWindowUtil!!.getErrorsMap()[errorIdx]!!["resource"]
            individualErrorText+="$errorResource"

            //locations
            var objectName:String? = null

            individualErrorText+="<ul style=\"list-style-type:none;color:#BBBBBB;padding-left:4px;\">"
            for ( eachFinding in findingsWindowUtil!!.getFindingsLocationInfo()[errorDescription]!!){
                if(eachFinding["locationMessage"].toString().trim().lowercase().startsWith("problem location")){
                    objectName = eachFinding["name"]
                }

                if(eachFinding["locationMessage"]!="null"){
                    individualErrorText+="<li><a style=\"color:#287BDE\" href=\"openLocation:"
                    individualErrorText+=eachFinding["line"]
                    individualErrorText+=":"
                    individualErrorText+=eachFinding["className"]
                    individualErrorText+=":"
                    individualErrorText+=eachFinding["filename"]
                    if(objectName !=null){
                        individualErrorText+=":"
                        individualErrorText+=eachFinding["name"]
                    }
                    individualErrorText+="\">"
                    individualErrorText+=eachFinding["line"]
                    individualErrorText+="</a>"
                    individualErrorText+="   "
                    individualErrorText+=eachFinding["locationMessage"]
                    individualErrorText+="</li>"
                }

            }
            individualErrorText+="</ul>"
            individualErrorText+="</p>"

            //Locks and Accesses, Call Hierarchy and Rule Description
            individualErrorText+="<p>"
            //Locks and Accesses
            if(checkIfLockAndAccesses(findingsWindowUtil!!.getPreferencesData()[errorType]!!["name"].toString())){
                individualErrorText+="<a style=\"color:#287BDE\" href=\""
                individualErrorText+="accessesAndLocks:$errorIdx"
                individualErrorText+="\">"
                individualErrorText+="Accesses and locks" + "</a><br>"
            }

            //Rule Description
            val ruleName = findingsWindowUtil!!.getPreferencesData()[errorType]!!["name"].toString().trim()
            individualErrorText+="<a href=\"displayRule:$ruleName\" style=\"color:#287BDE\">Rule Description</a><br></p>"

            //Call Hierarchy
            individualErrorText+="<a href=\"callHierarchy:$errorIdx\" style=\"color:#287BDE\">Call Hierarchy</a><br>"

            //Severity, Category and Type
            individualErrorText+="<p>"
            //Severity
            val errorSeverity = findingsWindowUtil!!.getPreferencesData()[errorType]!!["severity"].toString().replaceFirstChar { firstChar ->firstChar.titlecase() }
            individualErrorText+="Severity: $errorSeverity"
            individualErrorText+="<br>"
            //Category
            val errorCategory = findingsWindowUtil!!.getPreferencesData()[errorType]!!["category"]
            individualErrorText+="Category: $errorCategory"
            individualErrorText+="<br>"
            //Type
            val errorTypeCode = findingsWindowUtil!!.getErrorsMap()[errorIdx]!!["type"]
            individualErrorText+="Type: $errorTypeCode"
            individualErrorText+="</p>"

        }

        return individualErrorText

    }

    private fun checkIfLockAndAccesses(errorType:String) : Boolean{

        val eligibleTypes = Vector<String>()

        eligibleTypes.add("Inconsistent synchronization of accesses to a field")
        eligibleTypes.add("Inconsistent synchronization of accesses to a collection")
        eligibleTypes.add("Mixed synchronization of accesses to a field")
        eligibleTypes.add("Mixed synchronization of accesses to a collection stored in a field")
        eligibleTypes.add("Threadsafe collection consistently guarded")
        eligibleTypes.add("Unsynchronized access to field from asynchronously invoked method")
        eligibleTypes.add("GuardedBy annotation violated")

        return eligibleTypes.contains(errorType.trim())
    }


    private fun prepareSelectionStatisticsAreaText(selectionStatistics: HashMap<String, Int>): String {

        var selectionStatisticsAreaText = ""
        selectionStatisticsAreaText += "<h2><b>Selection Statistics</b></h2>"
        selectionStatisticsAreaText += "<p style=\"font-size:12px\" >"
        //Blocker
        val blockerFindingsCount = selectionStatistics["blocker"]
        selectionStatisticsAreaText+="<b>Blocker</b> <span >&#x2192;</span> $blockerFindingsCount<br>"

        //Critical
        val criticalFindingsCount = selectionStatistics["critical"]
        selectionStatisticsAreaText+="<b>Critical</b> <span >&#x2192;</span> $criticalFindingsCount<br>"

        //major
        val majorFindingsCount = selectionStatistics["major"]
        selectionStatisticsAreaText+="<b>Major</b> <span >&#x2192;</span> $majorFindingsCount<br>"

        //minor
        val minorFindingsCount = selectionStatistics["minor"]
        selectionStatisticsAreaText+="<b>Minor</b> <span >&#x2192;</span> $minorFindingsCount<br>"

        //info
        val infoFindingsCount = selectionStatistics["info"]
        selectionStatisticsAreaText+="<b>Info</b> <span >&#x2192;</span> $infoFindingsCount<br>"

        //total
        val totalFindingsCount = selectionStatistics["total"]
        selectionStatisticsAreaText+="<b>Total</b> <span >&#x2192;</span> $totalFindingsCount<br>"

        //end
        selectionStatisticsAreaText+="</p>"

        return selectionStatisticsAreaText

    }

    //
    private fun prepareSelectionStatisticsData(groupKey: String): HashMap<String,Int> {

        val selectionStatistics = HashMap<String,Int>()

        val groupingParam = findingsWindowUI!!.getFindingsWindowButtonsPanel().getGroupFindingsButton()!!.selectedItem!!.toString()
        var errorDetailsMap: HashMap<String, Vector<HashMap<String, String>>>? = null
        val preferencesData = findingsWindowUtil!!.getPreferencesData()

        when(groupingParam){
            "Severity" -> errorDetailsMap = findingsWindowUtil!!.getErrorGroupedBySeverity()
            "Type" -> errorDetailsMap = findingsWindowUtil!!.getErrorGroupedByType()
            "Resource" -> errorDetailsMap = findingsWindowUtil!!.getErrorGroupedByResource()
            "No Grouping" -> errorDetailsMap = findingsWindowUtil!!.getErrorsUngrouped()
        }

        //Initialize Entries
        selectionStatistics["blocker"] = 0
        selectionStatistics["critical"] = 0
        selectionStatistics["major"] = 0
        selectionStatistics["minor"] = 0
        selectionStatistics["info"] = 0
        selectionStatistics["total"] = 0


        for(entry in errorDetailsMap!![groupKey]!!){
            val errorIdx = entry["idx"]!!.toInt()
            val errorType = findingsWindowUtil!!.getErrorsMap()[errorIdx]!!["type"].toString()
            when(preferencesData[errorType]!!["severity"].toString().lowercase()){
                "blocker" -> selectionStatistics["blocker"] = selectionStatistics["blocker"]!! + 1
                "critical" -> selectionStatistics["critical"] = selectionStatistics["critical"]!! + 1
                "major" -> selectionStatistics["major"] = selectionStatistics["major"]!! + 1
                "minor" -> selectionStatistics["minor"] = selectionStatistics["minor"]!! + 1
                "info" -> selectionStatistics["info"] = selectionStatistics["info"]!! + 1
            }
            selectionStatistics["total"] = selectionStatistics["total"]!! + 1
        }
        return selectionStatistics
    }

}