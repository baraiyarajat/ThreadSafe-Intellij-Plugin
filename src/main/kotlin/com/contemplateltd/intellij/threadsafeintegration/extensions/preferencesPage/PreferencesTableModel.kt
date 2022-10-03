package com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage

import java.util.*
import javax.swing.table.DefaultTableModel


//Provides TableModel for PreferencesTable
class PreferencesTableModel {

    //Vectors needed to keep track of changed table values
    private var enabledValues = Vector<Boolean>()
    private var paramValueData = Vector<Int>()
    private var severityValueData = Vector<String>()

    private var preferencesTableModel: DefaultTableModel? = null



     fun createPreferenceTableModel(preferenceData: HashMap<String, HashMap<String, String>>) {

        enabledValues = Vector<Boolean>()
        val nameData = Vector<String>()
        val categoryData = Vector<String>()
        val severityData = Vector<String>()
        severityValueData = Vector<String>()
        val paramNameData = Vector<String>()
        paramValueData = Vector<Int>()

        for (key in preferenceData.keys.sorted()) {
            if (preferenceData[key]!!["enabled"] == "true") {
                enabledValues.add(true)
            } else {
                enabledValues.add(false)
            }

            nameData.add(preferenceData[key]!!["name"])
            categoryData.add(preferenceData[key]!!["category"])
            severityData.add(preferenceData[key]!!["severity"])
            //To check for changes
            severityValueData.add(preferenceData[key]!!["severity"])

            if (preferenceData[key]!!.containsKey("paramName")) {
                paramNameData.add(preferenceData[key]!!["paramName"])
                paramValueData.add(preferenceData[key]!!["paramValue"]!!.toInt())
            } else {
                paramNameData.add("-")
                paramValueData.add(null)
            }
        }


        val tableModel = object : DefaultTableModel() {
            override fun getColumnClass(column: Int): Class<out Any> {
                when(column){
                    0-> return Boolean::class.javaObjectType
                    5-> return Int::class.javaObjectType
                }

                return String::class.javaObjectType

            }

            override fun isCellEditable(row: Int, column: Int): Boolean {
                if (column == 0 || column == 3) {
                    return true
                }else if(column == 5 && this.getValueAt(row,4)!="-"){
                    return true
                }
                return false
            }

        }

        tableModel.addColumn("", enabledValues)
        tableModel.addColumn("Rules", nameData)
        tableModel.addColumn("Category", categoryData)
        tableModel.addColumn("Severity", severityValueData)
        tableModel.addColumn("Param Name", paramNameData)
        tableModel.addColumn("Param Value", paramValueData)

         preferencesTableModel =  tableModel

    }



    fun getPreferencesTableModel(): DefaultTableModel? {
        return preferencesTableModel
    }

    fun getEnabledValues(): Vector<Boolean> {
        return enabledValues
    }

    fun getParamValueData(): Vector<Int> {
        return paramValueData
    }

    fun getSeverityData(): Vector<String> {
        return severityValueData
    }

    fun setEnabledValues( enabledValuesVector: Vector<Boolean>) {
        enabledValues = enabledValuesVector.clone() as Vector<Boolean>
    }

    fun setSeverityData( severityValuesVector : Vector<String>) {
        severityValueData = severityValuesVector.clone() as Vector<String>
    }
    fun setParamValueData(paramsValuesVector : Vector<Int>){
        paramValueData = paramsValuesVector.clone() as Vector<Int>
    }

}