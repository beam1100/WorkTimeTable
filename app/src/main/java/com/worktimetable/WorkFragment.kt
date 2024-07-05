package com.worktimetable

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.worktimetable.databinding.FragmentWorkBinding

class WorkFragment : Fragment() {
    companion object { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var _vBinding: FragmentWorkBinding? = null
    private val vBinding get() = _vBinding!!
    private lateinit var mainActivity:MainActivity


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _vBinding = FragmentWorkBinding.inflate(inflater, container, false)
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
            val inflater = LayoutInflater.from(requireContext())
            val holderLayout = vBinding.workTypeLayout
            holderLayout.removeAllViews()

            mainActivity.helper.selectAll("WorkTable"){ workMapList->
                workMapList.forEach { workMap ->
                    val holder = inflater.inflate(R.layout.holder, null) as LinearLayout
                    holder.findViewById<ImageButton>(R.id.holderMoveItemDown).isGone = true
                    holder.findViewById<ImageButton>(R.id.holderMoveItemUp).isGone = true
                    mkHolder(workMapList, holderLayout, holder, workMap, "workName"){ clickedWorkMap->
                        setWorkDialog(
                            clickedWorkMap,
                            {id, workName, typeList, shiftList->
                                mainActivity.helper.updateByCondition(
                                    "WorkTable",
                                    hashMapOf("id" to id as Any),
                                    hashMapOf(
                                        "workName" to workName,
                                        "typeList" to typeList,
                                        "shiftList" to shiftList
                                    )
                                )
                                onViewCreated(view, savedInstanceState)
                            },
                            {
                                mainActivity.helper.deleteByCondition("WorkTable", hashMapOf("id" to clickedWorkMap["id"]))
                                onViewCreated(view, savedInstanceState)
                            }
                        )
                    }
                }
            }

            vBinding.mkWorkBtn.setOnClickListener {
                setWorkDialog(
                    null,
                    { _, workName, typeList, shiftList->
                        mainActivity.helper.insert("WorkTable", hashMapOf("workName" to workName, "typeList" to typeList, "shiftList" to shiftList))
                        onViewCreated(view, savedInstanceState)
                    },
                    {}
                )
            }

            vBinding.dropWorkTableBtn.setOnClickListener {
                mainActivity.helper.dropTable(("WorkTable"))
            }

            vBinding.workTestBtn.setOnClickListener {
                try{
                    mainActivity.helper.selectAll("WorkTable"){
                        Log.d("test", it.toString())
                    }
                }catch(err:Exception){
                    Log.d("test", err.toString())
                    Log.d("test", err.stackTraceToString())
                }
            }

        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private  fun mkHolder(
        data:ArrayList<HashMap<String, Any>>,
        holderLayout:LinearLayout,
        holder:LinearLayout,
        mapItem:HashMap<String,Any>,
        toPrintKey:String,
        longClickCallback: (HashMap<String,Any>) -> Unit){
        try{

                //홀더 근무이름 텍스트뷰
                holder.findViewById<TextView>(R.id.holderTV).apply{
                    text = mapItem[toPrintKey] as String
                    setOnLongClickListener {_ ->
                        longClickCallback(mapItem)
                        return@setOnLongClickListener true
                    }
                }

                //홀더: 근무이동(위로)
                holder.findViewById<ImageButton>(R.id.holderMoveItemUp).setOnClickListener {
                    val holderIndex = holderLayout.indexOfChild(holder)
                    if(holderIndex > 0 && holderLayout.childCount>=2){
                        holderLayout.removeView(holder)
                        holderLayout.addView(holder, holderIndex-1)
                    }
                    val mapIndex = data.indexOf(mapItem)
                    if(mapIndex > 0){
                        data[mapIndex] = data[mapIndex-1]
                        data[mapIndex-1] = mapItem
                    }
                }

                //홀더: 근무이동(아래로)
                holder.findViewById<ImageButton>(R.id.holderMoveItemDown).setOnClickListener {
                    val holderIndex = holderLayout.indexOfChild(holder)
                    if(holderIndex < holderLayout.childCount-1 && holderLayout.childCount>=2){
                        holderLayout.removeView(holder)
                        holderLayout.addView(holder, holderIndex+1)
                    }

                    val mapIndex = data.indexOf(mapItem)
                    if(mapIndex < data.size-1){
                        data[mapIndex] = data[mapIndex+1]
                        data[mapIndex+1] = mapItem
                    }
                }
                holderLayout.addView(holder)
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun setWorkDialog(
        clickedMap:HashMap<String, Any>?=null,
        updateMap: (id:Int?, workName:String, typeList:ArrayList<HashMap<String, Any>>, shiftList:ArrayList<HashMap<String, Any>>) -> Unit,
        deleteMap: () -> Unit){
        try{
            val selectedWorkMap = clickedMap?: hashMapOf(
                "id" to null,
                "workName" to "",
                "typeList" to arrayListOf<HashMap<String, Any>>(),
                "shiftList" to arrayListOf<HashMap<String, Any>>(),
            )
            val exTypeMapList = (selectedWorkMap["typeList"] as ArrayList<*>)
            val exShiftMapList = (selectedWorkMap["shiftList"] as ArrayList<*>)
            val copiedTypeMapList = ArrayList(exTypeMapList.map{deepCopy(it) as HashMap<String, Any>})
            var copiedShiftMapList = ArrayList(exShiftMapList.map{deepCopy(it) as HashMap<String, Any>})

            Dialog(requireContext()).apply {
                setContentView(R.layout.dialog_set_work)
                setDialogSize(this, 0.9f, 0.8f)

                if(clickedMap==null){
                    this.findViewById<Button>(R.id.deleteWorkBtn).isGone=true
                }

                // 근무이름 출력
                clickedMap?.get("workName")?.let{
                    this.findViewById<EditText>(R.id.inputWorkName).setText(it as String)
                }

                val typeHolderLayout = this.findViewById<LinearLayout>(R.id.workTypeLayout).apply { removeAllViews() }
                val shiftHolderLayout = this.findViewById<LinearLayout>(R.id.workShiftLayout).apply { removeAllViews() }

                //기존 근무 유형 홀더에 담아서 레이아웃에 넣기
                copiedTypeMapList.forEach { typeMap ->
                    val inflater = LayoutInflater.from(requireContext())
                    val holder = inflater.inflate(R.layout.holder, null) as LinearLayout

                    mkHolder(copiedTypeMapList, typeHolderLayout, holder, typeMap, "type"){ longClickedTypeMap ->
                        addOrUpdateTypeDialog(
                            longClickedTypeMap,
                            {
                                newTypeMap ->
                                    holder.findViewById<TextView>(R.id.holderTV).text = newTypeMap["type"] as String
                                    longClickedTypeMap["type"] = newTypeMap["type"] as String
                                    longClickedTypeMap["isPatrol"] = newTypeMap["isPatrol"] as Boolean
                                    longClickedTypeMap["isConcurrent"] = newTypeMap["isConcurrent"] as Boolean
                            },
                            {
                                copiedTypeMapList.remove(typeMap)
                                typeHolderLayout.removeView(holder)
                            }
                        )
                    }
                }

                //★★★★★ 근무 추가하기 버튼 클릭: 디버깅 검토하기 ★★★★★
                this.findViewById<ImageButton>(R.id.mkAddWorkDialogBtn).setOnClickListener { _ ->
                    //다이얼로그에서 새로운 근무유형 홀더에 담기
                    val inflater = LayoutInflater.from(requireContext())
                    val holder = inflater.inflate(R.layout.holder, null) as LinearLayout
                    addOrUpdateTypeDialog(
                        null,
                        {toAddTypeMap ->
                            copiedTypeMapList.add(toAddTypeMap)
                            mkHolder(copiedTypeMapList, typeHolderLayout, holder, toAddTypeMap, "type"){ longClickedTypeMap ->
                                addOrUpdateTypeDialog(
                                    longClickedTypeMap,
                                    { newTypeMap ->
                                        holder.findViewById<TextView>(R.id.holderTV).text = newTypeMap["type"] as String
                                        longClickedTypeMap["type"] = newTypeMap["type"] as String
                                        longClickedTypeMap["isPatrol"] = newTypeMap["isPatrol"] as Boolean
                                        longClickedTypeMap["isConcurrent"] = newTypeMap["isConcurrent"] as Boolean
                                    },
                                    {
                                        copiedTypeMapList.remove(longClickedTypeMap)
                                        typeHolderLayout.removeView(holder)
                                    }
                                )
                            }
                        },
                        {}
                    )
                }

                //기존 근무시간 홀더에 담아서 레이아웃에 넣기
                copiedShiftMapList.forEach { shiftMap ->
                    val inflater = LayoutInflater.from(requireContext())
                    val holder = inflater.inflate(R.layout.holder, null) as LinearLayout
                    holder.findViewById<ImageButton>(R.id.holderMoveItemUp).isGone=true
                    holder.findViewById<ImageButton>(R.id.holderMoveItemDown).isGone=true
                    mkHolder(copiedShiftMapList, shiftHolderLayout, holder, shiftMap, "shift"){ clickedShiftMap ->
                        updateShiftDialog(clickedShiftMap,
                            {fh, fm, th, tm ->
                                val fromTime = fh*60+fm
                                val toTime = th*60+tm
                                val newShift = "${minuetToTimeStr(fromTime)} ~ ${minuetToTimeStr(toTime)}"
                                clickedShiftMap["shift"] = newShift
                                clickedShiftMap["fromTime"] = fromTime
                                clickedShiftMap["toTime"] = toTime
                                clickedShiftMap["interval"] = if(toTime>fromTime){toTime - fromTime }else{24*60-fromTime + toTime}
                                holder.findViewById<TextView>(R.id.holderTV).text = newShift
                            },
                            {
                                copiedShiftMapList.remove(clickedShiftMap)
                                shiftHolderLayout.removeView(holder)
                            }
                        )
                    }
                }

                //근무시간 설정 버튼 클릭
                this.findViewById<ImageButton>(R.id.mkSetShiftDialogAtOnceBtn).setOnClickListener {
                    shiftHolderLayout.removeAllViews()
                    setShiftAtOnceDialog{ shiftMapList->
                        copiedShiftMapList = shiftMapList
                        shiftMapList.forEach {shiftMap->
                            val inflater = LayoutInflater.from(requireContext())
                            val holder = inflater.inflate(R.layout.holder, null) as LinearLayout
                            holder.findViewById<ImageButton>(R.id.holderMoveItemUp).isGone = true
                            holder.findViewById<ImageButton>(R.id.holderMoveItemDown).isGone = true
                            mkHolder(shiftMapList, shiftHolderLayout, holder, shiftMap, "shift"){ clickedShiftMap->
                                updateShiftDialog(clickedShiftMap,
                                    {fh, fm, th, tm ->
                                        val fromTime = fh*60+fm
                                        val toTime = th*60+tm
                                        val newShift = "${minuetToTimeStr(fromTime)} ~ ${minuetToTimeStr(toTime)}"
                                        clickedShiftMap["shift"] = newShift
                                        clickedShiftMap["fromTime"] = fromTime
                                        clickedShiftMap["toTime"] = toTime
                                        clickedShiftMap["interval"] = if(toTime>fromTime){toTime - fromTime }else{24*60-fromTime + toTime}
                                        holder.findViewById<TextView>(R.id.holderTV).text = newShift
                                    },
                                    {
                                        copiedShiftMapList.remove(clickedShiftMap)
                                        shiftHolderLayout.removeView(holder)
                                    }
                                )
                            }
                        }
                    }
                }

                //근무유형, 근무시간 설정
                this.findViewById<RadioGroup>(R.id.setWorkRadioGroup).setOnCheckedChangeListener { _, id ->
                    when(id){
                        this.findViewById<RadioButton>(R.id.setTypeRadio).id -> {
                            this.findViewById<LinearLayout>(R.id.setTypeLayout).isGone = false
                            this.findViewById<LinearLayout>(R.id.setShiftLayout).isGone = true
                        }
                        this.findViewById<RadioButton>(R.id.setTimeRadio).id -> {
                            this.findViewById<LinearLayout>(R.id.setTypeLayout).isGone = true
                            this.findViewById<LinearLayout>(R.id.setShiftLayout).isGone = false
                        }
                    }
                }

                //★★★★★ 저장버튼 ★★★★★★
                this.findViewById<Button>(R.id.saveWorkBtn).setOnClickListener {
                    updateMap(
                        selectedWorkMap["id"] as? Int,
                        findViewById<EditText>(R.id.inputWorkName).text.toString(),
                        copiedTypeMapList,
                        copiedShiftMapList)
                    dismiss()
                }

                //★★★★★ 삭제버튼 ★★★★★
                this.findViewById<Button>(R.id.deleteWorkBtn).setOnClickListener {
                    deleteMap()
                    dismiss()
                }

                // ★★★★★ 테스트 버튼 ★★★★★
                this.findViewById<Button>(R.id.workTestBtn2).setOnClickListener {
                    Log.d("test", """
                        copiedTypeMapList: $copiedTypeMapList
                        copiedShiftMapList : $copiedShiftMapList
                    """.trimIndent())
                }

                show()
            } // Dialog(requireContext()).apply End
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    } // private fun setWorkDialog() End



    @SuppressLint("MissingInflatedId")
    private fun setShiftAtOnceDialog(callback: (shiftMapList:ArrayList<HashMap<String, Any>>) -> Unit ) {
        try{
            Dialog(requireContext()).apply dialog@{
                setContentView(R.layout.dialog_set_shift_at_once)
                setDialogSize(this, 0.85f, null)

                val intervalMinuetEt = findViewById<TextInputEditText>(R.id.interverMinuetEt)
                val shiftNumEt = findViewById<TextInputEditText>(R.id.shiftNumEt)

                val fromHourEt = findViewById<TextInputEditText>(R.id.fromHourAtOnceEt).apply {
                    addTextChangedListener {et->
                        val number = et.toString().toIntOrNull()
                        val errCondition = number == null || number < 0 || number > 23
                        validateTimeForm(this@dialog.findViewById(R.id.startHourLayout), errCondition, "0~23")
                    }
                }

                val fromMinuetEt = findViewById<TextInputEditText>(R.id.fromMinuetAtOnceEt).apply {
                    addTextChangedListener { et ->
                        val number = et.toString().toIntOrNull()
                        val errCondition = number == null || number < 0 || number > 59
                        validateTimeForm(this@dialog.findViewById(R.id.startMinuetLayout), errCondition, "0~59")
                    }
                }

                findViewById<Button>(R.id.setTimeBtn).setOnClickListener {
                    val fromHour = fromHourEt.text.toString().toIntOrNull()
                    val fromMinuet = fromMinuetEt.text.toString().toIntOrNull()
                    val intervalMinuet = intervalMinuetEt.text.toString().toIntOrNull()
                    val shiftNum = shiftNumEt.text.toString().toIntOrNull()
                    if(fromHour!=null && fromMinuet!=null && intervalMinuet != null && shiftNum!=null){
                        val resultMapList = arrayListOf<HashMap<String, Any>>()
                        for(i:Int in 0..shiftNum){
                            if(i<shiftNum){
                                val fromTime = fromHour*60 + fromMinuet + i*intervalMinuet
                                val toTime = fromHour*60 + fromMinuet + (i+1)*intervalMinuet
                                resultMapList.add(
                                    hashMapOf(
                                        "shift" to "${minuetToTimeStr(fromTime)} ~ ${minuetToTimeStr(toTime)}",
                                        "fromTime" to fromTime,
                                        "toTime" to toTime,
                                        "interval" to intervalMinuet
                                    )
                                )
                            }
                        }
                        callback(resultMapList)
                        dismiss()
                    }else{
                        Toast.makeText(requireContext(), "다시 입력하세요", Toast.LENGTH_SHORT).show()
                    }
                }

                show()
            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun addOrUpdateTypeDialog(
        typeMap:HashMap<String, Any>?=null,
        addTypeCallback:(HashMap<String, Any>)->Unit,
        deleteTypeCallback: () -> Unit){
        try{
            Dialog(requireContext()).apply{
                setContentView(R.layout.dialog_add_type)

                findViewById<Button>(R.id.deleteWorkTypeBtn).isGone = (typeMap==null)

                val workNameEditText = this.findViewById<TextView>(R.id.toAddWorkName)
                val isWorkPatrolBox = this.findViewById<CheckBox>(R.id.isPatrolCheckBox)
                val isConcurrentBox = this.findViewById<CheckBox>(R.id.isConcurentCheckBox)

                workNameEditText.text = typeMap?.get("type")?.let{it as String} ?: ""
                isWorkPatrolBox.isChecked= typeMap?.get("isPatrol")?.let { it as Boolean } ?: false
                isConcurrentBox.isChecked= typeMap?.get("isConcurrent")?.let { it as Boolean } ?: false

                //저장버튼 클릭
                this.findViewById<Button>(R.id.saveTypeBtn).setOnClickListener {
                    val toSaveTypeMap = hashMapOf<String, Any>(
                        "type" to workNameEditText.text.toString(),
                        "isPatrol" to isWorkPatrolBox.isChecked,
                        "isConcurrent" to isConcurrentBox.isChecked
                    )
                    addTypeCallback(toSaveTypeMap)
                    this.dismiss()
                }

                //삭제버튼 클릭
                this.findViewById<Button>(R.id.deleteWorkTypeBtn).setOnClickListener {
                    deleteTypeCallback()
                    this.dismiss()
                }
                show()
            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun updateShiftDialog(
        clickedShiftMap:HashMap<String, Any>,
        addShift:(fromHour:Int, fromMinuet:Int, toHour:Int, toMinuet:Int) -> Unit,
        deleteShift:() -> Unit
    ){
        try{
            Dialog(requireContext()).apply dialog@{
                setContentView(R.layout.dialog_update_shift)
                setDialogSize(this, 0.9f, null)
                val periodSplit = splitPeriod(clickedShiftMap["shift"] as String)
                val updateFromHourEt = findViewById<TextInputEditText>(R.id.updateFromHourEt).apply {
                    setText(periodSplit[0])
                    this.addTextChangedListener{
                        val num:Int? = this.text.toString().toIntOrNull()
                        val errCondition = num == null ||  num>23 || num<0
                        validateTimeForm( this@dialog.findViewById<TextInputLayout>(R.id.updateFromHourLayout), errCondition,  "0~23 입렵바람")
                    }
                }
                val updateFromMinuetEt = findViewById<TextInputEditText>(R.id.updateFromMinuetEt).apply {
                    setText(periodSplit[1])
                    addTextChangedListener {
                        val num:Int? = this.text.toString().toIntOrNull()
                        val errCondition = num == null ||  num>59 || num<0
                        validateTimeForm( this@dialog.findViewById<TextInputLayout>(R.id.updateFromMinuetLayout), errCondition,  "0~59 입렵바람")
                    }
                }
                val updateToHourEt = findViewById<TextInputEditText>(R.id.updateToHourEt).apply {
                    setText(periodSplit[2])
                    addTextChangedListener {
                        val num:Int? = this.text.toString().toIntOrNull()
                        val errCondition = num == null ||  num>23 || num<0
                        validateTimeForm( this@dialog.findViewById<TextInputLayout>(R.id.updateToHourLayout), errCondition,  "0~23 입렵바람")
                    }
                }
                val updateToMinuetEt = findViewById<TextInputEditText>(R.id.updateToMinuetEt).apply {
                    setText(periodSplit[3])
                    addTextChangedListener {
                        val num:Int? = this.text.toString().toIntOrNull()
                        val errCondition = num == null ||  num>59 || num<0
                        validateTimeForm( this@dialog.findViewById<TextInputLayout>(R.id.updateToMinuetLayout), errCondition,  "0~59 입렵바람")
                    }
                }
                findViewById<Button>(R.id.updateShiftBtn).setOnClickListener {
                    val fromHour = updateFromHourEt.text.toString().toIntOrNull()
                    val fromMinuet = updateFromMinuetEt.text.toString().toIntOrNull()
                    val toHour = updateToHourEt.text.toString().toIntOrNull()
                    val toMinuet = updateToMinuetEt.text.toString().toIntOrNull()
                    if(fromHour != null && fromMinuet != null && toHour != null && toMinuet != null){
                        addShift(fromHour, fromMinuet, toHour, toMinuet)
                        dismiss()
                    }
                }

                findViewById<Button>(R.id.deleteShiftBtn).setOnClickListener {
                    deleteShift()
                    this.dismiss()
                }

                show()
            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }


    private fun getMapByCondition(
        mapList:ArrayList<HashMap<String, Any>,>,
        condition: HashMap<String, Any>
    ):HashMap<String, Any>?{
        for(map in mapList){
            for(key in condition.keys){
                if(map[key] != condition[key]){
                    continue
                }else{
                    return map
                }
            }
        }
        return null
    }

    private fun deepCopy(obj: Any): Any {
        return when (obj) {
            is String -> obj
            is Int -> obj
            is Double -> obj
            is Boolean -> obj
            is Map<*, *> -> obj
                .mapKeys { it.key}
                .mapValues {it.value?.let {deepCopy(it)}}
            is List<*> -> obj.map {
                it?.let{deepCopy(it)}
            }
            else -> obj
        }
    }

    private fun setDialogSize(dialog: Dialog, width:Float?, height:Float?){
        val layoutParams = dialog.window?.attributes
        val viewWidth = vBinding.workDialogLayout.width
        val viewHeight = vBinding.workDialogLayout.height
        width?.let{
            layoutParams?.width = (viewWidth * width).toInt()
        }
        height?.let{
            layoutParams?.height = (viewHeight * height).toInt()
        }
        dialog.window?.attributes = layoutParams
    }

    private fun minuetToTimeStr(minuet:Int):String{
        val divided24 = minuet%(24*60)
        val hour = String.format("%02d", (divided24 / 60))
        val minuet = String.format("%02d", divided24 % 60 )
        return "${hour}:${minuet}"
    }

    private fun splitPeriod(timeStr:String):List<String>{
        val whitespaceRemovied = timeStr.trim()
        val splitByTild = whitespaceRemovied.split("~")
        val fromTimeSplitByColon = splitByTild[0].split(":").map{ it.trim() }
        val toTimeSplitByColon = splitByTild[1].split(":").map{ it.trim() }
        return fromTimeSplitByColon + toTimeSplitByColon
    }

    private fun validateTimeForm(textInputLayout: TextInputLayout, errCondition:Boolean, errMessage:String){
        try{
            if(errCondition){
                textInputLayout.isErrorEnabled = true
                textInputLayout.error = errMessage
            }else{
                textInputLayout.isErrorEnabled = false
            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }


} // class WorkFragment : Fragment() End