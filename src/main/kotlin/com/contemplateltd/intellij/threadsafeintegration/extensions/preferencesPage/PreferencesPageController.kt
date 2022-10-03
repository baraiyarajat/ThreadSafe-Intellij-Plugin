package com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage

import com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.UI.ErrorPromptDialogBox
import com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.UI.PreferencesPage
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import java.awt.event.ActionListener
import java.io.File
import java.nio.file.Paths
import java.security.PrivilegedActionException
import java.util.*
import javax.swing.JComponent
import javax.swing.JOptionPane

/*
    Rules Preferences Page Extension allowing users to enable/disable/modify rule configurations required
    during analysis
*/

class PreferencesPageController : Configurable {

    //Object to provide helper functions
    private val preferencesPageUtil:PreferencesPageUtil = PreferencesPageUtil()
    //Object to store table model
    private val preferencesTableModelObject:PreferencesTableModel = PreferencesTableModel()
    //Object to store PreferencesPageUI
    private var preferencesPageUI:PreferencesPage? = null


    //Variables to store ruleConfiguration data from XML files
    private var defaultPreferenceData = HashMap<String, HashMap<String, String>>()
    private var currentPreferenceData = HashMap<String, HashMap<String, String>>()

    //Modified Table Values as Map
    private val modifiedEnabledValuesMap = HashMap<String, Boolean>()
    private val modifiedSeverityValuesMap = HashMap<String, String>()
    private val modifiedParamValuesMap = HashMap<String, Int>()

    override fun createComponent(): JComponent {

        //Initialize required XML configuration files
        val ruleConfigFilesInitialized = preferencesPageUtil.createRuleConfigurations()

        if(!ruleConfigFilesInitialized){
            //return empty component and state the error
            println("return empty component and state the error")
        }

        //Prepare HashMap Objects containing ruleConfigurations data
        //Default HashMap Object to restore to default values
        defaultPreferenceData = preferencesPageUtil.createPreferenceData("default")
        //Current HashMap Object to detect changes from UI
        currentPreferenceData = preferencesPageUtil.createPreferenceData("current")

        //Create PreferencesTableModel
        preferencesTableModelObject.createPreferenceTableModel(currentPreferenceData)

        //Create preferencesPageUI Object
        preferencesPageUI = PreferencesPage(preferencesTableModelObject.getPreferencesTableModel()!!)


        //Add Side Button Listeners
        setButtonListeners()

        //Create and return the Panel containing Table, Buttons and Details Text area
        return preferencesPageUI!!.getPreferencesPageUIPanel()


    }

    override fun isModified(): Boolean {

        var isModified = false

        for (idx in 0 until  preferencesTableModelObject.getEnabledValues().size) {
            val modelIdx = preferencesPageUI!!.getPreferencesTable().convertRowIndexToModel(idx)

            //Checking if Enabled values are changed
            if (preferencesTableModelObject.getEnabledValues().elementAt(modelIdx) != preferencesPageUI!!.getPreferencesTable().model.getValueAt(modelIdx, 0)) {
                isModified = true
            //Checking if Param values are changed
            } else if (preferencesTableModelObject.getParamValueData().elementAt(modelIdx) != preferencesPageUI!!.getPreferencesTable().model.getValueAt(modelIdx, 5)) {
                isModified = true
            //Checking if Severity values are changed
            } else if (preferencesTableModelObject.getSeverityData().elementAt(modelIdx) != preferencesPageUI!!.getPreferencesTable().model.getValueAt(modelIdx, 3)) {
                isModified = true
            }
        }

        return isModified
    }

    override fun apply() {
        //Modify Enabled Values data
        for (idx in 0 until preferencesTableModelObject.getEnabledValues().size) {

            val modelIdx = preferencesPageUI!!.getPreferencesTable().convertRowIndexToModel(idx)

            //Enabled Values
            preferencesTableModelObject.getEnabledValues()[modelIdx] = preferencesPageUI!!.getPreferencesTable().model.getValueAt(modelIdx, 0) as Boolean

            //Severity Values
            preferencesTableModelObject.getSeverityData()[modelIdx] = preferencesPageUI!!.getPreferencesTable().model.getValueAt(modelIdx, 3) as String

            //Param Values
            if(preferencesPageUI!!.getPreferencesTable().model.getValueAt(modelIdx, 5) != null){
                val ruleName = preferencesPageUI!!.getPreferencesTable().model.getValueAt(modelIdx, 1).toString()
                //Check if number between 0 and 100
                val paramValue = preferencesPageUI!!.getPreferencesTable().model.getValueAt(modelIdx, 5).toString().toInt()
                if(paramValue in 0..100){
                    preferencesTableModelObject.getParamValueData()[modelIdx] = paramValue

                }else{
                    //raise error
                    ErrorPromptDialogBox.openInvalidParamValuePrompt(preferencesPageUI!!.getPreferencesPageUIPanel(),ruleName)
                    val displayRowIdx = preferencesPageUI!!.getPreferencesTable().convertRowIndexToView(modelIdx)
//                    preferencesPageUI!!.getPreferencesTable().setValueAt(preferencesTableModelObject.getParamValueData()[modelIdx].toInt(),displayRowIdx,5)

                    //Focus on incorrect cell Value
                    focusOnInvalidCell(displayRowIdx)
                    //Check if other table values are modified
                    return
                }
            }
        }

        //Prepare Modified Table values as HashMaps
        //Populates modifiedEnabledValuesMap,modifiedSeverityValuesMap,modifiedParamValuesMap
        prepareModifiedMapValues()

        //Update the current XML config file using current table model
        preferencesPageUtil.xmlDocModifier(modifiedEnabledValuesMap,
                                            modifiedSeverityValuesMap,
                                                modifiedParamValuesMap)

    }

