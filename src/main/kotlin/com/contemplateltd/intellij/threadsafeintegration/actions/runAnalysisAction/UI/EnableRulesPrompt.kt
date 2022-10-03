package com.contemplateltd.intellij.threadsafeintegration.actions.runAnalysisAction.UI

import com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.PreferencesPageController
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import javax.swing.JFrame
import javax.swing.JOptionPane

class EnableRulesPrompt {

    companion object{
        fun enableRulePrompt(project: Project, frame: JFrame?){
            val options = arrayOf<Any>(
                "Configure",
                "Cancel"
            )
            //Show Dialog Box and capture user input
            val userChoice = JOptionPane.showOptionDialog(
                frame, "Please enable at least one rule in ThreadSafe Preferences Page",
                "ThreadSafe Rules Disabled",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[1]
            )

            if(userChoice== JOptionPane.YES_OPTION){
                ShowSettingsUtil.getInstance().showSettingsDialog(project, PreferencesPageController::class.java)
            }
        }
    }
}