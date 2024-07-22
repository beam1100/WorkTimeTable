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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.isGone
import com.worktimetable.databinding.FragmentMemberBinding


class MemberFragment : Fragment() {

    companion object {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var _vBinding: FragmentMemberBinding? = null
    private val vBinding get() = _vBinding!!
    private lateinit var mainActivity:MainActivity

    override fun onDestroyView() {
        super.onDestroyView()
        _vBinding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        _vBinding = FragmentMemberBinding.inflate(inflater,container,false)
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
            val holderLayout = vBinding.memberLayout
            holderLayout.removeAllViews()


            /* db 멤버 홀더에 담기 */
            mainActivity.helper.select("MemberTable",toSortColumn = "sortIndex").onEach {memberMap->
                val holder = inflater.inflate(R.layout.holder_item, null) as LinearLayout
                holder.findViewById<ImageButton>(R.id.holderGetBtn).isGone = true
                holder.findViewById<ImageButton>(R.id.holderDelBtn).isGone = true
                mainActivity.mkHolderFromDB(
                    "MemberTable", holderLayout, holder, memberMap, "memberName",
                    refreshCallback = {onViewCreated(view, savedInstanceState)},
                    getBtnCallback = {},
                    delBtnCallback = {},
                    updateBtnCallback = {clickedMemberMap->
                        setMemberDialog(
                            clickedMemberMap,
                            {id, memberName->
                                mainActivity.helper.updateByCondition(
                                    "MemberTable",
                                    hashMapOf("id" to id as Any),
                                    hashMapOf("memberName" to memberName)
                                )
                                onViewCreated(view, savedInstanceState)
                            },
                            {
                                mainActivity.helper.deleteByCondition("MemberTable",hashMapOf("id" to clickedMemberMap["id"]))
                                onViewCreated(view, savedInstanceState)
                            }
                        )
                    }

                )
            }


            /* 인원 추가 버튼 */
            vBinding.mkSetMemberDialog.setOnClickListener {
                setMemberDialog(
                    null,
                    { _, memberName ->
                        val sortIndexMaxOrNull = mainActivity.helper.select("MemberTable").maxOfOrNull { it["sortIndex"] as Int}
                        val sortIndex = if(sortIndexMaxOrNull==null){0}else{sortIndexMaxOrNull+1}
                        mainActivity.helper.insert(
                            "MemberTable",
                            hashMapOf(
                                "memberName" to memberName,
                                "sortIndex" to sortIndex
                            )
                        )
                        onViewCreated(view, savedInstanceState)
                    },
                    {}
                )
            }

            /* 인원 드랍 버튼 */
            vBinding.dropMemberBtn.setOnClickListener {
                mainActivity.helper.dropTable(("MemberTable"))
                onViewCreated(view, savedInstanceState)
            }

            /* 인원 출력 버튼 */
            vBinding.selectMemberBtn.setOnClickListener {
                mainActivity.helper.select("MemberTable", toSortColumn = "sortIndex").onEach {memberMap->
                    memberMap.forEach{(key, value)->
                        Log.d("test", "■key:$key,    ■value: $value")
                    }
                    Log.d("test", "=".repeat(150))
                }
            }

        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }


    private fun setMemberDialog(
        clickedMap:HashMap<String, Any>?=null,
        setMap: (id:Int?, memberName:String) -> Unit,
        deleteMap: () -> Unit) {

        val selectedMemberMap = clickedMap?: hashMapOf("id" to null,"memberName" to "")
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_set_member)
            mainActivity.setDialogSize(this, vBinding.memberFragmentLayout, 0.9f, null)


            if(clickedMap==null){
                findViewById<Button>(R.id.deleteMemberBtn).isGone = true
            }

            /* 멤버 이름 출력 */
            clickedMap?.get("memberName")?.let{
                this.findViewById<EditText>(R.id.memberNameET).setText(it as String)
            }

            /* 멤버 저장 */
            findViewById<Button>(R.id.saveMemberBtn).setOnClickListener {
                setMap(
                    selectedMemberMap["id"] as? Int,
                    findViewById<EditText>(R.id.memberNameET).text.toString()
                )
                dismiss()
            }

            /* 멤버 삭제 */
            findViewById<Button>(R.id.deleteMemberBtn).setOnClickListener {
                deleteMap()
                dismiss()
            }

            show()
        }
    }


}