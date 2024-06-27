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

    private val sampleData = arrayListOf<HashMap<String,Any>>(
        hashMapOf(
            "workName" to "주간근무",
            "typeList" to arrayListOf<HashMap<String, Any>>(
                hashMapOf("type" to "상황","isPatrol" to false, "isConcurrent" to false),
                hashMapOf("type" to "1구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "2구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "3구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "4구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "도보","isPatrol" to false, "isConcurrent" to false),
            ),
            "shiftList" to arrayListOf<HashMap<String, Any>>(
                hashMapOf("shift" to "07:00 ~ 07:30", "interval" to 30),
                hashMapOf("shift" to "07:30 ~ 11:00", "interval" to 210),
                hashMapOf("shift" to "11:00 ~ 14:00", "interval" to 180),
                hashMapOf("shift" to "14:00 ~ 17:00", "interval" to 180),
                hashMapOf("shift" to "17:00 ~ 19:30", "interval" to 150),
            )
        ),
        hashMapOf(
            "workName" to "야간근무",
            "typeList" to arrayListOf<HashMap<String, Any>>(
                hashMapOf("type" to "상황","isPatrol" to false, "isConcurrent" to false),
                hashMapOf("type" to "1구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "2구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "3구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "4구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "도보","isPatrol" to false, "isConcurrent" to false),
                hashMapOf("type" to "대기","isPatrol" to false, "isConcurrent" to false),
            ),
            "shiftList" to arrayListOf<HashMap<String, Any>>(
                hashMapOf("shift" to "19:30 ~ 20:00", "interval" to 30),
                hashMapOf("shift" to "20:00 ~ 00:00", "interval" to 240),
                hashMapOf("shift" to "00:00 ~ 03:30", "interval" to 180),
                hashMapOf("shift" to "03:30 ~ 07:00", "interval" to 210),
                hashMapOf("shift" to "07:00 ~ 07:30", "interval" to 30),
            )
        )
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
            sampleData.forEach { workMap ->
                val holder = inflater.inflate(R.layout.holder, null) as LinearLayout
                mkHolder(sampleData, holderLayout, holder, hashMapOf("workName" to workMap["workName"] as String)){ clickedWorkMap->
                    setWorkDialog(
                        clickedWorkMap,
                        {toUpdateMap->
                            sampleData[sampleData.indexOf(clickedWorkMap)] = toUpdateMap
                            onViewCreated(view, savedInstanceState)
                        },
                        {toRemoveMap->
                            sampleData.remove(toRemoveMap)
                            onViewCreated(view, savedInstanceState)
                        }
                    )
                }
            }

            vBinding.mkWorkTypeBtn.setOnClickListener {
                setWorkDialog(
                    null,
                    { toUpdateMap->
                        sampleData.add(toUpdateMap)
                        onViewCreated(view, savedInstanceState)
                    },{}
                )
            }

            vBinding.workTestBtn.setOnClickListener {
                sampleData.forEach {
                    Log.d("test", it.toString())
                }
            }

        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun mkHolder(
        data:ArrayList<HashMap<String, Any>>,
        holderLayout:LinearLayout,
        holder:LinearLayout,
        condition:HashMap<String,Any>,
        callback: (HashMap<String,Any>) -> Unit){
        try{
            getMapByCondition(data, condition)?.let{map ->
                //홀더 근무이름 텍스트뷰
                holder.findViewById<TextView>(R.id.holderTV).apply{
                    text = condition.entries.first().value as String
                    setOnLongClickListener {_ ->
                        callback(map)
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
                    val mapIndex = data.indexOf(map)
                    if(mapIndex > 0){
                        data[mapIndex] = data[mapIndex-1]
                        data[mapIndex-1] = map
                    }
                }

                //홀더: 근무이동(아래로)
                holder.findViewById<ImageButton>(R.id.holderMoveItemDown).setOnClickListener {
                    val holderIndex = holderLayout.indexOfChild(holder)
                    if(holderIndex < holderLayout.childCount-1 && holderLayout.childCount>=2){
                        holderLayout.removeView(holder)
                        holderLayout.addView(holder, holderIndex+1)
                    }

                    val mapIndex = data.indexOf(map)
                    if(mapIndex < data.size-1){
                        data[mapIndex] = data[mapIndex+1]
                        data[mapIndex+1] = map
                    }
                }
                holderLayout.addView(holder)
            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun setWorkDialog(
        clickedMap:HashMap<String, Any>?=null,
        updateMap: (HashMap<String, Any>) -> Unit,
        deleteMap: (HashMap<String, Any>) -> Unit){
        try{
            val selectedWorkMap = clickedMap?: hashMapOf(
                "workName" to "",
                "typeList" to arrayListOf<HashMap<String, Any>>(),
                "shiftList" to arrayListOf<HashMap<String, Any>>(),
            )
            val exTypeMapList = (selectedWorkMap["typeList"] as ArrayList<*>)
            val copiedTypeMapList = ArrayList(exTypeMapList.map{deepCopy(it) as HashMap<String, Any>})
            val exShiftMapList = (selectedWorkMap["shiftList"] as ArrayList<*>)
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

                val workHolderLayout = this.findViewById<LinearLayout>(R.id.workTypeLayout)
                val shiftHolderLayout = this.findViewById<LinearLayout>(R.id.workShiftLayout)

                workHolderLayout.removeAllViews()
                shiftHolderLayout.removeAllViews()

                //기존 근무 유형 홀더에 담아서 레이아웃에 넣기
                copiedTypeMapList.forEach { typeMap ->
                    val inflater = LayoutInflater.from(requireContext())
                    val holder = inflater.inflate(R.layout.holder, null) as LinearLayout
                    val exType = typeMap["type"] as String

                    mkHolder(copiedTypeMapList, workHolderLayout, holder, hashMapOf("type" to exType)){ clickedTypeMap ->
                        addOrUpdateTypeDialog(copiedTypeMapList, clickedTypeMap, workHolderLayout, holder){newType, newIsPatrol, newIsConcurrent ->
                            holder.findViewById<TextView>(R.id.holderTV).text = newType
                            clickedTypeMap["type"] = newType
                            clickedTypeMap["isPatrol"] = newIsPatrol
                            clickedTypeMap["isConcurrent"] = newIsConcurrent
                        }
                    }
                }

                //근무 추가하기 버튼 클릭
                this.findViewById<ImageButton>(R.id.mkAddWorkDialogBtn).setOnClickListener { _ ->
                    //다이얼로그에서 새로운 근무유형 홀더에 담기
                    addOrUpdateTypeDialog { addedType, addedIsPatrol, addedIsConcurrent ->
                        copiedTypeMapList.add(
                            hashMapOf("type" to addedType,"isPatrol" to addedIsPatrol, "isConcurrent" to addedIsConcurrent)
                        )
                        val inflater = LayoutInflater.from(requireContext())
                        val holder = inflater.inflate(R.layout.holder, null) as LinearLayout
                        mkHolder(copiedTypeMapList, workHolderLayout, holder, hashMapOf("type" to addedType)){ clickedTypeMap->
                            addOrUpdateTypeDialog(copiedTypeMapList, clickedTypeMap, workHolderLayout, holder){ newType, newIsPatrol, newIsConcurrent ->
                                holder.findViewById<TextView>(R.id.holderTV).text = newType
                                clickedTypeMap["type"] = newType
                                clickedTypeMap["isPatrol"] = newIsPatrol
                                clickedTypeMap["isConcurrent"] =newIsConcurrent
                            }
                        }
                    } // addOrUpdateTypeDialog End

                } // this.findViewById<Button>(R.id.mkAddWorkDialogBtn).setOnClickListener End

                //기존 근무시간 홀더에 담아서 레이아웃에 넣기
                copiedShiftMapList.forEach { shiftMap ->
                    val inflater = LayoutInflater.from(requireContext())
                    val holder = inflater.inflate(R.layout.holder, null) as LinearLayout
                    holder.findViewById<ImageButton>(R.id.holderMoveItemUp).isGone=true
                    holder.findViewById<ImageButton>(R.id.holderMoveItemDown).isGone=true
                    val exShift = shiftMap["shift"] as String
                    mkHolder(copiedShiftMapList, shiftHolderLayout, holder, hashMapOf("shift" to exShift)){ clickedShiftMap ->
                        updateShiftDialog(clickedShiftMap,
                            {fh, fm, th, tm ->
                                val newShift = "${minuetToTimeStr(fh*60+fm)} ~ ${minuetToTimeStr(th*60+tm)}"
                                clickedShiftMap["shift"] = newShift
                                clickedShiftMap["fromShift"] = fh*60+fm
                                clickedShiftMap["toShift"] = th*60+tm
                                clickedShiftMap["interval"] = th*60+tm-th*60+tm
                                holder.findViewById<TextView>(R.id.holderTV).text = newShift
                            },
                            {toDeleteMap->
                                shiftHolderLayout.removeView(holder)
                                copiedShiftMapList.remove(toDeleteMap)
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
                            mkHolder(shiftMapList, shiftHolderLayout, holder, hashMapOf("shift" to shiftMap["shift"] as String)){ clickedShiftMap->
                                updateShiftDialog(clickedShiftMap,
                                    {fh, fm, th, tm ->
                                        val newShift = "${minuetToTimeStr(fh*60+fm)} ~ ${minuetToTimeStr(th*60+tm)}"
                                        clickedShiftMap["shift"] = newShift
                                        clickedShiftMap["fromTime"] = fh*60+fm
                                        clickedShiftMap["toTime"] = th*60+tm
                                        clickedShiftMap["interval"] = th*60+tm-th*60+tm
                                        holder.findViewById<TextView>(R.id.holderTV).text = newShift
                                    },
                                    {toDeleteMap ->
                                        shiftHolderLayout.removeView(holder)
                                        copiedShiftMapList.remove(toDeleteMap)
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

                //저장버튼
                this.findViewById<Button>(R.id.saveWorkBtn).setOnClickListener {
                    val selectedMap = hashMapOf<String, Any>(
                        "workName" to this.findViewById<EditText>(R.id.inputWorkName).text.toString(),
                        "typeList" to copiedTypeMapList,
                        "shiftList" to copiedShiftMapList
                    )
                    updateMap(selectedMap)
                    dismiss()
                }

                //삭제버튼
                this.findViewById<Button>(R.id.deleteWorkBtn).setOnClickListener {
                    clickedMap?.let { it1 -> deleteMap(it1) }
                    dismiss()
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
        typeMapList:ArrayList<HashMap<String, Any>>?=null,
        typeMap:HashMap<String, Any>?=null,
        holderLayout:LinearLayout?=null,
        holder:LinearLayout?=null,
        callback:(String, Boolean, Boolean)->Unit){
        try{
            Dialog(requireContext()).apply{
                setContentView(R.layout.dialog_add_type)
                val workNameEditText = this.findViewById<TextView>(R.id.toAddWorkName)
                val isWorkPatrolBox = this.findViewById<CheckBox>(R.id.isPatrolCheckBox)
                val isConcurrentBox = this.findViewById<CheckBox>(R.id.isConcurentCheckBox)
                workNameEditText.text = typeMap?.get("type")?.let{it as String} ?: ""
                isWorkPatrolBox.isChecked= typeMap?.get("isPatrol")?.let { it as Boolean } ?: false
                isConcurrentBox.isChecked= typeMap?.get("isConcurrent")?.let { it as Boolean } ?: false
                this.findViewById<Button>(R.id.addWorkBtn).setOnClickListener {
                    callback(workNameEditText.text.toString(), isWorkPatrolBox.isChecked, isConcurrentBox.isChecked)
                    this.dismiss()
                }
                this.findViewById<Button>(R.id.deleteWorkTypeBtn).setOnClickListener {
                    typeMapList?.remove(typeMap)
                    holderLayout?.removeView(holder)
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
        deleteShift:(clickedMap:HashMap<String, Any>) -> Unit
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
                    deleteShift(clickedShiftMap)
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