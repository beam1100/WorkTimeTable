package com.worktimetable
import android.app.Dialog
import android.content.Context
//import android.os.Build.VERSION_CODES.R
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.worktimetable.databinding.FragmentTableBinding
import java.text.SimpleDateFormat
import java.util.Calendar


class TableFragment : Fragment() {

    companion object {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var _vBinding: FragmentTableBinding? = null
    private val vBinding get() = _vBinding!!
    private lateinit var mainActivity:MainActivity

    private var typeMapList = arrayListOf<HashMap<String,Any>>()
    private var shiftMapList = arrayListOf<HashMap<String,Any>>()
    private var logMapList = arrayListOf<HashMap<String,Any>>()
    private lateinit var mainMemberList:ArrayList<String>
    private var subMemberList = arrayListOf("지원1", "지원2", "지원3")

    private var calendar: Calendar = Calendar.getInstance()
    private val formatter = SimpleDateFormat("yyyy-MM-dd")

    private var selectMode:Boolean = false
    private val selectedBtnList = arrayListOf<AppCompatButton>()

    private val stack: ArrayDeque<ArrayList<HashMap<String, Any>>> = ArrayDeque()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _vBinding = FragmentTableBinding.inflate(inflater, container, false)
        return vBinding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is MainActivity){
            mainActivity = context
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try{
            super.onViewCreated(view, savedInstanceState)

            vBinding.dateTV.text = formatter.format(calendar.time)

            updateTableForNewDate(0)

            // 다음 날짜로 이동 버튼
            vBinding.nextDateBtn.setOnClickListener{
                updateTableForNewDate(1)
                switchSelectMode(false)
                stack.clear()
            }

            // 이전 날짜로 이동 버튼
            vBinding.beforeDateBtn.setOnClickListener{
                updateTableForNewDate(-1)
                switchSelectMode(false)
                stack.clear()
            }

            // 이전 근무로 이동 버튼
            vBinding.beforeLogBtn.setOnClickListener {
                val logDateList = mainActivity.helper.select("LogTable", toSortColumn = "logDate").map { it["logDate"] as String }
                val betweenList = logDateList.map { mainActivity.daysBetween(it, formatter.format(calendar.time)) }
                betweenList
                    .filter {  it > 0}
                    .minOrNull()
                    ?.let{
                        updateTableForNewDate(
                            mainActivity.daysBetween( formatter.format(calendar.time), logDateList[betweenList.indexOf(it)] )
                        )
                    }
                switchSelectMode(false)
                stack.clear()
            }

            // 다음 근무로 이동 버튼
            vBinding.nextLogBtn.setOnClickListener {
                val logDateList = mainActivity.helper.select("LogTable", toSortColumn = "logDate").map { it["logDate"] as String }
                val betweenList = logDateList.map { mainActivity.daysBetween(it, formatter.format(calendar.time)) }
                betweenList
                    .filter {  it < 0}
                    .maxOrNull()
                    ?.let{
                        updateTableForNewDate(
                            mainActivity.daysBetween( formatter.format(calendar.time), logDateList[betweenList.indexOf(it)] )
                        )
                    }
                switchSelectMode(false)
                stack.clear()
            }

            //스크롤 연동
            arrayOf(vBinding.subSV, vBinding.mainSV, vBinding.colSV, vBinding.rowSV).forEach {
                it.setOnScrollChangeListener { view, x, y, _, _ ->
                    when (view.id) {
                        R.id.subSV -> vBinding.colSV.scrollTo(x, y)
                        R.id.mainSV -> vBinding.rowSV.scrollTo(x, y)
                        R.id.colSV -> vBinding.subSV.scrollTo(x, y)
                        R.id.rowSV -> vBinding.mainSV.scrollTo(x, y)
                    }
                }
            }

            //일지 작성
            vBinding.mkWorkSelectDialogBtn.setOnClickListener {
                mkSelectWorkDialog{selectedWorkName->
                    val workMap = mainActivity.helper.select("WorkTable", where= hashMapOf("workName" to selectedWorkName)).first()
                    typeMapList = workMap["typeList"] as ArrayList<HashMap<String, Any>>
                    shiftMapList = workMap["shiftList"] as ArrayList<HashMap<String, Any>>
                    logMapList = mkNewLogMapList(typeMapList, shiftMapList)
                    mkTable()
                }
            }

            //일지 저장
            vBinding.saveLogBtn.setOnClickListener {
                val toSaveLogMapList = logMapList.map {map->
                    map.filterKeys {key->
                        key != "btn"
                    }
                }

                val isRecord = mainActivity.helper.select("LogTable", where=hashMapOf("logDate" to formatter.format(calendar.time)))

                if(isRecord.isEmpty()){
                    mainActivity.helper.insert(
                        "LogTable",
                        hashMapOf(
                            "logDate" to formatter.format(calendar.time),
                            "logMapList" to toSaveLogMapList,
                            "typeMapList" to typeMapList,
                            "shiftMapList" to shiftMapList,
                            "mainMemberList" to mainMemberList,
                            "subMemberList" to subMemberList
                        )
                    )
                }else{
                    mainActivity.helper.updateByCondition(
                        "LogTable",
                        where = hashMapOf("logDate" to formatter.format(calendar.time)),
                        updateMap = hashMapOf(
                            "logMapList" to toSaveLogMapList,
                            "typeMapList" to typeMapList,
                            "shiftMapList" to shiftMapList,
                            "mainMemberList" to mainMemberList,
                            "subMemberList" to subMemberList
                        )
                    )
                }
            }

            //일지 삭제
            vBinding.deleteLogBtn.setOnClickListener {
                mainActivity.helper.deleteByCondition("LogTable", hashMapOf("logDate" to formatter.format(calendar.time)))
                clearTable()
            }

            //사고자 설정
            vBinding.mkSwitchMemberDialogBtn.setOnClickListener {
                mkSwitchMemberDialog{ workingMemberList->
                    mainMemberList = workingMemberList
                    logMapList.forEach { map->
                        val memberList = map["member"] as ArrayList<String>
                        val btn = map["btn"] as AppCompatButton
                        memberList.removeIf { name -> name !in workingMemberList }
                        btn.text = memberList.joinToString("\n")
                    }
                }
            }

            //지원근무자 설정
            vBinding.mkSubMemberDialogBtn.setOnClickListener {
                mkSubMemberDialog{
                    subMemberList = it
                }
            }

            //선택취소 버튼
            vBinding.cancelSelectBtn.setOnClickListener {
                switchSelectMode(false)
            }

            // 선택 칸 삭제 버튼
            vBinding.removeCellBtn.setOnClickListener {
                stack.addLast(mainActivity.deepCopy(logMapList) as ArrayList<HashMap<String,Any>>)
                selectedBtnList.forEach {btn->
                    mainActivity.getMapByCondition(logMapList, hashMapOf("btn" to btn))?.let{ map->
                        map["member"] = arrayListOf<String>()
                        btn.text = ""
                    }
                }
                switchSelectMode(false)
            }

            // 우측으로 셀 복사 버튼
            vBinding.copyToNextBtn.setOnClickListener {
                copyToRight()
            }

            // 되돌리기 버튼
            vBinding.undoBtn.setOnClickListener {
                if(stack.isNotEmpty()){
                    logMapList = stack.removeLast()
                    mkTable()
                }else{
                    Toast.makeText(requireContext(), "이전 작업이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            //교체 버튼
            val exchangeMemberClickListener = View.OnClickListener {
                mkExchangeDialog{ name1, name2 ->
                    exchangeWorker(name1, name2)
                }
            }
            vBinding.mkExchangeDialogBtn.setOnClickListener(exchangeMemberClickListener)
            vBinding.mkExchangeAtSelectedDialogBtn.setOnClickListener(exchangeMemberClickListener)

            //근무표 요약버튼
            vBinding.mkSummaryDialogBtn.setOnClickListener {
                mkSummaryDialog{name->
                    switchSelectMode(false)
                    switchSelectMode(true)
                    logMapList.forEach {map->
                        val memberList = map["member"] as ArrayList<String>
                        if(name in memberList){
                            val btn = map["btn"] as AppCompatButton
                            selectBtn(btn)
                        }
                    }
                }
            }

            //테이블 드랍 버튼(임시)
            vBinding.dropLogTableBtn.setOnClickListener {
                mainActivity.helper.dropTable("LogTable")
            }

            //출력 테스트(임시)
            vBinding.printLogBtn.setOnClickListener {
                logMapList.forEach {
                    Log.d("test", it.toString())
                }
                typeMapList.forEach {
                    Log.d("test", it.toString())
                }
            }



        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun mkTable(){
        try{
            clearTable()

            // 메인 테이블
            val tableLayout = TableLayout(requireContext())
            for(typeMap in typeMapList){
                val tableRow = TableRow(requireContext())
                val type = typeMap["type"] as String
                val isConcurrent = typeMap["isConcurrent"] as Boolean

                for(shiftMap in shiftMapList){
                    val shift = shiftMap["shift"] as String
                    AppCompatButton(requireContext()).apply btn@{
                        tableRow.addView(this)
                        val logMap = mainActivity.getMapByCondition( logMapList, hashMapOf("type" to type, "shift" to shift))?.apply { set("btn", this@btn) }
                        text = (logMap?.get("member") as ArrayList<*>).joinToString("\n")

                        this.setOnClickListener {
                            val alreadySelected = logMap?.get("member") as List<String>
                            val otherTimeSelected = if(isConcurrent){
                                listOf()
                            }else{
                                getSameTimeMember(shift).filterNot { it in alreadySelected}
                            }
                            mkCheckMemberDialog( alreadySelected, otherTimeSelected ){ checkedMember->
                                stack.add(mainActivity.deepCopy(logMapList) as ArrayList<HashMap<String,Any>>)
                                if(selectMode){
                                    selectedBtnList.forEach { btn->
                                        btn.text = checkedMember.joinToString("\n")
                                        mainActivity.getMapByCondition(logMapList, hashMapOf("btn" to btn))?.let{map->
                                            map["member"] = checkedMember
                                        }
                                    }
                                    switchSelectMode(false)
                                }else{
                                    text = checkedMember.joinToString("\n")
                                    logMap["member"] = checkedMember
                                }
                            }
                        }

                        this.setOnLongClickListener (cellLongClickEvent())
                        this.setOnDragListener{ v, event -> cellDrag(v, event) }
                        setBtnStyle(this, R.color.unSelectedColor)
                    }
                }
                tableLayout.addView(tableRow)
            }
            vBinding.subSV.addView(tableLayout)

            // 행제목(근무 종류)
            val rowTL = TableLayout(requireContext())
            for(typeMap in typeMapList){
                val row = TableRow(requireContext())
                AppCompatButton(requireContext()).apply{
                    val type = typeMap["type"] as String
                    this.text = type
                    this.textSize = 20f
                    row.addView(this)
                    rowTL.addView(row)
                    this.setOnClickListener {  }
                    setBtnStyle(this, androidx.appcompat.R.color.material_grey_600)
                    setOnLongClickListener(selectSameType(type))
                }
            }
            vBinding.rowSV.addView(rowTL)

            //열제목(근무 시간)
            val colTL = TableLayout(requireContext())
            val colRow = TableRow(requireContext())
            for(shiftMap in shiftMapList){
                AppCompatButton(requireContext()).apply {
                    val shift = shiftMap["shift"] as String
                    this.text = shift.replace(" ~ ", "\n~\n")
                    this.textSize = 20f
                    colRow.addView(this)
                    this.setOnClickListener{}
                    setBtnStyle(this, androidx.appcompat.R.color.material_grey_600, height=250)
                    setOnLongClickListener(selectSameShift(shift))
                }
            }
            colTL.addView(colRow)
            vBinding.colSV.addView(colTL)
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    } // mkTable End


    private fun mkExchangeDialog(callback:(name1:String, name2:String)->Unit) {
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_exchange)
            mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 0.9f, null)
            show()
            val exchangeSpinner1 = findViewById<Spinner>(R.id.exchangeSpinner1).apply {
                adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.custom_spinner,
                    mainMemberList + subMemberList
                )
            }
            val exchangeSpinner2 = findViewById<Spinner>(R.id.exchangeSpinner2).apply {
                adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.custom_spinner,
                    mainMemberList + subMemberList
                )
            }
            findViewById<Button>(R.id.exchangeMemberBtn).setOnClickListener {
                callback(exchangeSpinner1.selectedItem.toString(), exchangeSpinner2.selectedItem.toString())
                dismiss()
            }
        }
    }

    private fun exchangeWorker(name1: String, name2: String) {
        stack.add(mainActivity.deepCopy(logMapList) as ArrayList<HashMap<String,Any>>)
        if(selectMode){
            for(btn in selectedBtnList){
                val selectedMap = mainActivity.getMapByCondition(logMapList, hashMapOf("btn" to btn))
                val workerList = selectedMap?.get("member") as ArrayList<String>
                for(index in 0 until workerList.size){
                    if(workerList[index]==name1){
                        workerList[index] = name2
                    }else if(workerList[index]==name2){
                        workerList[index]=name1
                    }
                }
            }
            for(btn in selectedBtnList){
                val selectedMap = mainActivity.getMapByCondition(logMapList, hashMapOf("btn" to btn))
                val workerList = selectedMap?.get("member") as ArrayList<*>
                btn.text = workerList.joinToString("\n")
            }
        }else{
            for(map in logMapList){
                val workerList = map["member"] as ArrayList<String>
                for(index in 0 until workerList.size){
                    if(workerList[index]==name1){
                        workerList[index] = name2
                    }else if(workerList[index]==name2){
                        workerList[index]=name1
                    }
                }
            }
            for (map in logMapList){
                val btn = map["btn"] as AppCompatButton
                val workerList = map["member"] as ArrayList<*>
                btn.text = workerList.joinToString("\n")
            }
        }
    }



    private fun updateTableForNewDate(num:Int){
        calendar.add(Calendar.DAY_OF_MONTH, num)
        vBinding.dateTV.text = formatter.format(calendar.time)
        val recorded = mainActivity.helper.select("LogTable", where = hashMapOf("logDate" to formatter.format(calendar.time)))
        if(recorded.isNotEmpty()){
            typeMapList = recorded[0]["typeMapList"] as ArrayList<HashMap<String, Any>>
            shiftMapList = recorded[0]["shiftMapList"] as ArrayList<HashMap<String, Any>>
            logMapList = recorded[0]["logMapList"] as ArrayList<HashMap<String, Any>>
            // ★★★★★★★★★★★★★★★★★★★★★★★ mainMemberList 순서 정렬하기 ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
            mainMemberList = recorded[0]["mainMemberList"] as ArrayList<String>
            subMemberList = recorded[0]["subMemberList"] as ArrayList<String>
            mkTable()
        }else{
            logMapList.clear()
            mainMemberList = ArrayList(mainActivity.helper.select("MemberTable", toSortColumn = "sortIndex")
                .map{it["memberName"] as String})
            subMemberList.clear()
            clearTable()
        }
    }


    private fun mkNewLogMapList(typeList: ArrayList<HashMap<String, Any>>, shiftList: ArrayList<HashMap<String, Any>>):ArrayList<HashMap<String,Any>> {
        val resultMapList = arrayListOf<HashMap<String,Any>>()
        typeList.forEach {typeMap->
            shiftList.forEach { shiftMap->
                resultMapList.add(
                    hashMapOf(
                        "type" to typeMap["type"] as String,
                        "shift" to shiftMap["shift"] as String,
                        "member" to arrayListOf<String>()
                    )
                )
            }
        }
        return resultMapList
    }

    private fun mkSelectWorkDialog(resType:(String)->Unit) {
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_select_work)
            mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 0.9f, null)
            val workSpinner = findViewById<Spinner>(R.id.selectWorkSpinner).apply {
                adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.custom_spinner,
                    mainActivity.helper.select("WorkTable").map { it["workName"] }
                )
            }
            findViewById<Button>(R.id.mkNewTableBtn).setOnClickListener{
                dismiss()
                resType(workSpinner.selectedItem.toString())
            }
            show()
        }
    }

    private fun clearTable(){
        listOf(vBinding.subSV, vBinding.rowSV, vBinding.colSV).forEach {
            it.removeAllViews()
        }
    }

    private fun selectSameType(type: String): View.OnLongClickListener? {
        return View.OnLongClickListener {
            switchSelectMode(true)
            logMapList.forEach {logMap->
                if(logMap["type"] == type){
                    val toInputBtn = logMap["btn"] as AppCompatButton
                    if(toInputBtn !in selectedBtnList){
                        selectedBtnList.add(toInputBtn)
                        setBtnStyle(toInputBtn, R.color.selectedColor)
                    }
                }
            }
            return@OnLongClickListener true
        }
    }

    private fun selectSameShift(shift:String): View.OnLongClickListener? {
        return View.OnLongClickListener {
            switchSelectMode(true)
            logMapList.forEach {logMap->
                if(logMap["shift"] == shift){
                    val toInputBtn = logMap["btn"] as AppCompatButton
                    if(toInputBtn !in selectedBtnList){
                        selectedBtnList.add(toInputBtn)
                        setBtnStyle(toInputBtn, R.color.selectedColor)
                    }
                }
            }
            return@OnLongClickListener true
        }
    }

    private fun cellLongClickEvent(): View.OnLongClickListener? {
        return View.OnLongClickListener {
            val dragShadowBuilder = View.DragShadowBuilder(it)
            it.startDragAndDrop(null, dragShadowBuilder, it, 0)
            return@OnLongClickListener true
        }
    }

    private fun cellDrag(v:View, event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DROP -> {
                val fromBtn = event.localState as AppCompatButton
                val toBtn = v as AppCompatButton
                if(fromBtn==toBtn){
                    selectBtn(fromBtn)
                }else{
                    stack.add(mainActivity.deepCopy(logMapList) as ArrayList<HashMap<String,Any>>)
                    val fromMap = mainActivity.getMapByCondition(logMapList, hashMapOf("btn" to fromBtn))
                    val toMap = mainActivity.getMapByCondition(logMapList, hashMapOf("btn" to toBtn))
                    val cloneMap = fromMap?.clone() as HashMap<*, *>
                    fromMap["member"] = toMap!!["member"] as ArrayList<*>
                    toMap["member"] = cloneMap["member"] as ArrayList<*>
                    val tempText = fromBtn.text
                    fromBtn.text = toBtn.text
                    toBtn.text = tempText

                    if(selectedBtnList.contains(fromBtn) && !selectedBtnList.contains(toBtn)){
                        selectedBtnList.remove(fromBtn)
                        selectedBtnList.add(toBtn)
                        setBtnStyle(fromBtn, R.color.unSelectedColor)
                        setBtnStyle(toBtn, R.color.selectedColor)
                    }else if(!selectedBtnList.contains(fromBtn) && selectedBtnList.contains(toBtn)){
                        selectedBtnList.remove(toBtn)
                        selectedBtnList.add(fromBtn)
                        setBtnStyle(fromBtn, R.color.selectedColor)
                        setBtnStyle(toBtn, R.color.unSelectedColor)
                    }
                }
                return true
            }
        }
        return true
    }

    private fun selectBtn(btn:AppCompatButton){
        if(!selectMode){
            switchSelectMode(true)
        }
        if(btn in selectedBtnList){
            selectedBtnList.remove(btn)
            setBtnStyle(btn, R.color.unSelectedColor)
            if(selectedBtnList.isEmpty()){
                switchSelectMode(false)
            }
        }else{
            selectedBtnList.add(btn)
            setBtnStyle(btn, R.color.selectedColor)
        }
    }

    private fun switchSelectMode(turnOn:Boolean){
        if(turnOn){
            selectMode = true
            vBinding.selectedBtnLayout.visibility=View.VISIBLE
            vBinding.unselectedBtnLayout.visibility=View.GONE
        }else{
            selectMode = false
            selectedBtnList.forEach { btn->
                setBtnStyle(btn, R.color.unSelectedColor)
            }
            selectedBtnList.clear()
            vBinding.selectedBtnLayout.visibility=View.GONE
            vBinding.unselectedBtnLayout.visibility=View.VISIBLE
        }
    }



    private fun getSameTimeMember(shift: String): List<String> {
        val resultSet = mutableSetOf<String>()
        val sameTimeMap = logMapList.filter {it["shift"] == shift}
        sameTimeMap.forEach {map->
            (map["member"] as ArrayList<String>).forEach {name->
                resultSet.add(name)
            }
        }
        return resultSet.toList()
    }


    private fun setBtnStyle(btn: AppCompatButton, backGroundColor:Int, width:Int=200, height:Int=200){
        btn.setBackgroundColor(ContextCompat.getColor(requireContext(), backGroundColor))
        val params = TableRow.LayoutParams(width, height)
        params.gravity = Gravity.NO_GRAVITY
        params.setMargins(3, 3, 3, 3)
        btn.layoutParams = params
        btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
    }



    private fun mkCheckMemberDialog(
        alreadySelected:List<String>,
        sameTimeSelected:List<String>,
        callbackChecked: (List<String>) -> Unit
    ) {
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_check_member)
            mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 0.9f, null)
            show()

            val mainMemberHolderLayout = findViewById<LinearLayout>(R.id.mainMemberLayout)
            val subMemberHolderLayout = findViewById<LinearLayout>(R.id.subMemberLayout)
            val inflater = LayoutInflater.from(requireContext())

            mainMemberList.onEach {memberName->
                val myCheckbox = inflater.inflate(R.layout.custom_checkbox, null) as CheckBox
                myCheckbox.text = memberName
                if(memberName in sameTimeSelected){
                    myCheckbox.isEnabled = false
                }
                if(memberName in alreadySelected){
                    myCheckbox.isChecked = true
                }
                mainMemberHolderLayout.addView(myCheckbox)
            }

            subMemberList.onEach {memberName->
                val myCheckbox = (inflater.inflate(R.layout.custom_checkbox, null) as CheckBox)
                myCheckbox.text = memberName
                if(memberName in sameTimeSelected){
                    myCheckbox.isEnabled = false
                }
                if(memberName in alreadySelected){
                    myCheckbox.isChecked = true
                }
                subMemberHolderLayout.addView(myCheckbox)
            }
            findViewById<Button>(R.id.checkMemberBtn).setOnClickListener {
                val checkedMainList = mainMemberHolderLayout.children
                    .filterIsInstance<CheckBox>()
                    .filter { it.isChecked }
                    .mapTo(ArrayList()){it.text.toString()}
                val checkedSubList = subMemberHolderLayout.children
                    .filterIsInstance<CheckBox>()
                    .filter{it.isChecked}
                    .mapTo(ArrayList()){it.text.toString()}
                callbackChecked(checkedMainList+checkedSubList)
                dismiss()
            }
        }
    }

    private fun mkSwitchMemberDialog(callback: (ArrayList<String>) -> Unit) {
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_switch_main)
            mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 0.9f, null)
            show()

            val switchMemberLayout = findViewById<LinearLayout>(R.id.switchMemberLayout)

            val inflater = LayoutInflater.from(requireContext())
            mainActivity.helper.select("MemberTable").map { it["memberName"] }.onEach {name->
                val mySwitch = inflater.inflate(R.layout.custom_switch, null) as SwitchCompat
                mySwitch.text = name as String
                mySwitch.isChecked = name in mainMemberList
                switchMemberLayout.addView(mySwitch)
            }

            findViewById<Button>(R.id.switchMemberBtn).setOnClickListener {
                val checkedMember = switchMemberLayout.children
                    .filterIsInstance<SwitchCompat>()
                    .filter { it.isChecked }
                    .mapTo(ArrayList()) { it.text.toString() }
                callback(checkedMember)
                dismiss()
            }
        }
    }

    private fun mkSubMemberDialog(callbackSubMember: (ArrayList<String>) -> Unit) {
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_set_sub)
            mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 0.9f, 0.5f)
            show()
            val subMemberLayout = findViewById<LinearLayout>(R.id.sumMemberLayout)
            val inflater = LayoutInflater.from(requireContext())

            subMemberList.forEach {name->
                val holder = inflater.inflate(R.layout.holder_added_sub, null) as LinearLayout
                holder.findViewById<TextView>(R.id.subNameTV).text = name
                subMemberLayout.addView(holder)
                holder.findViewById<ImageButton>(R.id.removeSubBtn).setOnClickListener {
                    subMemberLayout.removeView(holder)
                }
            }

            findViewById<ImageButton>(R.id.addSubMemberBtn).setOnClickListener {
                val subNameET = findViewById<EditText>(R.id.toAddSubNameTV)
                val holder = inflater.inflate(R.layout.holder_added_sub, null) as LinearLayout
                holder.findViewById<TextView>(R.id.subNameTV).text = subNameET.text.toString()
                subMemberLayout.addView(holder)
                holder.findViewById<ImageButton>(R.id.removeSubBtn).setOnClickListener {
                    subMemberLayout.removeView(holder)
                }
                subNameET.text.clear()
            }

            findViewById<Button>(R.id.setSubMemberBtn).setOnClickListener {
                val addedSubList = subMemberLayout.children
                    .filterIsInstance<LinearLayout>()
                    .mapTo(ArrayList()){
                        (it.findViewById(R.id.subNameTV) as TextView).text.toString()
                    }
                callbackSubMember(addedSubList)
                dismiss()
            }
        }
    }

    private fun copyToRight() {
        val toRemoveBtnSet = mutableSetOf<AppCompatButton>()
        val toAddBtnSet = mutableSetOf<AppCompatButton>()
        stack.add(mainActivity.deepCopy(logMapList) as ArrayList<HashMap<String,Any>>)
        selectedBtnList.forEach { btn ->
            mainActivity.getMapByCondition(logMapList, hashMapOf("btn" to btn))?.let{thisMap ->
                val thisBtn = thisMap["btn"] as AppCompatButton
                val thisWork = thisMap["type"] as String
                val thisShift = thisMap["shift"] as String
                val thisWorker = thisMap["member"] as ArrayList<String>
                val thisShiftIndex = shiftMapList.indexOf(mainActivity.getMapByCondition(shiftMapList, hashMapOf("shift" to thisShift)))
                val nextShiftIndex = if(thisShiftIndex != shiftMapList.size-1){thisShiftIndex + 1}else{null}
                nextShiftIndex?.let{nextShiftIndex->
                    val nextShift = shiftMapList[nextShiftIndex]["shift"] as String
                    mainActivity.getMapByCondition(logMapList, hashMapOf("shift" to nextShift, "type" to thisWork))?.let{nextMap ->
                        val nextBtn = nextMap["btn"] as AppCompatButton
                        val nextWorker = nextMap["member"] as ArrayList<String>
                        toRemoveBtnSet.add(thisBtn)
                        toAddBtnSet.add(nextBtn)
                        setBtnStyle(thisBtn, R.color.unSelectedColor)
                        setBtnStyle(nextBtn, R.color.selectedColor)
                        if(nextWorker.isEmpty()){
                            nextMap["member"] = thisWorker
                            nextBtn.text = thisWorker.joinToString("\n")
                        }
                    }
                }
            }
        }
        selectedBtnList.removeAll(toRemoveBtnSet)
        selectedBtnList.addAll(toAddBtnSet)
    }

    private fun mkSummaryDialog(callback: (String) -> Unit){
        try{
            Dialog(requireContext()).apply {
                setContentView(R.layout.dialog_summary)
                mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 0.9f, null)
                show()

                val inflater = LayoutInflater.from(requireContext())
                (mainMemberList+subMemberList).forEach {name->
                    val holder = inflater.inflate(R.layout.holder_summary, null) as LinearLayout
                    findViewById<LinearLayout>(R.id.summaryLayout).addView(holder)
                    val countPair = countNameInLog(name)
                    holder.findViewById<TextView>(R.id.summaryHolderNameTV).text = name
                    holder.findViewById<TextView>(R.id.summaryHolderNum1).text = countPair.first.toString()
                    holder.findViewById<TextView>(R.id.summaryHolderNum2).text = countPair.second.toString()
                    holder.findViewById<TextView>(R.id.summaryHolderSelectMemberBtn).setOnClickListener {
                        callback(name)
                        dismiss()
                    }
                }

            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun countNameInLog(name:String):Pair<Int,Int>{
        var numOfAll = 0
        var numOfPatrol = 0
        logMapList.forEach {map->
            val memberList = map["member"] as ArrayList<String>
            val type = map["type"] as String
            val isPatrol = mainActivity.getMapByCondition(typeMapList, hashMapOf("type" to type))?.get("isPatrol") as Boolean
            if(name in memberList){
                if(isPatrol){
                    numOfAll++
                    numOfPatrol++
                }else{
                    numOfAll++
                }
            }
        }
        return Pair(numOfAll, numOfPatrol)
    }


} // class TableFragment : Fragment() End