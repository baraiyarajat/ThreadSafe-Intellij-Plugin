package com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.UI

import javax.swing.JOptionPane
import javax.swing.JPanel

class ErrorPromptDialogBox {

    companion object {
        fun openInvalidParamValuePrompt(parentComponent: JPanel, ruleName: String) {
            JOptionPane.showMessageDialog(
                parentComponent,
                "Param Value Should be between 0 and 100 for the rule: $ruleName.",
                "Invalid Param Value",
                JOptionPane.WARNING_MESSAGE
            )
        }
    }

}