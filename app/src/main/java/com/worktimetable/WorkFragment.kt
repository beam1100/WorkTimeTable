package com.worktimetable

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.provider.Settings.Global
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
import androidx.core.view.isGone
import com.worktimetable.databinding.FragmentWorkBinding
import java.io.Serializable


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
                hashMapOf("type" to "주간상황","isPatrol" to false, "isConcurrent" to false),
                hashMapOf("type" to "1구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "2구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "3구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "도보","isPatrol" to false, "isConcurrent" to false),
            ),
            "shiftList" to arrayListOf<HashMap<String, Any>>(
                hashMapOf("shift" to "07:00~07:30", "minuet" to 30),
                hashMapOf("shift" to "07:30~11:00", "minuet" to 210),
                hashMapOf("shift" to "11:00~14:00", "minuet" to 180),
                hashMapOf("shift" to "14:00~17:00", "minuet" to 180),
                hashMapOf("shift" to "17:00~19:30", "minuet" to 150),
            )
        ),
        hashMapOf(
            "workName" to "야간근무",
            "typeList" to arrayListOf<HashMap<String, Any>>(
                hashMapOf("type" to "야간상황","isPatrol" to false, "isConcurrent" to false),
                hashMapOf("type" to "1구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "2구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "3구역","isPatrol" to true, "isConcurrent" to false),
                hashMapOf("type" to "대기","isPatrol" to false, "isConcurrent" to false),
            ),
            "shiftList" to arrayListOf<HashMap<String, Any>>(
                hashMapOf("shift" to "19:30~20:00", "minuet" to 30),
                hashMapOf("shift" to "20:00~00:00", "minuet" to 240),
                hashMapOf("shift" to "00:00~03:30", "minuet" to 180),
                hashMapOf("shift" to "03:30~07:00", "minuet" to 210),
                hashMapOf("shift" to "07:00~07:30", "minuet" to 30),
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
            sampleData.forEach { workMap ->
                val holder = inflater.inflate(R.layout.holder_set_work, null) as LinearLayout
                mkHolder(sampleData, holderLayout, holder, hashMapOf("workName" to workMap["workName"] as String)){ clickedWorkMap->
                    showWorkDetailsDialog(clickedWorkMap)
                }
            }

            vBinding.mkWorkTypeBtn.setOnClickListener {
                showWorkDetailsDialog()
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



    private fun showWorkDetailsDialog(clickedMap:HashMap<String, Any>?=null){

        try{
            val selectedWorkMap = clickedMap?: hashMapOf(
                    "workName" to "",
                    "typeList" to arrayListOf<HashMap<String, Any>>(),
                    "shiftList" to arrayListOf<HashMap<String, Any>>(),
                )
            val exTypeMapList = (selectedWorkMap["typeList"] as ArrayList<*>)
            val copiedTypeMapList = ArrayList(exTypeMapList.map{deepCopy(it) as HashMap<String, Any>})
            val exShiftMapList = (selectedWorkMap["shiftList"] as ArrayList<*>)
            val copiedShiftMapList = ArrayList(exShiftMapList.map{deepCopy(it) as HashMap<String, Any>})


            Dialog(requireContext()).apply {
                setContentView(R.layout.dialog_set_work)
                val layoutParams = window?.attributes
                val viewWidth = vBinding.workDialogLayout.width
                val viewHeight = vBinding.workDialogLayout.height
                layoutParams?.width = (viewWidth * 0.95).toInt()
                layoutParams?.height = (viewHeight * 0.9).toInt()
                window?.attributes = layoutParams

                // 근무이름 출력
                clickedMap?.get("workName")?.let{
                    this.findViewById<EditText>(R.id.inputWorkName).setText(it as String)
                }

                val holderLayout = this.findViewById<LinearLayout>(R.id.workTypeLayout)
                holderLayout.removeAllViews()



                //기존 근무유형 홀더에 담기
                copiedTypeMapList.forEach { typeMap ->
                    val inflater = LayoutInflater.from(requireContext())
                    val holder = inflater.inflate(R.layout.holder_set_work, null) as LinearLayout
                    val existingType = typeMap["type"] as String

                    mkHolder(copiedTypeMapList, holderLayout, holder, hashMapOf("type" to existingType)){ clickedTypeMap ->
                        mkEditWorkDialog(copiedTypeMapList, clickedTypeMap, holderLayout, holder){
                            newType, newIsPatrol, newIsConcurrent ->
                            holder.findViewById<TextView>(R.id.holderWorkName).text = newType
                            clickedTypeMap["type"] = newType
                            clickedTypeMap["isPatrol"] = newIsPatrol
                            clickedTypeMap["isConcurrent"] = newIsConcurrent
                        }
                    }
                }


                //근무 추가하기 버튼 누르면 출력
                this.findViewById<ImageButton>(R.id.mkAddWorkDialogBtn).setOnClickListener { _ ->
                    //다이얼로그에서 새로운 근무유형 홀더에 담기
                    mkAddWorkDialog { addedType, addedIsPatrol, addedIsConcurrent ->
                        copiedTypeMapList.add(
                            hashMapOf("type" to addedType,"isPatrol" to addedIsPatrol, "isConcurrent" to addedIsConcurrent)
                        )
                        val inflater = LayoutInflater.from(requireContext())
                        val holder = inflater.inflate(R.layout.holder_set_work, null) as LinearLayout
                        mkHolder(copiedTypeMapList, holderLayout, holder, hashMapOf("type" to addedType)){ clickedTypeMap->
                            mkEditWorkDialog(copiedTypeMapList, clickedTypeMap, holderLayout, holder){
                                newType, newIsPatrol, newIsConcurrent ->
                                holder.findViewById<TextView>(R.id.holderWorkName).text = newType
                                clickedTypeMap["type"] = newType
                                clickedTypeMap["isPatrol"] = newIsPatrol
                                clickedTypeMap["isConcurrent"] =newIsConcurrent
                            }
                        }
                    } // mkAddWorkDialog End
                } // this.findViewById<Button>(R.id.mkAddWorkDialogBtn).setOnClickListener End


                //근무유형, 근무시간 설정
                this.findViewById<RadioGroup>(R.id.setWorkRadioGroup).setOnCheckedChangeListener { _, id ->
                    when(id){
                        this.findViewById<RadioButton>(R.id.setTypeRadio).id -> {
                            this.findViewById<LinearLayout>(R.id.setTypeLayout).isGone = false
                            this.findViewById<LinearLayout>(R.id.setTimeLayout).isGone = true
                        }
                        this.findViewById<RadioButton>(R.id.setTimeRadio).id -> {
                            this.findViewById<LinearLayout>(R.id.setTypeLayout).isGone = true
                            this.findViewById<LinearLayout>(R.id.setTimeLayout).isGone = false
                        }
                    }
                }

                //저장버튼
                this.findViewById<Button>(R.id.workSaveBtn).setOnClickListener {
                    val result = hashMapOf(
                        "workName" to this.findViewById<EditText>(R.id.inputWorkName).text.toString(),
                        "typeList" to copiedTypeMapList,
                        "shiftList" to copiedShiftMapList
                    )
                }

                show()
            } // Dialog(requireContext()).apply End
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    } // private fun showWorkDetailsDialog() End


    private fun mkHolder(
        data:ArrayList<HashMap<String, Any>>,
        holderLayout:LinearLayout,
        holder:LinearLayout,
        condition:HashMap<String,Any>,
        callback: (HashMap<String,Any>) -> Unit){
        try{
            getMapByCondition(data, condition)?.let{map ->
                //홀더 근무이름 텍스트뷰
                holder.findViewById<TextView>(R.id.holderWorkName).apply{
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


    private fun mkAddWorkDialog(callback:(String,Boolean, Boolean)->Unit){
        Dialog(requireContext()).apply{
            setContentView(R.layout.dialog_add_type)
            val workNameEditText = this.findViewById<TextView>(R.id.toAddWorkName)
            val isWorkPatrolBox = this.findViewById<CheckBox>(R.id.isPatrolCheckBox)
            val isConcurrentBox = this.findViewById<CheckBox>(R.id.isConcurentCheckBox)
            this.findViewById<Button>(R.id.deleteWorkTypeBtn).isGone = true
            this.findViewById<Button>(R.id.addWorkBtn).setOnClickListener {
                callback(workNameEditText.text.toString(), isWorkPatrolBox.isChecked, isConcurrentBox.isChecked)
                this.dismiss()
            }
            show()
        }
    }

    private fun mkEditWorkDialog(
        typeMapList:ArrayList<HashMap<String, Any>>,
        typeMap:HashMap<String, Any>,
        holderLayout:LinearLayout,
        holder:LinearLayout,
        callback:(String, Boolean, Boolean)->Unit){
        Dialog(requireContext()).apply{
            setContentView(R.layout.dialog_add_type)
            val workNameEditText = this.findViewById<TextView>(R.id.toAddWorkName)
            val isWorkPatrolBox = this.findViewById<CheckBox>(R.id.isPatrolCheckBox)
            val isConcurrentBox = this.findViewById<CheckBox>(R.id.isConcurentCheckBox)
            workNameEditText.text = typeMap["type"] as String
            isWorkPatrolBox.isChecked= typeMap["isPatrol"] as Boolean
            isConcurrentBox.isChecked= typeMap["isConcurrent"] as Boolean
            this.findViewById<Button>(R.id.addWorkBtn).setOnClickListener {
                callback(workNameEditText.text.toString(), isWorkPatrolBox.isChecked, isConcurrentBox.isChecked)
                this.dismiss()
            }
            this.findViewById<Button>(R.id.deleteWorkTypeBtn).setOnClickListener {
                typeMapList.remove(typeMap)
                holderLayout.removeView(holder)
                this.dismiss()
            }
            show()
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

} // class WorkFragment : Fragment() End
