package com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.UI

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel


//UI class that provides UI object to PreferencesPageController

class PreferencesPage(tableModel : DefaultTableModel) {


//    private val preferencesPageUIPanel:JPanel? = null
    private val preferencesTableUI =PreferencesPageTable(tableModel)
    private val preferencesTable = preferencesTableUI.getPreferencesTable()
    private val detailsAreaTextPaneUI = PreferencesPageDetailsTextArea()
    private val sideButtonPanelUI = PreferencesPageSideButtonPanel()
    private val preferencesPageUIPanel:JPanel = JPanel()

    init {
        createPreferencesPage()
    }

    //Creates Preferences Page
    private fun createPreferencesPage(){

        //Panel with Box Layout
        val layout = BoxLayout(preferencesPageUIPanel, BoxLayout.Y_AXIS)
        preferencesPageUIPanel.layout = layout

        //Add rowSelectionListener for preferencesTable
        //Sets Text in Details Text Area
        preferencesTable.selectionModel.addListSelectionListener(rowSelectionListener())

        //Create and add HorizontalTableSideButtonPanel to main ContentPanel
        //It contains Table and Side Buttons
        preferencesPageUIPanel.add(createHorizontalTableSideButtonPanel())

        //Add Empty Space
        preferencesPageUIPanel.add(Box.createRigidArea(Dimension(0, 10)))
        //Create and add DetailsAreaPanel to main ContentPanel
        preferencesPageUIPanel.add(createDetailAreaPanel())




    }

    private fun rowSelectionListener(): ListSelectionListener {

        return ListSelectionListener {
            var detailsAreaText = ""
            val originalIdx =  preferencesTable.selectedRow

            if(originalIdx!=-1){
                val idx = preferencesTable.convertRowIndexToModel(originalIdx)
                detailsAreaText = "<p style=\"color:#BBBBBB\">"
                val ruleName = preferencesTable.model.getValueAt(idx, 1) as String
                detailsAreaText += preferencesTable.model.getValueAt(idx, 1).toString().trim()
                detailsAreaText += " (<a style=\"color:#287BDE\" href='$ruleName'>more</a>)"
                detailsAreaText += "</p>"
            }

            //Change detailsAreaTextPane Text based on row selection
            detailsAreaTextPaneUI.setDetailsAreaText(detailsAreaText)

        }
    }


    private fun createTablePanel(): JBScrollPane {

        val tablePanel = JBScrollPane(preferencesTable)
        //Set tablePanel policies
        tablePanel.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        tablePanel.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        //Set tablePanel Dimensions
        tablePanel.minimumSize = Dimension(1200, 2000)
        tablePanel.maximumSize = Dimension(1200, 2000)

        return tablePanel

    }

    //Creating Panel to add Table and Side Button Panel
    private fun createHorizontalTableSideButtonPanel(): JPanel {

        val horizontalTableSideButtonPanel = JPanel()
        val horizontalTableSideButtonPanelLayout = BoxLayout(horizontalTableSideButtonPanel, BoxLayout.X_AXIS)
        horizontalTableSideButtonPanel.layout = horizontalTableSideButtonPanelLayout

        //Create TablePane
        val tablePanel:JBScrollPane = createTablePanel()
        horizontalTableSideButtonPanel.add(tablePanel)
        //Get SideButtonsPanel
        val sideButtonPanel = sideButtonPanelUI.getSideButtonPanel()
        horizontalTableSideButtonPanel.add(sideButtonPanel)
        horizontalTableSideButtonPanel.alignmentX = Component.LEFT_ALIGNMENT
        return horizontalTableSideButtonPanel
    }



    private fun createDetailAreaPanel(): JPanel {
        //Adding Rule Description Text Area
        val detailsAreaPanel = detailsAreaTextPaneUI.getDetailsAreaPanel()
        detailsAreaPanel.alignmentX = Component.LEFT_ALIGNMENT
        return detailsAreaPanel


    }

    fun getPreferencesPageUIPanel(): JPanel {
        return preferencesPageUIPanel
    }

    //Returns the specified side Button
    fun getSideButton(buttonName:String): JButton? {

        when(buttonName) {
            "EnableAll" -> return sideButtonPanelUI.getEnableAllButton()
            "DisableAll" -> return sideButtonPanelUI.getDisableAllButton()
            "Export" -> return sideButtonPanelUI.getExportButton()
            "Import" -> return sideButtonPanelUI.getImportButton()
            "RestoreDefault" -> return sideButtonPanelUI.getRestoreDefaultsButton()
            else -> println("Invalid ButtonName:$buttonName")
        }
        return null
    }


    fun getPreferencesTable(): JBTable {
        return preferencesTable
    }

    //Update preference table using new TableModel
    fun updatePreferenceTable(tableModel : DefaultTableModel){
        preferencesTableUI.updatePreferenceTable(tableModel)
    }



}