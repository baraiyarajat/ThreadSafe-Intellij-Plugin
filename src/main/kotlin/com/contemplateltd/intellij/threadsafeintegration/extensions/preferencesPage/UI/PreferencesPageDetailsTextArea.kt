package com.contemplateltd.intellij.threadsafeintegration.extensions.preferencesPage.UI

import com.contemplateltd.intellij.threadsafeintegration.util.HelpPageProvider
import java.awt.Component
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class PreferencesPageDetailsTextArea {


    private var detailsAreaTextPane = JTextPane()
    private var detailsArea = JTextArea()
    private var detailsAreaPanel:JPanel =  createDetailsTextAreaPanel()
    private var helpPageProvider = HelpPageProvider()

    //TextBox Component
    private fun createDetailsTextAreaPanel(): JPanel {

        val detailsLabel = JLabel()
        detailsLabel.text = "Rule Details:"

        detailsLabel.alignmentX = Component.LEFT_ALIGNMENT
        detailsLabel.alignmentY = Component.BOTTOM_ALIGNMENT

        //Details Area
        detailsArea.text = ""
        detailsArea.alignmentX = Component.LEFT_ALIGNMENT
        detailsArea.alignmentY = Component.TOP_ALIGNMENT
        detailsArea.isEnabled = false


        //Details Area TextPane
        detailsAreaTextPane.contentType = "text/html"
        detailsAreaTextPane.text = ""
        detailsAreaTextPane.isEditable = false
        detailsAreaTextPane.alignmentX = Component.LEFT_ALIGNMENT
        detailsAreaTextPane.addHyperlinkListener(detailsAreaHyperlinkListener())


        val detailsAreaPanel = JPanel()

        val detailsAreaPanelLayout = BoxLayout(detailsAreaPanel, BoxLayout.Y_AXIS)

        detailsAreaPanel.layout = detailsAreaPanelLayout
        detailsAreaPanel.add(detailsLabel)
        detailsAreaPanel.add(detailsAreaTextPane)
        detailsAreaPanel.minimumSize = Dimension(4000, 150)
        detailsAreaPanel.maximumSize = Dimension(4000, 150)
        detailsAreaPanel.alignmentX = Component.LEFT_ALIGNMENT

        return detailsAreaPanel

    }

    //Text Box Listener
    private fun detailsAreaHyperlinkListener(): HyperlinkListener {

        return HyperlinkListener { e ->

            if (HyperlinkEvent.EventType.ACTIVATED == e.eventType) {
                val ruleName = e.description.toString()
                //OpenHTMLEditor
                helpPageProvider.openHtmlEditor(ruleName)

            }
        }
    }

    fun setDetailsAreaText(requiredText:String){
        detailsAreaTextPane.text = requiredText
    }

    fun getDetailsAreaPanel(): JPanel {
        return detailsAreaPanel
    }

}