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
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isGone
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

    private var logMapList = arrayListOf<HashMap<String,Any>>()
    private var typeMapList = arrayListOf<HashMap<String,Any>>()
    private var shiftMapList = arrayListOf<HashMap<String,Any>>()

    private lateinit var mainMemberList:ArrayList<String>
    private var subMemberList = arrayListOf<String>()

    private var workName = ""

    private var calendar: Calendar = Calendar.getInstance()
    private val formatter = SimpleDateFormat("yyyy-MM-dd")

    private var selectMode:Boolean = false
    private val selectedBtnList = arrayListOf<AppCompatButton>()

    private val stack: ArrayDeque<ArrayList<HashMap<String, Any>>> = ArrayDeque()


    override fun onDestroyView() {
        super.onDestroyView()
        _vBinding = null
    }

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
            Log.d("test", "onViewCreated 실행")

            vBinding.dateTV.apply {
                text = formatter.format(calendar.time)
                textSize = mainActivity.preferences.getInt("dateSize", 30).toFloat()
            }

            getLog(0)


            // 다음 날짜로 이동 버튼
            vBinding.nextDateBtn.setOnClickListener{
                if(isChanged()){
                    mainActivity.mkConfirmDialog(
                        "변경된 내용을 저장하시겠습니까?",
                        {proceedWithSaving(1)},
                        {proceedWithoutSaving(1)}
                    )
                }else{
                    proceedWithoutSaving(1)
                }
            }

            // 이전 날짜로 이동 버튼
            vBinding.beforeDateBtn.setOnClickListener{
                if(isChanged()){
                    mainActivity.mkConfirmDialog(
                        "변경된 내용을 저장하시겠습니까?",
                        {proceedWithSaving(-1)},
                        {proceedWithoutSaving(-1)}
                    )
                }else{
                    proceedWithoutSaving(-1)
                }
            }

            // 이전 근무로 이동 버튼
            vBinding.beforeLogBtn.setOnClickListener {
                val logDateList = mainActivity.helper.select("LogTable", toSortColumn = "logDate").map { it["logDate"] as String }
                val betweenList = logDateList.map { mainActivity.daysBetween(it, formatter.format(calendar.time)) }
                betweenList
                    .filter {  it > 0}
                    .minOrNull()
                    ?.let{toMoveNum->
                        if(isChanged()){
                            mainActivity.mkConfirmDialog(
                                "변경된 내용을 저장하시겠습니까?",
                                {proceedWithSaving(toMoveNum*-1)},
                                {proceedWithoutSaving(toMoveNum*-1)}
                            )
                        }else{
                            proceedWithoutSaving(toMoveNum*-1)
                        }
                    }
                switchSelectMode(false)
                stack.clear()
            }

            // 다음 근무로 이동 버튼
            vBinding.nextLogBtn.setOnClickListener {
                val logDateList = mainActivity.helper.select("LogTable", toSortColumn = "logDate").map { it["logDate"] as String }
                val betweenList = logDateList.map { mainActivity.daysBetween(it, formatter.format(calendar.time)) }
                betweenList
                    .filter {it < 0}
                    .maxOrNull()
                    ?.let{moveToNum->
                        if(isChanged()){
                            mainActivity.mkConfirmDialog(
                                "변경된 내용을 저장하시겠습니까?",
                                {proceedWithSaving(moveToNum*-1)},
                                {proceedWithoutSaving(moveToNum*-1)}
                            )
                        }else{
                            proceedWithoutSaving(moveToNum*-1)
                        }
                    }
                switchSelectMode(false)
                stack.clear()
            }

            //근무표 리스트 버튼
            vBinding.mkLogListDialogBtn.setOnClickListener {
                mkLogListDialog(
                    goCallback = {date->
                        if(isChanged()){
                            mainActivity.mkConfirmDialog(
                                "변경된 내용을 저장하시겠습니까?",
                                {
                                    saveLog()
                                    getLog(mainActivity.daysBetween(formatter.format(calendar.time), date))
                                },
                                {
                                    getLog(mainActivity.daysBetween(formatter.format(calendar.time), date))
                                }
                            )
                        }else{
                            getLog(mainActivity.daysBetween(formatter.format(calendar.time), date))
                        }
                    },
                    getCallback = {map->
                        typeMapList = map["typeMapList"] as ArrayList<HashMap<String, Any>>
                        shiftMapList = map["shiftMapList"] as ArrayList<HashMap<String, Any>>
                        logMapList = map["logMapList"] as ArrayList<HashMap<String, Any>>
                        mainMemberList = map["mainMemberList"] as ArrayList<String>
                        subMemberList = map["subMemberList"] as ArrayList<String>
                        workName = map["workName"] as String
                        mkTable()
                    }
                )

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

            //근무표 작성
            vBinding.mkWorkSelectDialogBtn.setOnClickListener {
                if(mainActivity.helper.select("WorkTable").isEmpty()){
                    mainActivity.replaceFragment(mainActivity.workFragment)
                    Toast.makeText(requireContext(), "근무를 생성하세요", Toast.LENGTH_SHORT).show()
                }else if(mainActivity.helper.select("MemberTable").isEmpty()){
                    mainActivity.replaceFragment(mainActivity.memberFragment)
                    Toast.makeText(requireContext(), "인원을 추가하세요", Toast.LENGTH_SHORT).show()
                }else{
                    mkSelectWorkDialog{selectedWorkName->
                        val workMap = mainActivity.helper.select("WorkTable", where= hashMapOf("workName" to selectedWorkName)).first()
                        typeMapList = workMap["typeList"] as ArrayList<HashMap<String, Any>>
                        shiftMapList = workMap["shiftList"] as ArrayList<HashMap<String, Any>>
                        logMapList = mkNewLogMapList(typeMapList, shiftMapList)
                        workName = selectedWorkName
                        mkTable()
                    }
                }
            }

            //근무표 저장
            vBinding.saveLogBtn.setOnClickListener {
                mainActivity.mkConfirmDialog(
                    "근무표를 저장하시겠습니까?",
                    {saveLog()},
                    {}
                )
            }

            //근무표 삭제
            vBinding.deleteLogBtn.setOnClickListener {
                if(logMapList.isNotEmpty()){
                    mainActivity.mkConfirmDialog(
                        "현재 근무표를 삭제하시겠습니까?",
                        {
                            mainActivity.helper.deleteByCondition(
                                "LogTable",
                                hashMapOf("logDate" to formatter.format(calendar.time))
                            )
                            logMapList.clear()
                            mainMemberList = ArrayList(mainActivity.helper.select("MemberTable", toSortColumn = "sortIndex").map{it["memberName"] as String})
                            subMemberList.clear()
                            clearTable()
                        },
                        {}
                    )
                }
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
                    var isFound = false
                    logMapList.forEach {map->
                        if(name in map["member"] as ArrayList<*>){
                            isFound = true
                            selectBtn(map["btn"] as AppCompatButton)
                        }
                    }
                    if(!isFound){
                        switchSelectMode(false)
                    }
                }
            }

            // 크기 설정 버튼
            vBinding.mkSetSizeDialogBtn.setOnClickListener {
                setSizeDialog()
            }

            //테이블 드랍 버튼(임시)
            vBinding.dropLogTableBtn.setOnClickListener {
                mainActivity.helper.dropTable("LogTable")
            }

            //출력 테스트(임시)
            vBinding.printLogBtn.setOnClickListener {
                Log.d("test", mainActivity.helper.select("SizeTable").toString())
            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun proceedWithSaving(num:Int) {
        saveLog()
        proceedWithoutSaving(num)
    }

    private fun proceedWithoutSaving(num:Int) {
        getLog(num)
        switchSelectMode(false)
        stack.clear()
    }

    fun saveLog(){
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
                    "subMemberList" to subMemberList,
                    "workName" to workName
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
                    "subMemberList" to subMemberList,
                    "workName" to workName
                )
            )
        }
    }

    private fun getLog(num:Int){
        try{
            calendar.add(Calendar.DAY_OF_MONTH, num)
            vBinding.dateTV.text = formatter.format(calendar.time)
            val recorded = mainActivity.helper.select("LogTable", where = hashMapOf("logDate" to formatter.format(calendar.time)))
            if(recorded.isEmpty()){
                mainMemberList = ArrayList(mainActivity.helper.select("MemberTable", toSortColumn = "sortIndex").map{it["memberName"] as String})
                logMapList.clear()
                subMemberList.clear()
                clearTable()
            }else{
                typeMapList = recorded[0]["typeMapList"] as ArrayList<HashMap<String, Any>>
                shiftMapList = recorded[0]["shiftMapList"] as ArrayList<HashMap<String, Any>>
                logMapList = recorded[0]["logMapList"] as ArrayList<HashMap<String, Any>>
                mainMemberList = recorded[0]["mainMemberList"] as ArrayList<String>
                subMemberList = recorded[0]["subMemberList"] as ArrayList<String>
                workName = recorded[0]["workName"] as String
                mkTable()
            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun mkTable(){
        try{
            clearTable()
            switchSelectMode(false)

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
                        textSize = mainActivity.preferences.getInt("myTextSize", 20).toFloat()
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
                        setBtnStyle(
                            this, R.color.unSelectedColor,
                            doesUpdateSize=true,
                            height = mainActivity.preferences.getInt("tableHeight", 250),
                            width = mainActivity.preferences.getInt("tableWidth", 200),
                            heightRate = typeMap["heightRate"] as Int
                        )
                    }
                }
                tableLayout.addView(tableRow)
            }
            vBinding.subSV.addView(tableLayout)

            // 행제목(근무 형태)
            val rowTL = TableLayout(requireContext())
            for(typeMap in typeMapList){
                val row = TableRow(requireContext())
                AppCompatButton(requireContext()).apply{
                    val type = typeMap["type"] as String
                    this.text = type
                    textSize = mainActivity.preferences.getInt("myTextSize", 20).toFloat()
                    row.addView(this)
                    rowTL.addView(row)
                    setBtnStyle(
                        this,
                        androidx.appcompat.R.color.material_grey_600,
                        doesUpdateSize=true,
                        height = mainActivity.preferences.getInt("tableHeight", 250),
                        width = mainActivity.preferences.getInt("tableWidth", 200),
                        heightRate = typeMap["heightRate"] as Int,
                        widthRate = mainActivity.preferences.getInt("typeWidth", 100)
                    )
                    setOnClickListener {setTypeHeightDialog(typeMap)}
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
                    textSize = mainActivity.preferences.getInt("myTextSize", 20).toFloat()
                    colRow.addView(this)
                    this.setOnClickListener{}
                    setBtnStyle(
                        this,
                        androidx.appcompat.R.color.material_grey_600,
                        doesUpdateSize=true,
                        height = mainActivity.preferences.getInt("tableHeight", 250),
                        width = mainActivity.preferences.getInt("tableWidth", 200)
                    )
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
                    mainActivity.helper.select("WorkTable", toSortColumn="sortIndex").map { it["workName"] }
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
                        setBtnStyle(
                            toInputBtn,
                            R.color.selectedColor,
                            height = mainActivity.preferences.getInt("tableHeight", 0),
                            width = mainActivity.preferences.getInt("tableWidth", 0)
                        )
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
                        setBtnStyle(toInputBtn,R.color.selectedColor)
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
                        setBtnStyle(fromBtn,R.color.unSelectedColor)
                        setBtnStyle(toBtn,R.color.selectedColor)
                    }else if(!selectedBtnList.contains(fromBtn) && selectedBtnList.contains(toBtn)){
                        selectedBtnList.remove(toBtn)
                        selectedBtnList.add(fromBtn)
                        setBtnStyle(fromBtn,R.color.selectedColor)
                        setBtnStyle(toBtn,R.color.unSelectedColor)
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
            setBtnStyle( btn, R.color.unSelectedColor )
            if(selectedBtnList.isEmpty()){
                switchSelectMode(false)
            }
        }else{
            selectedBtnList.add(btn)
            setBtnStyle(btn, R.color.selectedColor )
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
                setBtnStyle(btn,R.color.unSelectedColor)
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


    private fun setBtnStyle(
        btn: AppCompatButton,
        backGroundColor:Int,
        doesUpdateSize:Boolean = false,
        height:Int?=null,
        width:Int?=null,
        heightRate:Int?=null,
        widthRate:Int?=null){
            btn.setBackgroundColor(ContextCompat.getColor(requireContext(), backGroundColor))
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            if(doesUpdateSize){

                val widthToApply =
                if(width==null){
                    if(widthRate==null){
                        btn.width
                    }else{
                        btn.width * widthRate / 100
                    }
                } else{
                    if(widthRate==null){
                        width.coerceAtLeast(30)
                    }else{
                        width.coerceAtLeast(30) * widthRate / 100
                    }
                }

                val heightToApply = if(heightRate == null){
                    height ?: btn.height
                }else{
                    if(height == null){
                        btn.height * heightRate / 100
                    }else{
                        height.coerceAtLeast(30) * heightRate / 100
                    }
                }

                val params = TableRow.LayoutParams(widthToApply.toInt(), heightToApply.toInt())
                params.gravity = Gravity.NO_GRAVITY
                params.setMargins(3, 3, 3, 3)
                btn.layoutParams = params
            }
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
            val resultList = ArrayList(alreadySelected)

            mainMemberList.forEach {memberName->
                (inflater.inflate(R.layout.custom_checkbox, null) as CheckBox).apply {
                    text = memberName
                    isEnabled = memberName !in sameTimeSelected
                    isChecked = memberName in alreadySelected
                    setOnCheckedChangeListener { _, isChecked ->
                        if(isChecked){
                            resultList.add(memberName)
                        }else{
                            resultList.remove(memberName)
                        }
                    }
                    mainMemberHolderLayout.addView(this)
                }
            }

            subMemberList.forEach {memberName->
                (inflater.inflate(R.layout.custom_checkbox, null) as CheckBox).apply {
                    text = memberName
                    isChecked = memberName in alreadySelected
                    isEnabled = memberName !in sameTimeSelected
                    setOnCheckedChangeListener { _, isChecked ->
                        if(isChecked){
                            resultList.add(memberName)
                        }else{
                            resultList.remove(memberName)
                        }
                    }
                    subMemberHolderLayout.addView(this)
                }
            }

            findViewById<Button>(R.id.checkMemberBtn).setOnClickListener {
                callbackChecked(resultList)
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
            mainActivity.helper.select("MemberTable").map { it["memberName"] }.forEach {name->
                (inflater.inflate(R.layout.custom_switch, null) as SwitchCompat).apply {
                    text = name as String
                    isChecked = name in mainMemberList
                    switchMemberLayout.addView(this)
                }
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
            mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 0.9f, null)
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
                        setBtnStyle(nextBtn,R.color.selectedColor)
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

    private fun setTypeHeightDialog(typeMap:HashMap<String,Any>){
        try{
            Dialog(requireContext()).apply {
                setContentView(R.layout.dialog_one_seekbar)
                mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 0.9f, null)
                show()

                val type = typeMap["type"] as String
                var toUpdateHeightRate = 100

                findViewById<TextView>(R.id.oneSeekbarTV).apply {
                    val title = "$type 높이"
                    text = title
                }

                findViewById<SeekBar>(R.id.oneSeekbar).apply {
                    progress =  (typeMap["heightRate"] as Int)/10
                    setOnSeekBarChangeListener( object :OnSeekBarChangeListener{
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                            Log.d("test", typeMap["heightRate"].toString())
                            typeMap["heightRate"]  = ((p1+1)*10)
                            toUpdateHeightRate = (p1+1)*10
                            mkTable()
                        }
                        override fun onStartTrackingTouch(p0: SeekBar?) {}
                        override fun onStopTrackingTouch(p0: SeekBar?) {}
                    })
                }
                setOnDismissListener {
                    mainActivity.helper.select("WorkTable").forEach {map->
                        val id = map["id"] as Int
                        val newTypeList = (map["typeList"] as ArrayList<HashMap<String,Any>>).onEach { selectedTypeMap->
                            if(selectedTypeMap["type"] == type){
                                selectedTypeMap["heightRate"] = toUpdateHeightRate
                            }
                        }
                        mainActivity.helper.updateByCondition(
                            "WorkTable",
                            hashMapOf("id" to id),
                            hashMapOf("typeList" to newTypeList)
                        )
                    }
                }
            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun setSizeDialog(){
        try{
            Dialog(requireContext()).apply {
                setContentView(R.layout.dialog_set_size)
                mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 0.9f, null)
                show()
                val editor = mainActivity.preferences.edit()

                val widthSeekBar = findViewById<SeekBar>(R.id.widthSeekBar).apply {
                    progress = mainActivity.preferences.getInt("tableWidth", 200) / 3
                    setOnSeekBarChangeListener( object :OnSeekBarChangeListener{
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                            editor.putInt("tableWidth", p1*3)
							editor.apply()
                            mkTable()
                        }
                        override fun onStartTrackingTouch(p0: SeekBar?) {}
                        override fun onStopTrackingTouch(p0: SeekBar?) {}
                    })
                }

                val heightSeekBar = findViewById<SeekBar>(R.id.heightSeekBar).apply {
                    progress = mainActivity.preferences.getInt("tableHeight", 200) / 3
                    setOnSeekBarChangeListener( object :OnSeekBarChangeListener{
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                            editor.putInt("tableHeight", p1*3)
							editor.apply()
                            mkTable()
                        }
                        override fun onStartTrackingTouch(p0: SeekBar?) {}
                        override fun onStopTrackingTouch(p0: SeekBar?) {}
                    })
                }

                val typeWidthSeekBar = findViewById<SeekBar>(R.id.workTypeWidthSeekbar).apply {
                    progress = mainActivity.preferences.getInt("typeWidth", 15)/10
                    setOnSeekBarChangeListener( object :OnSeekBarChangeListener{
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                            editor.putInt("typeWidth", (p1+1)*10)
                            editor.apply()
                            mkTable()
                        }
                        override fun onStartTrackingTouch(p0: SeekBar?) {}
                        override fun onStopTrackingTouch(p0: SeekBar?) {}
                    })
                }

                val textSizeSeekBar = findViewById<SeekBar>(R.id.textSizeSeekBar).apply {
                    progress = mainActivity.preferences.getInt("myTextSize", 10) * 5
                    setOnSeekBarChangeListener( object :OnSeekBarChangeListener{
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                            editor.putInt("myTextSize", p1/5)
                            editor.apply()
                            mkTable()
                        }
                        override fun onStartTrackingTouch(p0: SeekBar?) {}
                        override fun onStopTrackingTouch(p0: SeekBar?) {}
                    })
                }

                val dateSizeSeekBar = findViewById<SeekBar>(R.id.dateSizeSeekBar).apply {
                    progress = mainActivity.preferences.getInt("dateSize", 20)
                    setOnSeekBarChangeListener( object:OnSeekBarChangeListener{
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                            editor.putInt("dateSize", p1)
                            editor.apply()
                            vBinding.dateTV.textSize = p1.toFloat()
                        }
                        override fun onStartTrackingTouch(p0: SeekBar?) {}
                        override fun onStopTrackingTouch(p0: SeekBar?) {}
                    })
                }

                findViewById<Button>(R.id.mkSaveSizeDialogBtn).setOnClickListener {
                    mainActivity.mkInputTextDialog("저장할 레이아웃 이름") {
                        mainActivity.helper.insert(
                            "SizeTable",
                            hashMapOf(
                                "sizeName" to it,
                                "tableWidth" to widthSeekBar.progress,
                                "tableHeight" to heightSeekBar.progress,
                                "typeWidth" to typeWidthSeekBar.progress,
                                "tableTextSize" to textSizeSeekBar.progress,
                                "dateTextSize" to dateSizeSeekBar.progress
                            )
                        )
                    }
                }

                findViewById<Button>(R.id.mkGetSizeDialogBtn).setOnClickListener {
                    val selectedSizeMapList = mainActivity.helper.select("SizeTable")
                    if(selectedSizeMapList.isNotEmpty()){
                        Dialog(requireContext()).apply {
                            setContentView(R.layout.dialog_holder_layout)
                            mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 0.9f, null)
                            show()
                            val holderLayout = findViewById<LinearLayout>(R.id.holderLayout)
                            val inflater = LayoutInflater.from(requireContext())
                            selectedSizeMapList.forEach { mapItem->
                                val holder = inflater.inflate(R.layout.holder_item, null) as LinearLayout
                                holder.findViewById<ImageButton>(R.id.holderMoveItemUp).isGone = true
                                holder.findViewById<ImageButton>(R.id.holderMoveItemDown).isGone = true
                                holder.findViewById<ImageButton>(R.id.holderUpdateBtn).isGone = true
                                mainActivity.mkHolderFromMap(
                                    selectedSizeMapList , holderLayout, holder, mapItem, "sizeName",
                                    updateBtnCallback = {},
                                    getBtnCallback = {map->
                                        widthSeekBar.progress = map["tableWidth"] as Int
                                        heightSeekBar.progress = map["tableHeight"] as Int
                                        typeWidthSeekBar.progress = map["typeWidth"] as Int
                                        textSizeSeekBar.progress = map["tableTextSize"] as Int
                                        dateSizeSeekBar.progress = map["dateTextSize"] as Int
                                        dismiss()
                                        Log.d("test", map.toString())
                                    },
                                    delBtnCallback = {map->
                                        mainActivity.helper.deleteByCondition("SizeTable", hashMapOf("id" to map["id"]))
                                        holderLayout.removeView(holder)
                                    }
                                )
                            }
                        }
                    }else{
                        Toast.makeText(requireContext(), "저장된 크기설정이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun mkLogListDialog(
        goCallback: (String) -> Unit,
        getCallback: (HashMap<String,Any>) -> Unit
    ){
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_log_list)
            mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 1f, null)
            show()
            val inflater = LayoutInflater.from(requireContext())
            mainActivity.helper.select("LogTable", toSortColumn = "logDate").forEach {logMap->
                val holder = inflater.inflate(R.layout.holder_log, null) as LinearLayout
                findViewById<LinearLayout>(R.id.logLayout).addView(holder)
                holder.findViewById<TextView>(R.id.workNameTV).text = logMap["workName"] as String
                holder.findViewById<TextView>(R.id.numOfMainTV).text = (logMap["mainMemberList"] as ArrayList<*>).size.toString()
                holder.findViewById<TextView>(R.id.numOfSubTV).text = (logMap["subMemberList"] as ArrayList<*>).size.toString()
                holder.findViewById<Button>(R.id.logDateBtn).apply {
                    text = logMap["logDate"] as String
                    setOnClickListener {
                        goCallback(logMap["logDate"] as String)
                        dismiss()
                    }
                }
                holder.findViewById<Button>(R.id.getLogBtn).setOnClickListener {
                    getCallback(logMap)
                    dismiss()
                }
            }
        }
    }

    fun isChanged():Boolean{
        try{
            val recorded = mainActivity.helper.select("LogTable", where = hashMapOf("logDate" to formatter.format(calendar.time)))
            val btnRemovedLogMapList = logMapList.map {map->map.filterKeys {key->key != "btn"}}
            return if(recorded.isEmpty()){
                btnRemovedLogMapList.isNotEmpty()
            }else{
                val recordedTypeMapList = recorded[0]["typeMapList"] as ArrayList<HashMap<String, Any>>
                val recordedLogMapList = recorded[0]["logMapList"] as ArrayList<HashMap<String, Any>>
                val recordedMainMemberList = recorded[0]["mainMemberList"] as ArrayList<String>
                val recordedSubMemberList = recorded[0]["subMemberList"] as ArrayList<String>
                recordedLogMapList!=btnRemovedLogMapList ||
                        recordedTypeMapList != typeMapList ||
                        recordedMainMemberList != mainMemberList ||
                        recordedSubMemberList != subMemberList
            }
        }catch (err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
        return false
    }




} // class TableFragment : Fragment() End