package com.contemplateltd.intellij.threadsafeintegration.findings.findingsWindow

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionListener
import java.util.*
import javax.swing.*
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel
import kotlin.collections.HashMap


/**
 * @param errorIdx -> Key to find error finding for ErrorsMap
 * @param errorsMap -> HashMap containing ThreadSafe findings
 * @param findingsLocationInfo -> HashMap containing error location info
 * @param findingsAccessAndLocksInfo -> HashMap containing Accesses and Locks info
 * @param guardsInfo -> HashMap containing guards info
 **/
class AccessesAndLocksController(errorIdx:Int,
                                 errorsMap:HashMap<Int, HashMap<String, String>>,
                                 findingsLocationInfo:HashMap<String, Vector<HashMap<String, String>>>,
                                 findingsAccessAndLocksInfo:HashMap<String, Vector<HashMap<String, String>>>,
                                 guardsInfo:HashMap<String, Vector<HashMap<String, String>>> ) {


    //Data
    private var errorIdx:Int? = null
    private var errorsMap:HashMap<Int, HashMap<String, String>>? = null
    private var findingsLocationInfo:HashMap<String, Vector<HashMap<String, String>>>? = null
    private var findingsAccessAndLocksInfo:HashMap<String, Vector<HashMap<String, String>>>? = null
    private var guardsInfo:HashMap<String, Vector<HashMap<String, String>>>? = null
    private var accessesAndLocksMethodData : HashMap<String, Vector<HashMap<String, String>>>? = null

    //UI
    private var accessesAndLocksTable = JBTable()
    private var accessesAndLocksTable2 = JBTable()
    private val radioButtonGroup = ButtonGroup()
    private val fieldDetailLabel = JLabel()


    init{
        this.errorIdx = errorIdx
        this.errorsMap = errorsMap
        this.findingsAccessAndLocksInfo = findingsAccessAndLocksInfo
        this.guardsInfo = guardsInfo
        this.findingsLocationInfo = findingsLocationInfo
    }

    //Prepares accessesAndLocksTable and accessesAndLocksTable2
    fun prepareTables(){


        //Preparing Table Model
        val iconData = Vector<Icon?>()
        val methodData = Vector<String>()
        val lineData = Vector<String>()
        val typeData = Vector<String>()
        val guardData = Vector<String>()

        val findingKey:String =  errorsMap!![errorIdx]!!["name"]!!

        println("AccessesAndLocksID: $findingKey")


        //GetAccessesAndLocksData
        val requireData = findingsAccessAndLocksInfo!![findingKey]
        val guardColumnData = HashMap<String, Vector<String>>()

         accessesAndLocksMethodData = HashMap()

        for(idx in 0 until requireData!!.size){

            var methodsFullDisplayText =requireData[idx]["method"]
            methodsFullDisplayText += if(requireData[idx]["desc"]!!.split(")")[0] != "("){
                "(...) : "
            }else{
                "() : "
            }

            if(requireData[idx]["desc"]!!.split(")")[1].startsWith("Ljava/lang/Object") ){
                methodsFullDisplayText+="Object"
            }else if(requireData[idx]["desc"]!!.split(")")[1].startsWith("Ljava/lang/String") ){
                methodsFullDisplayText+="String"
            }else if(requireData[idx]["desc"]!!.split(")")[1] == "V"){
                methodsFullDisplayText+="void"
            }else if(requireData[idx]["desc"]!!.split(")")[1] == "I"){
                methodsFullDisplayText+="int"
            }else if(requireData[idx]["desc"]!!.split(")")[1] == "Z") {
                methodsFullDisplayText += "boolean"
            }

            methodsFullDisplayText+=" - "
            methodsFullDisplayText+=requireData[idx]["className"]

            if(!methodData.contains(methodsFullDisplayText)){
                methodData.add(methodsFullDisplayText)


                //If No Guard, add icons here TODO
                if((guardsInfo!![findingKey] ==null || guardsInfo!![findingKey]!!.size==0) &&  requireData[idx]["type"]!!.lowercase().contains("unsync") ){
                    println("Unsync Added")
                    iconData.add(AllIcons.CodeWithMe.CwmTerminate)
                }else{
                    iconData.add(null)
                }
                val newVector = Vector<HashMap<String,String>>()
                accessesAndLocksMethodData!![methodsFullDisplayText!!] = newVector.clone() as Vector<HashMap<String, String>>
            }

            val newHashMap = kotlin.collections.HashMap<String,String>()
            newHashMap["line"] = requireData[idx]["line"]!!
            newHashMap["type"] = requireData[idx]["type"]!!
            accessesAndLocksMethodData!![methodsFullDisplayText!!]!!.add(newHashMap.clone() as HashMap<String, String>?)

        }

        val requiredGuardData = guardsInfo!![findingKey]
        var colIdx=0
        var idx = 0


        for(guardObject in requiredGuardData!!){

            guardColumnData["column$colIdx"] = Vector<String>()
            var name = ""
            if(guardObject["name"]!=""){
                name = guardObject["name"]!!
            }

            for(findingObject in findingsLocationInfo!![findingKey]!! ){
                var found = false
                if(guardObject["guardPath.locationRef.key"] == findingObject["key"]){
                    if(name!=""){
                        name = findingObject["className"]!!.split(".")[1] + ".this." + name
                    }else if( guardObject["intrinsic"] == "true" && findingObject.containsKey("name")){
                        name = findingObject["name"]!!
                        name = findingObject["className"]!!.split(".")[1] + ".this." + name
                    }
                    else{
                        name = findingObject["className"]!!.split(".")[1] + ".this"
                    }
                    guardData.add(name)

                    requiredGuardData[idx]["displayName"] = name

                    idx+=1

                    break
                } else {

                    for(accessObject in findingsAccessAndLocksInfo!![findingKey]!!){
                        if(accessObject["guardRefKey"] == guardObject["key"] &&accessObject["location"] == findingObject["key"] ){
                            if(findingObject.containsKey("name")){
                                name = findingObject["name"]!!
                                name = findingObject["className"]!!.split(".")[1] + ".this." + name
                            }else{
                                name = findingObject["className"]!!.split(".")[1] + ".this"
                            }
                            guardData.add(name)
                            found = true


                            requiredGuardData[idx]["displayName"] = name
                            idx+=1

                            break
                        }
                    }
                }
                if(found){
                    break
                }
            }

            //Adding Locks Status Data
            for(method in methodData){
                val methodGuardValues = Vector<String>()
                for(findingObject in findingsAccessAndLocksInfo!![findingKey]!!){
                    if(method.contains(findingObject["method"].toString())  ){
                        if(findingObject["guardRefKey"] == guardObject["key"]){
                            if(findingObject["status"]=="always"){
                                methodGuardValues.add("Always Held")
                            }else if(findingObject["status"]==""){
                                methodGuardValues.add("Not Held")
                            }else{
                                methodGuardValues.add("Maybe Held")
                            }
                        }else{
                            methodGuardValues.add("Not Held")
                        }
                    }
                }

                //0 ->Always Held
                //1 -> Maybe Held
                //2 ->Never Held

                var statusCode = 0
                for(value in methodGuardValues ){
                    if(value=="Not Held"){
                        statusCode = 2
                        break
                    }else if(value =="Maybe Held"){
                        statusCode = 1
                    }
                }

                if(statusCode==0){
                    guardColumnData["column$colIdx"]!!.add("Always Held")
                }else if(statusCode==1){
                    guardColumnData["column$colIdx"]!!.add("Maybe Held")
                }else{
                    guardColumnData["column$colIdx"]!!.add("Not Held")
                }

            }
            //Incrementing Col Idx
            colIdx+=1


        }

        //Adding Modified Required Data
        guardsInfo!![findingKey] = requiredGuardData.clone() as Vector<HashMap<String, String>>



        val accessessAndLocksTableModel = object : DefaultTableModel(){
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }

            override fun getColumnClass(column: Int): Class<out Any> {
                return if (column == 0) {
                    Icon::class.javaObjectType
                } else {
                    String::class.javaObjectType
                }
            }


        }

        val accessessAndLocksTableModel2 = object : DefaultTableModel(){
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }

            override fun getColumnClass(column: Int): Class<out Any> {
                return if (column == 0) {
                    Icon::class.javaObjectType
                } else {
                    String::class.javaObjectType
                }
            }


        }

        accessessAndLocksTableModel.addColumn("",iconData)
        accessessAndLocksTableModel.addColumn("Method Name",methodData)

        if(guardData.size==0){
            accessessAndLocksTableModel.addColumn("")
        }else{
            //Adding Guard Columns
            var idx = 0
            for(guardName in guardData){
                accessessAndLocksTableModel.addColumn(guardName,guardColumnData["column$idx"])
                idx+=1
            }
        }


        //Add Icon Data
        val notHeldIcon = AllIcons.CodeWithMe.CwmTerminate

        if(guardData.size>0){
            for(rowIdx in 0 until  accessessAndLocksTableModel.rowCount){
                if(accessessAndLocksTableModel.getValueAt(rowIdx,2) !="Always Held"){
                    accessessAndLocksTableModel.setValueAt(notHeldIcon,rowIdx,0)
                }
            }
        }

        accessessAndLocksTableModel2.addColumn("")
        accessessAndLocksTableModel2.addColumn("Line",lineData)
        accessessAndLocksTableModel2.addColumn("Type",typeData)

        //Setting up Table
        accessesAndLocksTable.model = accessessAndLocksTableModel
        accessesAndLocksTable.tableHeader.reorderingAllowed = false
        accessesAndLocksTable.autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
        accessesAndLocksTable.rowSelectionAllowed = true
        accessesAndLocksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)


        //Resizing
        accessesAndLocksTable.columnModel.getColumn(0).resizable = false
        accessesAndLocksTable.columnModel.getColumn(0).width = 20
        accessesAndLocksTable.columnModel.getColumn(0).preferredWidth = 20
        accessesAndLocksTable.columnModel.getColumn(0).maxWidth = 20

        accessesAndLocksTable.columnModel.getColumn(1).resizable = true
        accessesAndLocksTable.columnModel.getColumn(1).preferredWidth = 150

        accessesAndLocksTable.columnModel.getColumn(2).resizable = true
        accessesAndLocksTable.columnModel.getColumn(2).preferredWidth = 150

        if(accessesAndLocksTable.columnCount>3){
            for(idx in 3 until accessesAndLocksTable.columnCount){
                accessesAndLocksTable.columnModel.getColumn(idx).resizable = true
                accessesAndLocksTable.columnModel.getColumn(idx).preferredWidth = 150
            }
        }


        //Add Row Selection Listener
        accessesAndLocksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)


        println("Adding New Finding Key:$findingKey")
        //Action Upon selecting Row
        accessesAndLocksTable.selectionModel.addListSelectionListener(accessAndLocksSelectionListener())

        //Populating Table 2
        accessesAndLocksTable2.model = accessessAndLocksTableModel2
        resizeAccessesAndLocksTable2()

        //Adding Field Details Label
        fieldDetailLabel.text = "      Guards for the access to field "


        for(findingObject in findingsLocationInfo!![findingKey]!!){
            if (findingObject["locationMessage"]=="Problem location"){
                val className = findingObject["className"].toString().split(".")[1]
                val fieldName = findingObject["name"]

                var fieldType = ""

                fieldType = if(findingObject["type"].toString().contains("I")){
                    "int"
                }else if(findingObject["type"].toString().contains("Ljava/util/List")){
                    "list"
                }else if(findingObject["type"].toString().contains("Ljava/util/Map")){
                    "map"
                }else if(findingObject["type"].toString().contains("Ljava/lang/Object")){
                    "object"
                }else  if(findingObject["type"].toString().contains("Ljava/util/concurrent/BlockingQueue")){
                    "queue"
                }else{
                    findingObject["type"].toString()
                }

                fieldDetailLabel.text+="$className.$fieldName: $fieldType"
            }
        }

    }


    private fun resizeAccessesAndLocksTable2(){
        accessesAndLocksTable2.tableHeader.reorderingAllowed = false
        accessesAndLocksTable2.autoResizeMode = JBTable.AUTO_RESIZE_OFF
        accessesAndLocksTable2.rowSelectionAllowed = false


        accessesAndLocksTable2.columnModel.getColumn(0).resizable = false
        accessesAndLocksTable2.columnModel.getColumn(0).width = 20
        accessesAndLocksTable2.columnModel.getColumn(0).preferredWidth = 20
        accessesAndLocksTable2.columnModel.getColumn(0).maxWidth = 20


        accessesAndLocksTable2.columnModel.getColumn(1).resizable = false
        accessesAndLocksTable2.columnModel.getColumn(1).width = 50
        accessesAndLocksTable2.columnModel.getColumn(1).preferredWidth = 50
        accessesAndLocksTable2.columnModel.getColumn(1).maxWidth = 50

        accessesAndLocksTable2.columnModel.getColumn(2).resizable = false
        accessesAndLocksTable2.columnModel.getColumn(2).width = 50
        accessesAndLocksTable2.columnModel.getColumn(2).preferredWidth = 50
        accessesAndLocksTable2.columnModel.getColumn(2).maxWidth = 50
    }


    private fun accessAndLocksSelectionListener(): ListSelectionListener? {
        return ListSelectionListener {
            for(  idx in 0 until accessesAndLocksTable.rowCount){
                if(accessesAndLocksTable.isRowSelected(idx)){
                    val lineData = Vector<String>()
                    val typeData = Vector<String>()
                    val methodName = accessesAndLocksTable.getValueAt(idx,1)
                    val methodData = accessesAndLocksMethodData!![methodName]
//                    var table2RowIdx = 0
                    for(data in methodData!!){
                        lineData.add(data["line"])

                        if(data["type"]!!.lowercase().contains("read")){
                            typeData.add("Read")
                        }else{
                            typeData.add("Write")
                        }
                    }

                    val accessessAndLocksTableModel2 = object : DefaultTableModel(){
                        override fun isCellEditable(row: Int, column: Int): Boolean {
                            return false
                        }

                        override fun getColumnClass(column: Int): Class<out Any> {
                            return if (column == 0) {
                                Icon::class.javaObjectType
                            } else {
                                String::class.javaObjectType
                            }
                        }

                    }
                    accessessAndLocksTableModel2.addColumn("")
                    accessessAndLocksTableModel2.addColumn("line",lineData)
                    accessessAndLocksTableModel2.addColumn("type",typeData)

                    accessesAndLocksTable2.model = accessessAndLocksTableModel2
                    resizeAccessesAndLocksTable2()


                    var guardName = ""
                    for(button in radioButtonGroup.elements){
                        if(button.isSelected){
                            guardName = button.actionCommand
                            break
                        }
                    }

                    //Add Not Held Markers when line selected
                    addNotHeldMarkersToAccessesTable2(guardName)

                }
            }
        }
    }


    private fun addNotHeldMarkersToAccessesTable2(guardName:String){


        val errorName = errorsMap!![errorIdx]!!["name"]!!
        val lockNotHeldIcon = AllIcons.CodeWithMe.CwmTerminate

        //Read Entries in AccessesTable2
        for(rowIdx in 0 until accessesAndLocksTable2.rowCount){
            val line = accessesAndLocksTable2.getValueAt(rowIdx,1)
            if(findingsAccessAndLocksInfo!![errorName]!!.size==0){
                if( accessesAndLocksTable2.getValueAt(accessesAndLocksTable.selectedRow,0).toString() != ""){
                    accessesAndLocksTable2.setValueAt(lockNotHeldIcon,rowIdx,0)
                }else{
                    accessesAndLocksTable2.setValueAt("",rowIdx,0)
                }
            }else{
                var lineMatched = false

                for(accessesObject in findingsAccessAndLocksInfo!![errorName]!!){
                    if(accessesObject["line"]==line.toString()){
                        lineMatched = true
                        if(accessesObject["guardRefKey"] == "" || accessesObject["guardRefKey"] == null){
                            if(accessesObject["status"]!="always"){
                                accessesAndLocksTable2.setValueAt(lockNotHeldIcon,rowIdx,0)
                            }else{
                                accessesAndLocksTable2.setValueAt("",rowIdx,0)
                            }
                        }else{
                            var requiredGuardObject: java.util.HashMap<String, String>? = null
                            //Match GuardName
                            for(guardObject in guardsInfo!![errorName]!!){
                                if(guardObject["displayName"]==guardName){
                                    requiredGuardObject = guardObject.clone() as HashMap<String, String>
                                    break
                                }
                            }
                            if(requiredGuardObject!=null && requiredGuardObject["key"] == accessesObject["guardRefKey"]){
                                if(accessesObject["status"]!="always"){
                                    accessesAndLocksTable2.setValueAt(lockNotHeldIcon,rowIdx,0)
                                }else{
                                    accessesAndLocksTable2.setValueAt("",rowIdx,0)
                                }
                            }else if(requiredGuardObject!=null && requiredGuardObject["key"] != accessesObject["guardRefKey"]){
                                accessesAndLocksTable2.setValueAt(lockNotHeldIcon,rowIdx,0)
                            }
                        }
                    }
                }

                if(!lineMatched){
                    if( accessesAndLocksTable.getValueAt(accessesAndLocksTable.selectedRow,0) == null  || accessesAndLocksTable.getValueAt(accessesAndLocksTable.selectedRow,0).toString() != "" ){
                        accessesAndLocksTable2.setValueAt(lockNotHeldIcon,rowIdx,0)
                    }else{
                        accessesAndLocksTable2.setValueAt("",rowIdx,0)
                    }
                }
            }
        }
    }

    private fun radioButtonActionListener(): ActionListener {
        return ActionListener {
            println("Selected: "+it.actionCommand)
            val lockNotHeldIcon = AllIcons.CodeWithMe.CwmTerminate
            //Get Column Idx of selected radio button
            val colIdx = accessesAndLocksTable.columnModel.getColumnIndex(it.actionCommand)
            //Adding icons to Table1
            for(rowIdx in 0 until accessesAndLocksTable.rowCount){
                if(accessesAndLocksTable.getValueAt(rowIdx,colIdx) !="Always Held"){
                    accessesAndLocksTable.setValueAt(lockNotHeldIcon,rowIdx,0)
                }else{
                    accessesAndLocksTable.setValueAt("",rowIdx,0)
                }
            }
            //Read Entries in AccessesTable2
            addNotHeldMarkersToAccessesTable2(it.actionCommand)
        }
    }

    fun createAccessesAndLocksSection(): JPanel {

        val errorName = errorsMap!![errorIdx]!!["name"]!!

        val accessesAndLocksPanel = JPanel()
        accessesAndLocksPanel.layout = BoxLayout(accessesAndLocksPanel, BoxLayout.PAGE_AXIS)

        //TextBox
        val accessesAndLocksPanelTop = JPanel()
        accessesAndLocksPanelTop.layout = BorderLayout()
        accessesAndLocksPanelTop.preferredSize = Dimension(1500,40)
        accessesAndLocksPanelTop.add(fieldDetailLabel)

        val accessesAndLocksPanelBottom = JPanel()
        val accessesAndLocksPanelLayout = BoxLayout(accessesAndLocksPanelBottom, BoxLayout.LINE_AXIS)
        accessesAndLocksPanelBottom.layout = accessesAndLocksPanelLayout

        //For Table 1
        val scrollablePanel = JBScrollPane(accessesAndLocksTable)
        scrollablePanel.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollablePanel.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollablePanel.minimumSize = Dimension(320,600)
        scrollablePanel.preferredSize = Dimension(800,600)
        scrollablePanel.maximumSize = Dimension(1000,1200)

        //Adding findingsPanel to OuterPanel
        accessesAndLocksPanelBottom.add(scrollablePanel)

        //For Table 2
        val scrollablePanel2 = JBScrollPane(accessesAndLocksTable2)
        scrollablePanel2.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollablePanel2.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollablePanel2.maximumSize = Dimension(120,600)
        scrollablePanel2.minimumSize = Dimension(120,600)
        scrollablePanel2.preferredSize = Dimension(120,600)
        accessesAndLocksPanelBottom.add(scrollablePanel2)

        //Another Section for RadioButton List
        val radioButtonPane = JPanel()
        radioButtonPane.layout = BoxLayout(radioButtonPane, BoxLayout.Y_AXIS)


        //Get GuardNames
        val guardNames =Vector<String>()
        for(guardObject in  guardsInfo!![errorName]!!){
            guardNames.add(guardObject["displayName"])
        }


        val radioButtonPanel = createRadioButtons(guardNames)
        radioButtonPane.add(radioButtonPanel)

        radioButtonPane.maximumSize = Dimension(200,600)
        radioButtonPane.minimumSize = Dimension(120,600)
        radioButtonPane.preferredSize = Dimension(200,600)


        accessesAndLocksPanelBottom.add(radioButtonPane)



        //Add Top and Bottom Panels
        accessesAndLocksPanel.add(accessesAndLocksPanelTop)
        accessesAndLocksPanel.add(accessesAndLocksPanelBottom)


        return accessesAndLocksPanel


    }

    private fun createRadioButtons(guardsNames: Vector<String>): JPanel {

        val radioButtonPane = JPanel()
        radioButtonPane.layout = BoxLayout(radioButtonPane,BoxLayout.Y_AXIS)

        //Adding Guards Label
        val guardsListLabel = JLabel()
        guardsListLabel.text = " Guards List:"
        radioButtonPane.add(guardsListLabel)

        //Clear Existing Buttons in radioButton Group
        for(b in radioButtonGroup.elements){
            radioButtonGroup.remove(b)
        }
        radioButtonGroup.clearSelection()

        //Adding New Buttons to Group and Pane
        for(idx in 0 until guardsNames.size){
            var radioButtonVal = false
            if(idx==0){
                radioButtonVal=true
            }
            val button = JRadioButton(guardsNames[idx])
            button.actionCommand = guardsNames[idx]
            button.isSelected = radioButtonVal
            button.addActionListener(radioButtonActionListener())
            radioButtonGroup.add(button)
            radioButtonPane.add(button)
        }
        return radioButtonPane
    }
}