package com.worktimetable

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.worktimetable.databinding.FragmentTableBinding
import kotlin.math.log


class TableFragment : Fragment() {

    companion object {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var _vBinding: FragmentTableBinding? = null
    private val vBinding get() = _vBinding!!
    private lateinit var mainActivity:MainActivity
    private var logMapList = arrayListOf<HashMap<String,Any>>()

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

            vBinding.mkWorkSelectDialogBtn.setOnClickListener {
                mkSelectWorkDialog{selectedWorkName->
                    val workMap = mainActivity.helper.select("WorkTable", where= hashMapOf("workName" to selectedWorkName)).first()
                    val typeList = workMap["typeList"] as ArrayList<HashMap<String, Any>>
                    val shiftList = workMap["shiftList"] as ArrayList<HashMap<String, Any>>

                    logMapList = mkNewLogList(typeList, shiftList)
                    mkTable(typeList, shiftList)
                }
            }

            vBinding.printLogBtn.setOnClickListener {
                logMapList.forEach {
                    Log.d("test", it.toString())
                }
            }

        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun mkNewLogList(typeList: ArrayList<HashMap<String, Any>>, shiftList: ArrayList<HashMap<String, Any>>):ArrayList<HashMap<String,Any>> {
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

    private fun mkTable(typeList:ArrayList<HashMap<String,Any>>, shiftList:ArrayList<HashMap<String,Any>>){
        try{
            clearTable()

            // 메인 테이블
            val tableLayout = TableLayout(requireContext())
            for(typeMap in typeList){
                val tableRow = TableRow(requireContext())
                for(shiftMap in shiftList){
                    AppCompatButton(requireContext()).apply {
                        tableRow.addView(this)
                        mainActivity.getMapByCondition(
                            logMapList,
                            hashMapOf("type" to typeMap["type"] as String, "shift" to shiftMap["shift"] as String)
                        )?.set("btn", this)
                        this.setOnClickListener {
                            Toast.makeText(requireContext(), "클릭이벤트", Toast.LENGTH_SHORT).show()
                        }
                        setBtnStyle(this, androidx.appcompat.R.color.material_grey_100, 200, 200)


                    }
                }
                tableLayout.addView(tableRow)
            }
            vBinding.subSV.addView(tableLayout)

            // 행제목(근무 종류)
            val rowTL = TableLayout(requireContext())
            for(typeMap in typeList){
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
            for(shiftMap in shiftList){
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


    private fun setBtnStyle(btn: AppCompatButton, backGroundColor:Int, width:Int, height:Int){
        btn.setBackgroundColor(ContextCompat.getColor(requireContext(), backGroundColor))
        val params = TableRow.LayoutParams(width, height)
        params.gravity = Gravity.NO_GRAVITY
        params.setMargins(3, 3, 3, 3)
        btn.layoutParams = params
        btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
    }



}