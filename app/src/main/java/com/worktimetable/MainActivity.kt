package com.worktimetable

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.worktimetable.databinding.ActivityMainBinding

class MainActivity : FragmentActivity() {

    private lateinit var vBinding:ActivityMainBinding

    private val tableFragment = TableFragment()
    private val memberFragment = MemberFragment()
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

            vBinding.memberFragmentBtn.setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .replace(vBinding.fragmentContainerView.id, memberFragment)
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

    fun mkHolderFromMap(
        data:ArrayList<HashMap<String, Any>>,
        holderLayout: LinearLayout,
        holder: LinearLayout,
        mapItem:HashMap<String,Any>,
        toPrintKey:String,
        longClickCallback: (HashMap<String,Any>) -> Unit
    ){
        try{

            //홀더 근무이름 텍스트뷰
            holder.findViewById<TextView>(R.id.holderTV).apply{
                text = mapItem[toPrintKey] as String
                setOnLongClickListener {_ ->
                    longClickCallback(mapItem)
                    return@setOnLongClickListener true
                }
            }

            //맵에서 위로
            holder.findViewById<ImageButton>(R.id.holderMoveItemUp).setOnClickListener {
                val holderIndex = holderLayout.indexOfChild(holder)
                if(holderIndex > 0 && holderLayout.childCount>=2){
                    holderLayout.removeView(holder)
                    holderLayout.addView(holder, holderIndex-1)
                }
                val mapIndex = data.indexOf(mapItem)
                if(mapIndex > 0){
                    data[mapIndex] = data[mapIndex-1]
                    data[mapIndex-1] = mapItem
                }
            }

            //맵에서 아래로
            holder.findViewById<ImageButton>(R.id.holderMoveItemDown).setOnClickListener {
                val holderIndex = holderLayout.indexOfChild(holder)
                if(holderIndex < holderLayout.childCount-1 && holderLayout.childCount>=2){
                    holderLayout.removeView(holder)
                    holderLayout.addView(holder, holderIndex+1)
                }

                val mapIndex = data.indexOf(mapItem)
                if(mapIndex < data.size-1){
                    data[mapIndex] = data[mapIndex+1]
                    data[mapIndex+1] = mapItem
                }
            }
            holderLayout.addView(holder)
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }




    fun mkHolderFromDB(
        data:ArrayList<HashMap<String, Any>>,
        holderLayout: LinearLayout,
        holder: LinearLayout,
        mapItem:HashMap<String,Any>,
        toPrintKey:String,
        longClickCallback: (HashMap<String,Any>) -> Unit
    ){
        try{

            //홀더 근무이름 텍스트뷰
            holder.findViewById<TextView>(R.id.holderTV).apply{
                text = mapItem[toPrintKey] as String
                setOnLongClickListener {_ ->
                    longClickCallback(mapItem)
                    return@setOnLongClickListener true
                }
            }

            //DB에서 위로
            holder.findViewById<ImageButton>(R.id.holderMoveItemUp).setOnClickListener {
                Toast.makeText(this@MainActivity, "db 순서 올리기", Toast.LENGTH_SHORT).show()
            }

            //DB에서 아래로
            holder.findViewById<ImageButton>(R.id.holderMoveItemDown).setOnClickListener {
                Toast.makeText(this@MainActivity, "db순서 내리기", Toast.LENGTH_SHORT).show()
            }
            holderLayout.addView(holder)
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }



}