package com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.UI

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.table.JBTable
import javax.swing.DefaultCellEditor
import javax.swing.JLabel
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class PreferencesPageTable(tableModel : DefaultTableModel ) {
    /** Column Description and Index
        Enable Rule -> 0
        Rule Name -> 1
        Rule Category -> 2
        Rule Severity -> 3
        Additional Parameter name -> 4
        Additional Parameter value -> 5
    **/
    private var preferencesTable = JBTable()

    init{
        //Use tableModel and create preferences table component
        createPreferencesTable(tableModel)
    }

    //Setup preferencesTable
    private fun createPreferencesTable(tableModel: DefaultTableModel) {

        preferencesTable.model = tableModel


        //Adding Severity Dropdown
        val severityComboBox = ComboBox<String>()
        severityComboBox.addItem("blocker")
        severityComboBox.addItem("critical")
        severityComboBox.addItem("major")
        severityComboBox.addItem("minor")
        severityComboBox.addItem("info")


        //Dropdown to select Severity value
        preferencesTable.columnModel.getColumn(3).cellEditor = DefaultCellEditor(severityComboBox)


//        //Button to store additional Param value
//        preferencesTable.columnModel.getColumn(5).cellEditor = ParamEditButtonCellEditor()

        //Adjusting Table Size and other adjustments

        preferencesTable.tableHeader.reorderingAllowed = false
        preferencesTable.autoResizeMode = JBTable.AUTO_RESIZE_OFF
        preferencesTable.columnModel.getColumn(0).resizable = false
        preferencesTable.columnModel.getColumn(0).width = 20
        preferencesTable.columnModel.getColumn(0).maxWidth = 20

        preferencesTable.columnModel.getColumn(1).resizable = true
        preferencesTable.columnModel.getColumn(1).width = 250
        preferencesTable.columnModel.getColumn(1).maxWidth = 450

        preferencesTable.columnModel.getColumn(2).resizable = true
        preferencesTable.columnModel.getColumn(2).width = 100
        preferencesTable.columnModel.getColumn(2).maxWidth = 100

        preferencesTable.columnModel.getColumn(3).resizable = false
        preferencesTable.columnModel.getColumn(3).width = 60
        preferencesTable.columnModel.getColumn(3).maxWidth = 60

        preferencesTable.columnModel.getColumn(4).resizable = false
        preferencesTable.columnModel.getColumn(4).width = 80
        preferencesTable.columnModel.getColumn(4).maxWidth = 80

        preferencesTable.columnModel.getColumn(5).resizable = false
        preferencesTable.columnModel.getColumn(5).width = 90
        preferencesTable.columnModel.getColumn(5).maxWidth = 90

        //Center Text Alignment for Param Value column
        val centerRenderer = DefaultTableCellRenderer()
        centerRenderer.horizontalAlignment = JLabel.CENTER
        preferencesTable.columnModel.getColumn(5).cellRenderer = centerRenderer


        preferencesTable.rowSelectionAllowed = true
        preferencesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)


        //Sort Data
        preferencesTable.autoCreateRowSorter = true
        preferencesTable.rowSorter.toggleSortOrder(1)

    }

    //Returns preferences table component
    fun getPreferencesTable() : JBTable{
        return preferencesTable
    }

    fun updatePreferenceTable(tableModel: DefaultTableModel){
        createPreferencesTable(tableModel)
    }


}