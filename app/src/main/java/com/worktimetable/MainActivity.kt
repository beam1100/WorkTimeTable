package com.worktimetable

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.worktimetable.databinding.ActivityMainBinding

class MainActivity : FragmentActivity() {

    private lateinit var vBinding:ActivityMainBinding

    private val tableFragment = TableFragment()
    private val personnelFragment = PersonnelFragment()
    private val workFragment = WorkFragment()
    val helper = SqliteHelper(this, "WorkTable.db", 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        try{



            vBinding = ActivityMainBinding.inflate(layoutInflater)
            super.onCreate(savedInstanceState)
            setContentView(vBinding.root)

            vBinding.tableFragmentBtn.setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .replace(vBinding.fragmentContainerView.id, tableFragment)
                    .commit()
            }

            vBinding.personnelFragmentBtn.setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .replace(vBinding.fragmentContainerView.id, personnelFragment)
                    .commit()
            }

            vBinding.workFragmentBtn.setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .replace(vBinding.fragmentContainerView.id, workFragment)
                    .commit()
            }

        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }


    }


}