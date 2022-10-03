package com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.UI

import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class PreferencesPageSideButtonPanel {

    //Creating Buttons
    private val enableAllButton = JButton("Enable All")
    private val disableAllButton = JButton("Disable All")
    private val exportButton = JButton("Export")
    private val importButton = JButton("Import")
    private val restoreDefaultsButton = JButton("Restore Defaults")

    private val sideButtonPanel = createSideButtonPanel()

    private fun createSideButtonPanel(): JPanel {

        val buttonsPanel = JPanel()
        val buttonsPanelLayout = BoxLayout(buttonsPanel, BoxLayout.Y_AXIS)
        buttonsPanel.layout = buttonsPanelLayout

        //Fixing Button Sizes
        enableAllButton.maximumSize = Dimension(120, 40)
        disableAllButton.maximumSize = Dimension(120, 40)
        exportButton.maximumSize = Dimension(120, 40)
        importButton.maximumSize = Dimension(120, 40)
        restoreDefaultsButton.maximumSize = Dimension(120, 40)

        //Adding Buttons to the panel
        buttonsPanel.add(enableAllButton)
        buttonsPanel.add(disableAllButton)
        buttonsPanel.add(exportButton)
        buttonsPanel.add(importButton)
        buttonsPanel.add(restoreDefaultsButton)

        return buttonsPanel


    }


    fun getEnableAllButton(): JButton {
        return enableAllButton
    }

    fun getDisableAllButton(): JButton {
        return disableAllButton
    }

    fun getExportButton(): JButton {
        return exportButton
    }

    fun getImportButton(): JButton {
        return importButton
    }

    fun getRestoreDefaultsButton(): JButton {
        return restoreDefaultsButton
    }


    fun getSideButtonPanel(): JPanel {
        return sideButtonPanel
    }


}