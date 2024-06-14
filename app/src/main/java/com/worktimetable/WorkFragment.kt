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

            this.findViewById<Button>(R.id.mkAddWorkDialogBtn).setOnClickListener {
                mkAddWorkDialog {
                    this.findViewById<LinearLayout>(R.id.workLayout).removeAllViews()
                    workMapList.add(hashMapOf(
                        "workName" to it.first,
                        "isPatrol" to it.second
                    ))
                    printWorkToLayout(this)
                    Log.d("test", workMapList.toString())
                }
            }
            this.findViewById<Button>(R.id.saveWorkBtn).setOnClickListener {
            }
            show()
        }
    }

    private fun mkAddWorkDialog(callback:(Pair<String,Boolean>)->Unit){
        Dialog(requireContext()).apply{
            setContentView(R.layout.dialog_add_work)
            this.findViewById<Button>(R.id.addWorkBtn).setOnClickListener {
                callback(
                    Pair(
                        this.findViewById<TextView>(R.id.toAddWorkName).text.toString(),
                        this.findViewById<CheckBox>(R.id.isToAddPatrol).isChecked)
                )
                this.dismiss()
            }
            show()
        }
    }

    private fun printWorkToLayout(dialog:Dialog){
        workMapList.forEach {
            val inflater = LayoutInflater.from(requireContext())
            val holder = inflater.inflate(R.layout.holder_set_work, null) as LinearLayout
            val holderTV = holder.findViewById<TextView>(R.id.workNameInHolder)
            holderTV.text = it["workName"] as String
            dialog.findViewById<LinearLayout>(R.id.workLayout).addView(holder)
        }
    }


}
