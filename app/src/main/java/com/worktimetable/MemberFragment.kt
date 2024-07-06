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
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isGone
import com.worktimetable.databinding.FragmentMemberBinding
import com.worktimetable.databinding.FragmentWorkBinding


class MemberFragment : Fragment() {

    companion object {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var _vBinding: FragmentMemberBinding? = null
    private val vBinding get() = _vBinding!!
    private lateinit var mainActivity:MainActivity

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


            vBinding.addNewMemberBtn.setOnClickListener {
                setMemberDialog()
            }

            /*인원 드랍 버튼*/
            vBinding.dropMemberBtn.setOnClickListener {
                mainActivity.helper.dropTable(("MemberTable"))
                onViewCreated(view, savedInstanceState)
            }

        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun setMemberDialog() {
        Dialog(requireContext()).apply {
            setContentView(R.layout.dialog_set_member)

            /* 멤버 삭제 */
            findViewById<Button>(R.id.deleteMemberBtn).setOnClickListener {
                Toast.makeText(requireContext(), "삭제", Toast.LENGTH_SHORT).show()
            }

            mainActivity.setDialogSize(this, vBinding.memberFragmentLayout, 0.9f, null)
            show()
        }
    }


}