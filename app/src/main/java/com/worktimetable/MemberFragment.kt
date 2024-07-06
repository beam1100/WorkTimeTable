package com.worktimetable

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }



}