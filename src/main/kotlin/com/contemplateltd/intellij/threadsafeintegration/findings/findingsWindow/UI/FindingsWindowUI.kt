package com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow.UI

import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import java.util.*
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel


/**
 * Class that constructs and returns Findings Window UI
 * Contains Findings Table, Error Details Area and Buttons
 */
class FindingsWindowUI {


    private val findingsTableUI = FindingsTable()
    private var findingsWindowButtonsPanel = FindingsWindowButtonPanel()
    private var findingsWindowPanel = JPanel()
    private var tableAndButtonsPanel = JPanel()
    private var errorDetailPanel = JPanel()
    private var findingsTablePanel = JBScrollPane()
    private var errorDetailsAreaUI = ErrorDetailsArea()



    fun getFindingsWindow(): JPanel {
        return findingsWindowPanel
    }

    fun constructFindingsWindow(errorDetailsMap: HashMap<String, Vector<HashMap<String, String>>>,
                                preferencesData: HashMap<String, HashMap<String, String>>) {

        //findingsWindowPanel contains two panels: tableAndButtonsPanel and errorDetailsPanel
        val findingsWindowPanelLayout = BoxLayout(findingsWindowPanel, BoxLayout.X_AXIS)
        findingsWindowPanel.layout = findingsWindowPanelLayout

        //tableAndButtonsPanel contains two panels: findingsTablePanel and buttonsPanel
        val tableAndButtonsPanelLayout = BoxLayout(tableAndButtonsPanel, BoxLayout.Y_AXIS)
        tableAndButtonsPanel.layout = tableAndButtonsPanelLayout

        //Adding buttonsPanel to tableAndButtonsPanel
        tableAndButtonsPanel.add(findingsWindowButtonsPanel.getFindingsWindowButtonsPanel())

        val groupingParam = findingsWindowButtonsPanel.getGroupFindingsButton()!!.selectedItem!!.toString()

        //Create FindingsTable and add to findingsTablePanel
        findingsTableUI.createFindingsTable(errorDetailsMap,preferencesData,groupingParam)

        findingsTablePanel = JBScrollPane(findingsTableUI.getFindingsTable())
        findingsTablePanel.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        findingsTablePanel.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        findingsTablePanel.minimumSize = Dimension(1000,800)
        findingsTablePanel.maximumSize = Dimension(1000,800)

        //Adding findingsTablePanel to tableAndButtonsPanel
        tableAndButtonsPanel.add(findingsTablePanel)

        //Adding tableAndButtonsPanel to findingsWindowPanel
        findingsWindowPanel.add(tableAndButtonsPanel)

        //Rigid Area Between tableAndButtonsPanel and errorDetailsPanel
        findingsWindowPanel.add(Box.createRigidArea(Dimension(10, 500)))

        //errorDetailsPanel
        val errorDetailPanelLayout = BoxLayout(errorDetailPanel,BoxLayout.Y_AXIS)
        errorDetailPanel.layout = errorDetailPanelLayout

        //empty area
        errorDetailPanel.add(Box.createRigidArea(Dimension(500, 20)))
        //Error Details Text Pane
        errorDetailPanel.add(errorDetailsAreaUI.getErrorDetailsTextPane())

        //Setting errorDetailsPanel Size
        errorDetailPanel.minimumSize = Dimension(500,500)
        errorDetailPanel.maximumSize = Dimension(500,1000)

        //Adding errorDetailsPanel to findingsWindowPanel
        findingsWindowPanel.add(errorDetailPanel)

    }

    fun getFindingsWindowButtonsPanel(): FindingsWindowButtonPanel {
        return findingsWindowButtonsPanel
    }

    fun getFindingsTableUI(): FindingsTable {
        return findingsTableUI
    }

    fun getErrorDetailsAreaUI(): ErrorDetailsArea {
        return errorDetailsAreaUI
    }

}