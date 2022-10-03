package com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow.UI

import javax.swing.JTextPane

/*
    UI class that provides error Details Area
 */

class ErrorDetailsArea {

    private var errorDetailsTextPane = JTextPane()

    init{
        setErrorDetailsTextPaneProperties()
    }

    private fun setErrorDetailsTextPaneProperties(){
        errorDetailsTextPane.contentType = "text/html"
        errorDetailsTextPane.text = ""
        errorDetailsTextPane.isEditable = false
        errorDetailsTextPane.autoscrolls = true
    }

    fun getErrorDetailsTextPane(): JTextPane {
        return errorDetailsTextPane
    }
}