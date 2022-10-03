package com.contemplateltd.intellij.threadsafeintegration.extensions.LineMarker


import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.contemplateltd.intellij.threadsafeintegration.icons.PluginIcons
import java.util.*




class LineMarkerProvider : RelatedItemLineMarkerProvider() {


    private var lineMarkerProviderUtil:LineMarkerProviderUtil? = null

    init{
        lineMarkerProviderUtil = LineMarkerProviderUtil()
    }


    override fun collectNavigationMarkers(element: PsiElement,
                                          result: MutableCollection<in RelatedItemLineMarkerInfo<*>?>) {



        //Project
        val project: Project = element.project

        //If Files not present, return
        if(!lineMarkerProviderUtil!!.checkIfFilesPresent(project.name)){
            return
        }

        lineMarkerProviderUtil!!.setProject(project)
        lineMarkerProviderUtil!!.createLineMarkerInformation(project.name)

        //Get analysis Results
        val errorFindings =  lineMarkerProviderUtil!!.getErrorsMap()

        //If No analysis --> Return
        if (errorFindings.keys.size == 0) {
            return
        }

        val eachFileErrorLocation =  lineMarkerProviderUtil!!.getEachFileErrorLocationEachError().clone() as HashMap<String, Vector<HashMap<String, String>>>


        //Element and File
        val psiFile = element.containingFile
        val psiFileName = psiFile.name

        //Getting Required Problem Location Object
        val requiredArray = eachFileErrorLocation[psiFileName]


        val problemLocationArray:Vector<HashMap<String, String>> = Vector<HashMap<String, String>>()
        val secondaryLocationsArray:Vector<HashMap<String, String>> = Vector<HashMap<String, String>>()

        if (requiredArray != null) {
            for (finding in requiredArray) {
                if (finding["locationMessage"] == "Problem location") {
                    problemLocationArray.add(finding)
                }else if(finding["locationMessage"] != "null"){
                    secondaryLocationsArray.add(finding)
                }
            }
        }

        if(problemLocationArray.size>0){
            for(problemLocationObject in problemLocationArray ){
                if (problemLocationObject != null) {

                    val line = problemLocationObject["line"]!!.toInt()
                    var objectName:String? = null
                    if(problemLocationObject["name"]!=null){
                        objectName = problemLocationObject["name"]
                    }

                    val severity = problemLocationObject["severity"]!!
                    val toolTipText = problemLocationObject["toolTipText"]!!
                    val severityIcon = getSeverityIcon(severity)
                    val startOffset = PsiDocumentManager.getInstance(project).getDocument(psiFile!!)!!.getLineStartOffset(line)
                    val endOffset = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!.getLineEndOffset(line)
                    var offset = 0

                    val substring = psiFile.viewProvider.document!!.text.slice(IntRange(startOffset, endOffset))


                    if(objectName!=null){
                        for(item in substring.split(" ")){
                            if( item.trim().contains(objectName) ){
                                offset = psiFile.viewProvider.document!!.text.slice(IntRange(startOffset, endOffset)).indexOf(objectName)
                                break
                            }
                        }
                    }

                    offset += startOffset
                    for (elem in psiFile.children) {
                        if (elem.elementType.toString() == "CLASS") {
                            val requiredClassElement = elem as PsiClass
                            if (element == psiFile.findElementAt(offset)){
                                val gutterIconBuilder = NavigationGutterIconBuilder.create(severityIcon).setTargets(requiredClassElement).setTooltipText(toolTipText)
                                val gutterIconResult = gutterIconBuilder.createLineMarkerInfo(psiFile.findElementAt(offset)!!)

                                result.add(gutterIconResult)
                                return
                            }

                        }
                    }
                }
            }
        }

        //Adding Secondary Markers
        if(secondaryLocationsArray.size>0){
            for(secondaryLocationObject in secondaryLocationsArray ){
                if (secondaryLocationObject != null && !problemLocationArray.contains(secondaryLocationObject)) {

                    val line = secondaryLocationObject["line"]!!.toInt()
                    var objectName:String? = null
                    if(secondaryLocationObject["name"]!=null){
                        objectName = secondaryLocationObject["name"]
                    }
                    val toolTipText = secondaryLocationObject["toolTipText"]!!
                    val startOffset = PsiDocumentManager.getInstance(project).getDocument(psiFile!!)!!.getLineStartOffset(line)
                    val endOffset = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!.getLineEndOffset(line)
                    var offset = 0

                    val substring = psiFile.viewProvider.document!!.text.slice(IntRange(startOffset, endOffset))


                    if(objectName!=null){
                        for(item in substring.split(" ")){
                            if( item.trim().contains(objectName) ){
                                offset = psiFile.viewProvider.document!!.text.slice(IntRange(startOffset, endOffset)).indexOf(objectName)
                                break
                            }
                        }
                    }

                    offset += startOffset

                    val secondaryLocationIcon = AllIcons.Debugger.NextStatement

                    for (elem in psiFile.children) {
                        if (elem.elementType.toString() == "CLASS") {
                            val requiredClassElement = elem as PsiClass
                            if (element == psiFile.findElementAt(offset)) {
                                val gutterIconBuilder = NavigationGutterIconBuilder.create(secondaryLocationIcon)
                                    .setTargets(requiredClassElement).setTooltipText(toolTipText)
                                result.add(gutterIconBuilder.createLineMarkerInfo(psiFile.findElementAt(offset)!!))
                                return
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getSeverityIcon(severity: String): javax.swing.Icon {

        var icon = PluginIcons.infoSeverity

        when(severity){
            "blocker" -> icon = PluginIcons.blockerSeverity
            "critical" -> icon = PluginIcons.criticalSeverity
            "major" -> icon = PluginIcons.majorSeverity
            "minor" -> icon = PluginIcons.minorSeverity
            "info" -> icon = PluginIcons.infoSeverity
        }
        return icon
    }

}
