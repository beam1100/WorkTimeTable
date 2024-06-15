package com.worktimetable

import android.app.Dialog
import android.app.ProgressDialog.show
import android.content.Context
import android.media.Image
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
                    val workMap = hashMapOf<String, Any>(
                        "workName" to it.first,
                        "isPatrol" to it.second
                    )

                    workMapList.add(workMap)
                    val inflater = LayoutInflater.from(requireContext())
                    val holder = inflater.inflate(R.layout.holder_set_work, null) as LinearLayout
                    holderLayout.addView(holder)

                    //홀더 근무이름 텍스트뷰
                    holder.findViewById<TextView>(R.id.holderWorkName).apply{
                        text = it.first
                        setOnLongClickListener {_ ->
                            getMapByCondition(workMapList, hashMapOf("workName" to text))?.let{ map ->
                                mkEditWorkDialog(map["workName"] as String, map["isPatrol"] as Boolean){
                                    holder.findViewById<TextView>(R.id.holderWorkName).text = it.first
                                    map["workName"] = it.first
                                    map["isPatrol"] = it.second
                                }
                            }
                            return@setOnLongClickListener true
                        }
                    }


                    //홀더: 근무삭제
                    holder.findViewById<ImageButton>(R.id.holderDeleteWorkBtn).setOnClickListener {
                        workMapList.remove(workMap)
                        holderLayout.removeView(holder)
                    }

                    //홀더: 근무이동(위로)
                    holder.findViewById<ImageButton>(R.id.holderMoveItemUp).setOnClickListener {
                        val holderIndex = holderLayout.indexOfChild(holder)
                        if(holderIndex > 0 && holderLayout.childCount>=2){
                            holderLayout.removeView(holder)
                            holderLayout.addView(holder, holderIndex-1)
                        }
                        val mapIndex = workMapList.indexOf(workMap)
                        if(mapIndex > 0){
                            workMapList[mapIndex] = workMapList[mapIndex-1]
                            workMapList[mapIndex-1] = workMap
                        }
                    }

                    //홀더: 근무이동(아래로)
                    holder.findViewById<ImageButton>(R.id.holderMoveItemDown).setOnClickListener {
                        val holderIndex = holderLayout.indexOfChild(holder)
                        if(holderIndex < holderLayout.childCount-1 && holderLayout.childCount>=2){
                            holderLayout.removeView(holder)
                            holderLayout.addView(holder, holderIndex+1)
                        }
                        val mapIndex = workMapList.indexOf(workMap)
                        if(mapIndex < workMapList.size-1){
                            workMapList[mapIndex] = workMapList[mapIndex+1]
                            workMapList[mapIndex+1] = workMap
                        }
                    }

                } // mkAddWorkDialog End
            } // this.findViewById<Button>(R.id.mkAddWorkDialogBtn).setOnClickListener End

            show()

            this.findViewById<Button>(R.id.saveWorkBtn).setOnClickListener {
                Log.d("test", workMapList.toString())
            }

        } // Dialog(requireContext()).apply End
    } // private fun mkWorkDialog() End


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
