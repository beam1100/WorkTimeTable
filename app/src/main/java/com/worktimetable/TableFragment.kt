package com.worktimetable
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
//import android.os.Build.VERSION_CODES.R
import android.os.Bundle
import android.util.Log
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
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.worktimetable.databinding.FragmentTableBinding
import org.w3c.dom.Text
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

            // 다음 날로 이동 버튼 클릭 리스터
            vBinding.nextDateBtn.setOnClickListener{updateTableForNewDate(1)}

            // 전 날로 이동 버튼 클릭 리스터
            vBinding.beforeDateBtn.setOnClickListener{updateTableForNewDate(-1)}

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
                mkSwitchMemberDialog{
                    mainMemberList = it
                    logMapList.forEach { map->
                        val memberList = map["member"] as ArrayList<String>
                        val btn = map["btn"] as AppCompatButton
                        memberList.forEach {name ->
                            if(name !in it){
                                memberList.remove(name)
                            }
                        }
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

            //출력 테스트
            vBinding.printLogBtn.setOnClickListener {
                /*logMapList.forEach {
                    Log.d("test", it.toString())
                }*/
                /*mainActivity.helper.select("LogTable").forEach {map->
                    map.forEach { (key, value) ->
                        Log.d("test", "▣key: $key, ▣value: $value")
                    }
                    Log.d("test", "=".repeat(150))
                }*/
                Log.d("test", """
                    logMapList size : ${logMapList.size}
                    mainMemberList : $mainMemberList
                    subMemberList : $subMemberList
                    
                """.trimIndent())
            }

            //드랍 LogTable
            vBinding.dropLogTableBtn.setOnClickListener {
                mainActivity.helper.dropTable("LogTable")
            }

        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
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
                                text = checkedMember.joinToString("\n")
                                logMap["member"] = checkedMember
                            }
                        }
                        setBtnStyle(this, androidx.appcompat.R.color.material_grey_300, 200, 200)
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
                    this.text = typeMap["type"] as String
                    this.textSize = 20f
                    row.addView(this)
                    rowTL.addView(row)
                    this.setOnClickListener {  }
                    setBtnStyle(this, androidx.appcompat.R.color.material_grey_600, 200, 200)
                }
            }
            vBinding.rowSV.addView(rowTL)

            //열제목(근무 시간)
            val colTL = TableLayout(requireContext())
            val colRow = TableRow(requireContext())
            for(shiftMap in shiftMapList){
                AppCompatButton(requireContext()).apply {
                    this.text = (shiftMap["shift"] as String).replace(" ~ ", "\n~\n")
                    this.textSize = 20f
                    colRow.addView(this)
                    this.setOnClickListener{}
                    setBtnStyle(this, androidx.appcompat.R.color.material_grey_600, 200, 250)
                }
            }
            colTL.addView(colRow)
            vBinding.colSV.addView(colTL)
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    } // mkTable End

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


    private fun setBtnStyle(btn: AppCompatButton, backGroundColor:Int, width:Int, height:Int){
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
            /*mainActivity.helper.select("MemberTable", toSortColumn = "sortIndex").map { it["memberName"] }*/
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

    private fun mkSwitchMemberDialog(workingMemberList: (ArrayList<String>) -> Unit) {
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
                workingMemberList(checkedMember)
                dismiss()
            }
        }
    }

    private fun mkSubMemberDialog(callbackSubMember: (ArrayList<String>) -> Unit) {
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_set_sub)
            mainActivity.setDialogSize(this, vBinding.tableFragmentLayout, 1f, null)
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


} // class TableFragment : Fragment() End