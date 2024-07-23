package com.worktimetable

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.worktimetable.databinding.FragmentDatabaseBinding
import java.io.FileOutputStream


class DatabaseFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    private var _vBinding: FragmentDatabaseBinding? = null
    private val vBinding get() = _vBinding!!
    private lateinit var mainActivity:MainActivity



    override fun onDestroyView() {
        super.onDestroyView()
        _vBinding = null
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _vBinding = FragmentDatabaseBinding.inflate(inflater,container,false)
        return vBinding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is MainActivity){
            mainActivity = context
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            super.onViewCreated(view, savedInstanceState)

            vBinding.putDbBtn.setOnClickListener {
                mainActivity.mkConfirmDialog(
                    "자료를 백업하시겠습니까?",
                    {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/octet-stream"
                            putExtra(Intent.EXTRA_TITLE, "WorkTable")
                        }
                        mainActivity.resultLauncher.launch(intent)
                    },
                    {}
                )
            }

            vBinding.getDbBtn.setOnClickListener {
                mainActivity.mkConfirmDialog(
                    "기존 기록이 모두 지워지고, 가져온 기록이 적용됩니다. 진행하시겠습니까?",
                    {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/octet-stream"
                        }
                        mainActivity.readResultLauncher.launch(intent)
                    },
                    {}
                )
            }

            vBinding.resetDbBtn.setOnClickListener {
                mainActivity.mkConfirmDialog(
                    "기존 기록을 모두 삭제합니다. 진행하시겠습니까?",
                    {
                        listOf("LogTable", "MemberTable", "WorkTable", "SizeTable").forEach {
                            mainActivity.helper.dropTable(it)
                        }
                        val editor = mainActivity.preferences.edit()
                        editor.clear()
                        editor.apply()
                        Toast.makeText(requireContext(), "초기화 되었습니다.", Toast.LENGTH_SHORT).show()
                    },
                    {}
                )
            }

        }catch (err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }






}// class DatabaseFragment : Fragment()  End