    override fun getDisplayName(): String {
        return preferencesPageUtil.getPreferencesPageProperty("PreferencesPageDisplayName")
    }

    //Update table values using current RuleConfigurations file
    override fun reset() {

        //Current HashMap Object to reset UI changes
        currentPreferenceData = preferencesPageUtil.createPreferenceData("current")
        //Updated Table Model
        preferencesTableModelObject.createPreferenceTableModel(currentPreferenceData)
        //Update PreferenceTable using new tableModel
        preferencesPageUI!!.updatePreferenceTable(preferencesTableModelObject.getPreferencesTableModel()!!)

    }

    //Prepare Modified Table values as HashMaps
    //Populates modifiedEnabledValuesMap,modifiedSeverityValuesMap,modifiedParamValuesMap
    private fun prepareModifiedMapValues(){
        //Populate Modified Values Map from current table values

        //Getting Modified Values
        for (idx in 0 until preferencesPageUI!!.getPreferencesTable().model.rowCount) {
            //Modified Enabled Values
            modifiedEnabledValuesMap[preferencesPageUI!!.getPreferencesTable().model.getValueAt(idx, 1) as String] = preferencesPageUI!!.getPreferencesTable().model.getValueAt(idx, 0) as Boolean
            //Modified Severity Values
            modifiedSeverityValuesMap[preferencesPageUI!!.getPreferencesTable().model.getValueAt(idx, 1) as String] = preferencesPageUI!!.getPreferencesTable().model.getValueAt(idx, 3) as String
            //Modified Param Values
            if (preferencesPageUI!!.getPreferencesTable().model.getValueAt(idx, 5) != null) {
                modifiedParamValuesMap[preferencesPageUI!!.getPreferencesTable().model.getValueAt(idx, 1) as String] = preferencesPageUI!!.getPreferencesTable().model.getValueAt(idx, 5) as Int
            }
        }
    }


    private fun focusOnInvalidCell(rowIdx:Int){
        preferencesPageUI!!.getPreferencesTable().editCellAt(rowIdx,5)
    }


    //Sets button listener functions present in Page UI
    private fun setButtonListeners(){

        //Enable all Button Action Listener
        preferencesPageUI!!.getSideButton("EnableAll")!!.addActionListener(enableAllAction())
        //Disable all Button Action Listener
        preferencesPageUI!!.getSideButton("DisableAll")!!.addActionListener(disableAllAction())
        //Export Button Action Listener
        preferencesPageUI!!.getSideButton("Export")!!.addActionListener(exportAction())
        //Import Button Action Listener
        preferencesPageUI!!.getSideButton("Import")!!.addActionListener(importAction())
        //Restore Defaults Action Listener
        preferencesPageUI!!.getSideButton("RestoreDefault")!!.addActionListener(restoreDefaultsAction())
    }


    //Enables Checkbox value of all rules
    private fun enableAllAction(): ActionListener {
        return ActionListener {
            val tableRowCount = preferencesPageUI!!.getPreferencesTable().model.rowCount
            for (idx in 0 until tableRowCount) {
                preferencesPageUI!!.getPreferencesTable().model.setValueAt(true, idx, 0)
            }
        }
    }

    //Disables Checkbox value of all rules
    private fun disableAllAction(): ActionListener {
        return ActionListener {
            val tableRowCount = preferencesPageUI!!.getPreferencesTable().model.rowCount
            for (idx in 0 until tableRowCount) {
                preferencesPageUI!!.getPreferencesTable().model.setValueAt(false, idx, 0)
            }

        }
    }


