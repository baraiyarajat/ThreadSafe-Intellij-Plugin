package com.contemplateltd.intellij.threadsafeintegration.actions.runAnalysisAction


import com.intellij.openapi.progress.ProgressIndicator

/**
 * Progress Indicator Class to track analysis progress
 * @param projectName -> Project Name for which analysis is triggered
 * @param progressIndicator -> ProgressIndicator object to update progress and show on IDE
 */

class IntellijProgressMonitor(projectName:String,progressIndicator:ProgressIndicator): AnalyserProgressMonitor() {

    private var projectName: String? = null
    private var progressIndicator:ProgressIndicator? = null
    private var totalWork : Int? = null
    private var completedTasks:Double = 0.0


    init {
        this.projectName = projectName
        this.progressIndicator = progressIndicator
    }

    override fun begin(totalWork: Int) {
        super.begin(totalWork)
        this.totalWork = totalWork
        progressIndicator!!.text = "Running ThreadSafe Analysis on Project: $projectName"
    }

    override fun worked(worked: Int) {
        completedTasks+=worked
        progressIndicator!!.fraction = completedTasks / totalWork!!
    }

    override fun isCancelled(): Boolean {
        return super.isCancelled() || progressIndicator!!.isCanceled
    }

    override fun done() {
        super.done()
        progressIndicator!!.fraction = 1.0
    }

}