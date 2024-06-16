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
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
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
            "work" to arrayListOf<HashMap<String, Any>>(
                hashMapOf("work" to "상황","isPatrol" to false),
                hashMapOf("work" to "1지구","isPatrol" to true),
                hashMapOf("work" to "2지구","isPatrol" to true),
                hashMapOf("work" to "3지구","isPatrol" to true),
                hashMapOf("work" to "도보","isPatrol" to false),
            )
        ),
        hashMapOf(
            "workName" to "야간근무",
            "work" to arrayListOf<HashMap<String, Any>>(
                hashMapOf("workType" to "상황","isPatrol" to false),
                hashMapOf("workType" to "1지구","isPatrol" to true),
                hashMapOf("workType" to "2지구","isPatrol" to true),
                hashMapOf("workType" to "3지구","isPatrol" to true),
                hashMapOf("workType" to "대기","isPatrol" to false),
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
        super.onViewCreated(view, savedInstanceState)

        val inflater = LayoutInflater.from(requireContext())
        val holderLayout = vBinding.workLayout
        sampleData.forEach { map ->
            val holder = inflater.inflate(R.layout.holder_set_work, null) as LinearLayout
            mkHolder(sampleData, holderLayout, holder, hashMapOf("workName" to map["workName"] as String))
        }

        vBinding.mkWorkTypeBtn.setOnClickListener {
            mkWorkDialog()
        }
    }

    private fun mkWorkDialog(){
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_set_work)
            val layoutParams = window?.attributes
            val viewWidth = vBinding.workDialogLayout.width
            val viewHeight = vBinding.workDialogLayout.height
            layoutParams?.width = (viewWidth * 0.95).toInt()
            layoutParams?.height = (viewHeight * 0.9).toInt()
            window?.attributes = layoutParams

            val holderLayout = this.findViewById<LinearLayout>(R.id.workLayout)
            holderLayout.removeAllViews()

            this.findViewById<ImageButton>(R.id.mkAddWorkDialogBtn).setOnClickListener { _ ->
                mkAddWorkDialog { it ->
                    val workMap = hashMapOf<String, Any>("workType" to it.first,"isPatrol" to it.second)
                    workMapList.add(workMap)
                    val inflater = LayoutInflater.from(requireContext())
                    val holder = inflater.inflate(R.layout.holder_set_work, null) as LinearLayout

                    mkHolder(workMapList, holderLayout, holder, hashMapOf("workType" to it.first))

                } // mkAddWorkDialog End
            } // this.findViewById<Button>(R.id.mkAddWorkDialogBtn).setOnClickListener End
            show()

            this.findViewById<Button>(R.id.saveWorkBtn).setOnClickListener {
                Log.d("test", workMapList.toString())
            }

        } // Dialog(requireContext()).apply End
    } // private fun mkWorkDialog() End

    private fun mkHolder(data:ArrayList<HashMap<String, Any>>, holderLayout:LinearLayout, holder:LinearLayout, condition:HashMap<String,Any>){
        try{
            val workMap = getMapByCondition(data, condition)

            //홀더 근무이름 텍스트뷰
            holder.findViewById<TextView>(R.id.holderWorkName).apply{
                text = condition.entries.first().value as String
                /*setOnLongClickListener {_ ->
                    workMap?.let{ map ->
                        mkEditWorkDialog(map["workType"] as String, map["isPatrol"] as Boolean){ result ->
                            holder.findViewById<TextView>(R.id.holderWorkName).text = result.first
                            map["workType"] = result.first
                            map["isPatrol"] = result.second
                        }
                    }
                    return@setOnLongClickListener true
                }*/
            }

            //홀더: 근무삭제
            holder.findViewById<ImageButton>(R.id.holderDeleteWorkBtn).setOnClickListener {
                data.remove(workMap)
                holderLayout.removeView(holder)
            }

            //홀더: 근무이동(위로)
            holder.findViewById<ImageButton>(R.id.holderMoveItemUp).setOnClickListener {
                val holderIndex = holderLayout.indexOfChild(holder)
                if(holderIndex > 0 && holderLayout.childCount>=2){
                    holderLayout.removeView(holder)
                    holderLayout.addView(holder, holderIndex-1)
                }
                workMap?.let{ map->
                    val mapIndex = data.indexOf(map)
                    if(mapIndex > 0){
                        data[mapIndex] = data[mapIndex-1]
                        data[mapIndex-1] = map
                    }
                }
            }

            //홀더: 근무이동(아래로)
            holder.findViewById<ImageButton>(R.id.holderMoveItemDown).setOnClickListener {
                val holderIndex = holderLayout.indexOfChild(holder)
                if(holderIndex < holderLayout.childCount-1 && holderLayout.childCount>=2){
                    holderLayout.removeView(holder)
                    holderLayout.addView(holder, holderIndex+1)
                }
                workMap?.let{map->
                    val mapIndex = data.indexOf(map)
                    if(mapIndex < data.size-1){
                        data[mapIndex] = data[mapIndex+1]
                        data[mapIndex+1] = map
                    }
                }
            }

        holderLayout.addView(holder)

        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }

    }


    private fun mkAddWorkDialog(callback:(Pair<String,Boolean>)->Unit){
        Dialog(requireContext()).apply{
            setContentView(R.layout.dialog_add_work)
            val workNameEditText = this.findViewById<TextView>(R.id.toAddWorkName)
            val isWorkPatrolBox = this.findViewById<CheckBox>(R.id.isToAddPatrol)
            this.findViewById<Button>(R.id.addWorkBtn).setOnClickListener {
                callback(Pair(workNameEditText.text.toString(), isWorkPatrolBox.isChecked))
                this.dismiss()
            }
            show()
        }
    }

    private fun mkEditWorkDialog(name:String, isPatrol:Boolean, callback:(Pair<String,Boolean>)->Unit){
        Dialog(requireContext()).apply{
            setContentView(R.layout.dialog_add_work)
            val workNameEditText = this.findViewById<TextView>(R.id.toAddWorkName)
            val isWorkPatrolBox = this.findViewById<CheckBox>(R.id.isToAddPatrol)
            workNameEditText.text = name
            isWorkPatrolBox.isChecked=isPatrol
            this.findViewById<Button>(R.id.addWorkBtn).setOnClickListener {
                callback(Pair(workNameEditText.text.toString(), isWorkPatrolBox.isChecked))
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

} // class WorkFragment : Fragment() End
