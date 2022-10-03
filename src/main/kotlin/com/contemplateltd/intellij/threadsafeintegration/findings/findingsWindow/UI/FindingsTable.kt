package com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow.UI

import com.intellij.icons.AllIcons
import org.jdesktop.swingx.JXTreeTable
import org.jdesktop.swingx.treetable.DefaultTreeTableModel
import java.awt.Component
import java.util.*
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.ListSelectionModel
import javax.swing.tree.DefaultTreeCellRenderer

/**
 * UI Class to prepare Findings Table
 */
class FindingsTable {

    private val headings = arrayOf("Description", "Resource", "Path")
    private var root: Node? = null
    private var model: DefaultTreeTableModel? = null
    private var table: JXTreeTable? = JXTreeTable()

    /*
        Creates findings table based on passed error data, preferences data and grouping param
     */
     fun createFindingsTable(
         errorDetailsMap: HashMap<String, Vector<HashMap<String, String>>>,
         preferencesData: HashMap<String, HashMap<String, String>>,
         groupingParam:String) {

        //No Error Type,Icon or GroupKey
        root = Node(null,Node.ROOTNODETYPE,null ,null,null,arrayOf("Root"))


        //Populate root based on grouping param
         when(groupingParam){
             "Type" -> populateRootByType(root!!,errorDetailsMap,preferencesData)
             "Severity" -> populateRootBySeverity(root!!,errorDetailsMap,preferencesData)
             "Resource" -> populateRootByResource(root!!,errorDetailsMap,preferencesData)
             "No Grouping" -> populateRootWithoutGrouping(root!!,errorDetailsMap,preferencesData)
         }
        model = DefaultTreeTableModel(root, headings.toMutableList())
        table!!.treeTableModel = model

        //Selection Model
        table!!.rowSelectionAllowed = true
        table!!.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table!!.tableHeader.reorderingAllowed = false

        table!!.autoCreateRowSorter = true

        //Custom TableCellRenderer to display severity icons
        table!!.treeCellRenderer = treeCellRenderer()


        table!!.setShowGrid(true, true)
        table!!.isColumnControlVisible = false
        table!!.packAll()

    }

    fun getFindingsTable(): JXTreeTable? {
        return table
    }

    //Called when errors are grouped by type
    private fun populateRootByType(
        root: Node,
        errorDetailsMap: HashMap<String, Vector<HashMap<String, String>>>,
        preferencesData: HashMap<String, HashMap<String, String>>
    ){
        for(key in errorDetailsMap.keys){

            //Prepare Error Group Name and Count
            val errorTypeName = preferencesData[key]!!["name"]!!
            val errorCount = errorDetailsMap[key]!!.size
            val groupName ="$errorTypeName ($errorCount)"
            val groupIcon = getIconForSeverityValue(preferencesData[key]!!["severity"]!!)
            val errorGroupNode = Node(null,Node.ROOTNODETYPE, key,groupIcon,key ,arrayOf(groupName))

            for(entry in errorDetailsMap[key]!!){

                val errorIdx = entry["idx"]
                val errorDescription = entry["description"].toString()
                val errorResource = entry["resource"].toString()
                val errorPath = entry["path"].toString().replace(".","/")
                val errorPathValue = "/$errorPath.java"
                val data = arrayOf(errorDescription,errorResource,errorPathValue)
                val child = Node(errorIdx!!.toInt(),Node.CHILDNODETYPE,null,groupIcon,null,data)

                errorGroupNode.add(child)
            }
            root.add(errorGroupNode)
        }
    }

    //Returns Icon based on severity value
    private fun getIconForSeverityValue(severityValue: String): Icon? {
        var icon:Icon? = null
        when(severityValue.lowercase()){
            "blocker" -> icon = com.contemplateltd.intellij.threadsafeintegration.icons.PluginIcons.blockerSeverity
            "critical" -> icon = com.contemplateltd.intellij.threadsafeintegration.icons.PluginIcons.criticalSeverity
            "major" -> icon = com.contemplateltd.intellij.threadsafeintegration.icons.PluginIcons.majorSeverity
            "minor" -> icon = com.contemplateltd.intellij.threadsafeintegration.icons.PluginIcons.minorSeverity
            "info" ->    icon = com.contemplateltd.intellij.threadsafeintegration.icons.PluginIcons.infoSeverity
        }
        return icon
    }

