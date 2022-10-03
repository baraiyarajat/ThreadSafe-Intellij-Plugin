package com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow.UI

import com.intellij.openapi.project.Project
import javax.swing.JFrame
import javax.swing.JOptionPane

/*
    Class to prompt Dialog Box to show warnings
 */
class DialogPromptClass {

    companion object{
        fun show(project: Project, frame: JFrame?,promptTitle:String,promptMessage:String){
            val options = arrayOf<Any>(
                "okay"
            )
            //Show Dialog Box and capture user input
            val userChoice = JOptionPane.showOptionDialog(
                frame, promptMessage,
                promptTitle,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]
            )
        }
    }

}