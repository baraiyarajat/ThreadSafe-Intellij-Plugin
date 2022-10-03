package com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow.UI

import com.intellij.icons.AllIcons
import org.jdesktop.swingx.JXComboBox
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel


/**
 * Creates ButtonsPanel for FindingsWindow
 */

class FindingsWindowButtonPanel {

    //Creating Buttons
    private val runAnalysisButton = JButton()
    private val clearMarkersButton = JButton()
    private val expandAllButton = JButton()
    private val collapseAllButton = JButton()


    //GroupFindings Menu
    private var groupFindingsComboBox:JXComboBox? = null
//    private val groupByTypeParamIdx = 0
//    private val groupBySeverityParamIdx = 1
//    private val groupByResourceParamIdx = 2
//    private val noGroupParamIdx = 3

    //Creating FindingsWindowButtonPanel
    private val findingsWindowButtonPanel =JPanel()


    init{
        createSideButtonPanel()
    }

    companion object{
        private var currentGroupFindingsParameter = 0
//        fun getCurrentGroupingParameter(): Int {
//            return currentGroupFindingsParameter
//        }
//        fun setCurrentGroupingParameter(groupingParameter:Int){
//            currentGroupFindingsParameter = groupingParameter
//        }
    }

    private fun createSideButtonPanel() {
        //Button Panels Layout and Size
        val buttonsPanelLayout = BoxLayout(findingsWindowButtonPanel, BoxLayout.X_AXIS)
        findingsWindowButtonPanel.layout = buttonsPanelLayout
        findingsWindowButtonPanel.minimumSize = Dimension(1000,35)
        findingsWindowButtonPanel.maximumSize = Dimension(1000,35)

        //Customizing GroupFindings
        val groupFindingsOptions = arrayOf("Type","Severity","Resource","No Grouping")
        groupFindingsComboBox = JXComboBox(groupFindingsOptions)
        groupFindingsComboBox!!.selectedItem = groupFindingsOptions[currentGroupFindingsParameter]

        //Fixing Button Sizes
        runAnalysisButton.maximumSize = Dimension(20, 30)
        clearMarkersButton.maximumSize = Dimension(20, 30)
        expandAllButton.maximumSize = Dimension(20, 30)
        collapseAllButton.maximumSize = Dimension(20, 30)
        groupFindingsComboBox!!.maximumSize = Dimension(120,30)

        //Set ToolTip Text
        runAnalysisButton.toolTipText = "Run ThreadSafe Analysis on current project"
        clearMarkersButton.toolTipText = "Clear all findings"
        expandAllButton.toolTipText = "Expand all"
        collapseAllButton.toolTipText = "Collapse all"
        groupFindingsComboBox!!.toolTipText = "Group Findings"

        //Add Button Icons
        runAnalysisButton.icon = AllIcons.Actions.Execute
        clearMarkersButton.icon = AllIcons.Actions.Cancel
        expandAllButton.icon = AllIcons.Actions.Expandall
        collapseAllButton.icon = AllIcons.Actions.Collapseall


        //Remove Button Borders
        runAnalysisButton.isBorderPainted = false
        runAnalysisButton.isContentAreaFilled = false

        clearMarkersButton.isBorderPainted = false
        clearMarkersButton.isContentAreaFilled = false

        expandAllButton.isBorderPainted = false
        expandAllButton.isContentAreaFilled = false

        collapseAllButton.isBorderPainted = false
        collapseAllButton.isContentAreaFilled = false


        //Adding Buttons to the panel
        findingsWindowButtonPanel.add(runAnalysisButton)
        findingsWindowButtonPanel.add(clearMarkersButton)
        //Vertical Space to categorize buttons
        findingsWindowButtonPanel.add(Box.createRigidArea(Dimension(10, 35)))
        findingsWindowButtonPanel.add(expandAllButton)
        findingsWindowButtonPanel.add(collapseAllButton)
        //Vertical Space to categorize buttons
        findingsWindowButtonPanel.add(Box.createRigidArea(Dimension(10, 35)))
        findingsWindowButtonPanel.add(groupFindingsComboBox)
    }

    fun getRunAnalysisButton(): JButton {
        return runAnalysisButton
    }

    fun getClearMarkersButton(): JButton {
        return clearMarkersButton
    }

    fun getExpandAllButton(): JButton {
        return expandAllButton
    }

    fun getCollapseAllButton(): JButton {
        return collapseAllButton
    }

    fun getGroupFindingsButton(): JXComboBox? {
        return groupFindingsComboBox
    }

    fun getFindingsWindowButtonsPanel(): JPanel {
        return findingsWindowButtonPanel
    }

}