    //Custom Tree Cell Renderer to show severity icons
    private fun treeCellRenderer(): DefaultTreeCellRenderer {
        val treeCellRenderer = object:DefaultTreeCellRenderer() {
            private val label = JLabel();
            override fun getTreeCellRendererComponent(
                tree: JTree?,
                value: Any?,
                sel: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ): Component {
                (value as Node).userObject
                label.icon = value.getIcon();
                label.text = value.data[0];
                return label
            }
        }
        return treeCellRenderer
    }

    //Called when errors are grouped by severity
    private fun populateRootBySeverity(
        root: Node,
        errorDetailsMap: HashMap<String, Vector<HashMap<String, String>>>,
        preferencesData: HashMap<String, HashMap<String, String>>
    ){

        for(key in errorDetailsMap.keys){

            //Prepare Error Group Name and Count
            val errorSeverity = key
            val errorCount = errorDetailsMap[key]!!.size
            val groupName ="$errorSeverity ($errorCount)"
            val groupIcon = getIconForSeverityValue(errorSeverity)
            val errorGroupNode = Node(null,Node.ROOTNODETYPE, key,groupIcon,key ,arrayOf(groupName))

            for(entry in errorDetailsMap[key]!!){

                val errorIdx = entry["idx"]
                val errorDescription = entry["description"].toString()
                val errorResource = entry["resource"].toString()
                val errorPath = entry["path"].toString().replace(".","/")
                val errorPathValue = "/$errorPath.java"
                val data = arrayOf(errorDescription,errorResource,errorPathValue)
                val child = Node(errorIdx!!.toInt(),Node.CHILDNODETYPE,null,groupIcon,null,data)

                errorGroupNode.add(child)

            }

            root.add(errorGroupNode)

        }

    }

    //Called when errors are grouped by resource
    private fun populateRootByResource(
        root: Node,
        errorDetailsMap: HashMap<String, Vector<HashMap<String, String>>>,
        preferencesData: HashMap<String, HashMap<String, String>>
    ) {

        for(key in errorDetailsMap.keys){
            val errorCount = errorDetailsMap[key]!!.size
            val groupName ="$key ($errorCount)"
            val groupIcon = AllIcons.FileTypes.Java
            val errorGroupNode = Node(null,Node.ROOTNODETYPE, key,groupIcon,key ,arrayOf(groupName))
            for(entry in errorDetailsMap[key]!!){

                val errorIdx = entry["idx"]
                val errorDescription = entry["description"].toString()
                val errorResource = entry["resource"].toString()
                val errorPath = entry["path"].toString().replace(".","/")
                val errorPathValue = "/$errorPath.java"
                val data = arrayOf(errorDescription,errorResource,errorPathValue)

                val errorType = entry["type"]
                val icon = getIconForSeverityValue(preferencesData[errorType]!!["severity"]!!)

                val child = Node(errorIdx!!.toInt(),Node.CHILDNODETYPE,null,icon,null,data)

                errorGroupNode.add(child)
            }
            root.add(errorGroupNode)
        }
    }

    //Called when errors are ungrouped
    private fun populateRootWithoutGrouping(root: Node, errorDetailsMap: HashMap<String, Vector<HashMap<String, String>>>, preferencesData: HashMap<String, HashMap<String, String>>) {
        for(entry in errorDetailsMap["all"]!!){
            val errorIdx = entry["idx"]
            val errorDescription = entry["description"].toString()
            val errorResource = entry["resource"].toString()
            val errorPath = entry["path"].toString().replace(".","/")
            val errorPathValue = "/$errorPath.java"
            val data = arrayOf(errorDescription,errorResource,errorPathValue)
            val errorType = entry["type"]
            val icon = getIconForSeverityValue(preferencesData[errorType]!!["severity"]!!)
            val child = Node(errorIdx!!.toInt(),Node.CHILDNODETYPE,null,icon,null,data)
            root.add(child)
        }

    }
}