    //Exports current rule configurations as an XML file
    private fun exportAction(): ActionListener {
        return ActionListener {

            val exportCurrentConfigRulesDescriptor = FileChooserDescriptor(
                false, true, false, false, false, false
            )

            //Adding Descriptor Details
            exportCurrentConfigRulesDescriptor.description = "Choose the folder to save the current configurations in"
            exportCurrentConfigRulesDescriptor.title = "Export Rule Configurations"

            //Get Project Value
            val project = ProjectManager.getInstance().defaultProject

            //Get the selected folder path
            var exportConfigPath = ""

            //Opening Path Selector Dialog Box
            FileChooser.chooseFile(exportCurrentConfigRulesDescriptor, project, null) {
                exportConfigPath = it.path
            }

            if (exportConfigPath != "") {
                // Saving the current rules preferences before exporting
                apply()
                //Prepare Export Path
                val exportPath = Paths.get(exportConfigPath, "exportedRulesConfigFile.xml").toAbsolutePath().toString()
                //Delete File if it exists
                if (File(exportPath).exists()) {
                    File(exportPath).delete()
                }
                //Copy current rule configurations to destination path
                File(preferencesPageUtil.getCurrentRuleConfigPath()).copyTo(File(exportPath))
            }
        }
    }

    //Imports Rule configurations from specified XML file
    private fun importAction(): ActionListener {
        return ActionListener {

            val importCurrentConfigRulesDescriptor = FileChooserDescriptor(
                true, false, false, false, false, false
            )

            //Adding Descriptor Details
            importCurrentConfigRulesDescriptor.description = "Choose the config file to be imported"
            importCurrentConfigRulesDescriptor.title = "Import Rule Configurations"

            //Get Project Value
            val project = ProjectManager.getInstance().defaultProject
            var importConfigPath = ""

            //Opening Path Selector Dialog Box
            FileChooser.chooseFile(importCurrentConfigRulesDescriptor, project, null) {
                importConfigPath = it.path
            }

            if ( importConfigPath!="" && !importConfigPath.lowercase().endsWith(".xml")) {
                println("Invalid file selected. Please select an XML file.")

                showErrorDialogBox("Please select a .xml file")


            } else if( importConfigPath!="" ) {

                //Old values required to enable apply button
                val oldEnabledValues = preferencesTableModelObject.getEnabledValues().clone()
                val oldSeverityValues = preferencesTableModelObject.getSeverityData().clone()
                val oldParamValues = preferencesTableModelObject.getParamValueData().clone()

                var properFormat  = false

                try{
                    //Read Data from new imported filepath
                    currentPreferenceData = preferencesPageUtil.createPreferenceData("imported",importConfigPath)
                    properFormat = true
                }catch (error: PrivilegedActionException){
                    print(error)
                    showErrorDialogBox("Please select a valid .xml rule configurations file")
                }catch (error:Error){
                    print(error)
                    showErrorDialogBox("Please select a valid .xml rule configurations file")
                }

                if(properFormat){
                    //Create new table model and update table UI based on default Data
                    preferencesTableModelObject.createPreferenceTableModel(currentPreferenceData)
                    preferencesPageUI!!.updatePreferenceTable(preferencesTableModelObject.getPreferencesTableModel()!!)

                    //Set old values back which will be updated only if user clicks apply button
                    preferencesTableModelObject.setEnabledValues(oldEnabledValues as Vector<Boolean>)
                    preferencesTableModelObject.setSeverityData(oldSeverityValues as Vector<String>)
                    preferencesTableModelObject.setParamValueData( oldParamValues as Vector<Int>)

                }
            }
        }
    }

    //Restores Table values from default rule configurations file
    private fun restoreDefaultsAction(): ActionListener {

        return ActionListener {
            //Old values required to enable apply button
            val oldEnabledValues = preferencesTableModelObject.getEnabledValues().clone()
            val oldSeverityValues = preferencesTableModelObject.getSeverityData().clone()
            val oldParamValues = preferencesTableModelObject.getParamValueData().clone()

            //Create new table model and update table UI based on default Data
            preferencesTableModelObject.createPreferenceTableModel(defaultPreferenceData)
            preferencesPageUI!!.updatePreferenceTable(preferencesTableModelObject.getPreferencesTableModel()!!)

            //Set old values back which will be updated only if user clicks apply button
            preferencesTableModelObject.setEnabledValues(oldEnabledValues as Vector<Boolean>)
            preferencesTableModelObject.setSeverityData(oldSeverityValues as Vector<String>)
            preferencesTableModelObject.setParamValueData( oldParamValues as Vector<Int>)
        }
    }

    private fun showErrorDialogBox(message:String){
        JOptionPane.showMessageDialog(preferencesPageUI!!.getPreferencesTable(),
            message,
            "Selected File Error",
            JOptionPane.ERROR_MESSAGE)
    }


}