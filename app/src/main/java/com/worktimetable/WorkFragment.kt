package com.worktimetable

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
import android.widget.TextView
import android.widget.Toast
import com.worktimetable.databinding.FragmentWorkBinding



class WorkFragment : Fragment() {


    companion object { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var _vBinding: FragmentWorkBinding? = null
    private val vBinding get() = _vBinding!!
    private lateinit var mainActivity:MainActivity

    private val workMapList = arrayListOf<HashMap<String,Any>>()
    private val timeMapList = arrayListOf<HashMap<String,Any>>()


    private val sampleData = arrayListOf<HashMap<String,Any>>(
        hashMapOf(
            "workName" to "주간근무",
            "typeList" to arrayListOf<HashMap<String, Any>>(
                hashMapOf("type" to "주간상황","isPatrol" to false),
                hashMapOf("type" to "1구역","isPatrol" to true),
                hashMapOf("type" to "2구역","isPatrol" to true),
                hashMapOf("type" to "3구역","isPatrol" to true),
                hashMapOf("type" to "도보","isPatrol" to false),
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
                hashMapOf("type" to "야간상황","isPatrol" to false),
                hashMapOf("type" to "1구역","isPatrol" to true),
                hashMapOf("type" to "2구역","isPatrol" to true),
                hashMapOf("type" to "3구역","isPatrol" to true),
                hashMapOf("type" to "대기","isPatrol" to false),
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

            val test = clickedMap?.let{
                deepCopyMap(it)
            }



            val selectedWorkMap = clickedMap
                ?: hashMapOf(
                    "workName" to "",
                    "typeList" to arrayListOf<HashMap<String, Any>>(),
                    "shiftList" to arrayListOf<HashMap<String, Any>>(),
                )

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

                val selectedTypeList = selectedWorkMap["typeList"] as ArrayList<HashMap<String,Any>>

                //기존 근무유형 홀더에 담기
                selectedTypeList.forEach { typeMap->
                    val inflater = LayoutInflater.from(requireContext())
                    val holder = inflater.inflate(R.layout.holder_set_work, null) as LinearLayout
                    val existingType = typeMap["type"] as String
                    val existingIsPatrol = typeMap["isPatrol"] as Boolean

                    mkHolder(selectedTypeList, holderLayout, holder, hashMapOf("type" to existingType)){ clickedTypeMap ->
                        mkEditWorkDialog(clickedTypeMap["type"] as String, existingIsPatrol){ newType, newIsPatrol ->
                            holder.findViewById<TextView>(R.id.holderWorkName).text = newType
                            clickedTypeMap["type"] = newType
                            clickedTypeMap["isPatrol"] = newIsPatrol
                        }
                    }
                }


                //근무 추가하기 버튼 누르면 출력
                this.findViewById<ImageButton>(R.id.mkAddWorkDialogBtn).setOnClickListener { _ ->
                    //다이얼로그에서 새로운 근무유형 홀더에 담기
                    mkAddWorkDialog { addedType, addedIsPatrol ->
                        selectedTypeList.add(
                            hashMapOf("type" to addedType,"isPatrol" to addedIsPatrol)
                        )
                        val inflater = LayoutInflater.from(requireContext())
                        val holder = inflater.inflate(R.layout.holder_set_work, null) as LinearLayout
                        mkHolder(selectedTypeList, holderLayout, holder, hashMapOf("type" to addedType)){ clickedTypeMap->
                            mkEditWorkDialog(clickedTypeMap["type"] as String, clickedTypeMap["isPatrol"] as Boolean){ newType, newIsPatrol ->
                                holder.findViewById<TextView>(R.id.holderWorkName).text = newType
                                clickedTypeMap["type"] = newType
                                clickedTypeMap["isPatrol"] = newIsPatrol
                            }
                        }
                    } // mkAddWorkDialog End
                } // this.findViewById<Button>(R.id.mkAddWorkDialogBtn).setOnClickListener End


                // 저장 버튼
                this.findViewById<Button>(R.id.saveWorkBtn).setOnClickListener {_->
                    val newWorkName = this.findViewById<EditText>(R.id.inputWorkName).text.toString()
                    if(newWorkName.isEmpty()){
                        Toast.makeText(requireContext(), "근무이름을 입력하세요", Toast.LENGTH_LONG).show()
                        Log.d("test", selectedWorkMap.toString())
                    }else{
                        Toast.makeText(requireContext(), "test", Toast.LENGTH_LONG).show()
                    }
                    Log.d("test", selectedWorkMap.toString())
                }

                //테스트 버튼
                this.findViewById<Button>(R.id.test2Btn).setOnClickListener {
                    Log.d("test", clickedMap.toString())
                    Log.d("test", test.toString())
                }

                show()
            } // Dialog(requireContext()).apply End
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    } // private fun showWorkDetailsDialog() End


    private fun mkHolder(data:ArrayList<HashMap<String, Any>>,
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

                //홀더: 근무삭제
                holder.findViewById<ImageButton>(R.id.holderDeleteWorkBtn).setOnClickListener {
                    data.remove(map)
                    holderLayout.removeView(holder)
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


    private fun mkAddWorkDialog(callback:(String,Boolean)->Unit){
        Dialog(requireContext()).apply{
            setContentView(R.layout.dialog_add_type)
            val workNameEditText = this.findViewById<TextView>(R.id.toAddWorkName)
            val isWorkPatrolBox = this.findViewById<CheckBox>(R.id.isToAddPatrol)
            this.findViewById<Button>(R.id.addWorkBtn).setOnClickListener {
                callback(workNameEditText.text.toString(), isWorkPatrolBox.isChecked)
                this.dismiss()
            }
            show()
        }
    }

    private fun mkEditWorkDialog(name:String, isPatrol:Boolean, callback:(String, Boolean)->Unit){
        Dialog(requireContext()).apply{
            setContentView(R.layout.dialog_add_type)
            val workNameEditText = this.findViewById<TextView>(R.id.toAddWorkName)
            val isWorkPatrolBox = this.findViewById<CheckBox>(R.id.isToAddPatrol)
            workNameEditText.text = name
            isWorkPatrolBox.isChecked=isPatrol
            this.findViewById<Button>(R.id.addWorkBtn).setOnClickListener {
                callback(workNameEditText.text.toString(), isWorkPatrolBox.isChecked)
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

    private fun deepCopyMap(map:HashMap<String, Any>):HashMap<String, Any>{
        val result = hashMapOf<String, Any>()
        map.forEach { (key, value) ->
            result[key] = value
        }
        return result
    }

} // class WorkFragment : Fragment() End
