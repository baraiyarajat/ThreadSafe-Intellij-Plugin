package com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow.UI

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode
import javax.swing.Icon

/**
 * @param errorIdx  -> Key to find error from errorsMap
 * @param nodeType  -> Specifying whether the node is root or child type
 * @param errorType -> ErrorType of finding
 * @param icon      -> Icon to show in findings table
 * @param groupKey  -> Key to find errorGroup
 * @param data      -> Array of column entries
 */
open class Node(errorIdx:Int?,nodeType:Int,errorType:String?,icon:Icon?,groupKey:String?,data: Array<String>) : AbstractMutableTreeTableNode(data) {


    private var nodeType : Int? = null

    //Populated only if Node is RootType
    private var errorType:String? = null
    private var groupKey:String? = null
    //Populated only if Node is ChildType
    private var errorIdx : Int? = null
    //Icon
    private var icon:Icon? = null

    init{
        this.errorIdx = errorIdx
        this.nodeType = nodeType
        this.errorType = errorType
        this.icon = icon
        this.groupKey = groupKey
    }

    companion object{
        const val ROOTNODETYPE = 0
        const val CHILDNODETYPE = 1
    }


    override fun getColumnCount(): Int {
        return data.size
    }

    override fun getValueAt(columnIndex: Int): Any {
        return data[columnIndex]
    }

    val data: Array<String>
        get() = getUserObject() as Array<String>

    fun getIcon(): Icon? {
        return icon
    }

    fun getNodeType(): Int? {
        return nodeType
    }

    fun getGroupKey():String?{
        return groupKey
    }

    fun getErrorIdx():Int?{
        return errorIdx
    }

}