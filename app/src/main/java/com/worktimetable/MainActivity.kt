package com.worktimetable

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
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
        tableName:String,
        holderLayout: LinearLayout,
        holder: LinearLayout,
        mapItem:HashMap<String,Any>,
        toPrintKey:String,
        longClickCallback: (HashMap<String,Any>) -> Unit,
        refreshHolder:()->Unit
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
                val selectMapList = helper.select(tableName, "sortIndex")
                val sortIndexList = selectMapList.map{it["sortIndex"] as Int}
                val hereSortIndex = mapItem["sortIndex"] as Int
                val hereIndex = sortIndexList.indexOf(hereSortIndex)
                if(hereIndex == 0){
                    return@setOnClickListener
                }else{
                    val beforeIndex = hereIndex -1
                    val beforeSortIndex = sortIndexList[beforeIndex]
                    val hereId = mapItem["id"] as Int
                    val beforeId = getMapByCondition(selectMapList, hashMapOf("sortIndex" to beforeSortIndex))?.get("id") as Int
                    helper.updateByCondition(tableName, hashMapOf("id" to hereId), hashMapOf("sortIndex" to beforeSortIndex))
                    helper.updateByCondition(tableName, hashMapOf("id" to beforeId), hashMapOf("sortIndex" to hereSortIndex))
                    refreshHolder()
                }
            }

            //DB에서 아래로
            holder.findViewById<ImageButton>(R.id.holderMoveItemDown).setOnClickListener {
                val selectMapList = helper.select(tableName, "sortIndex")
                val sortIndexList = selectMapList.map{it["sortIndex"] as Int}
                val hereSortIndex = mapItem["sortIndex"] as Int
                val hereIndex = sortIndexList.indexOf(hereSortIndex)
                if(hereIndex >= sortIndexList.size-1){
                    return@setOnClickListener
                }else{
                    val nextIndex = hereIndex + 1
                    val nextSortIndex = sortIndexList[nextIndex]
                    val hereId = mapItem["id"] as Int
                    val nextId = getMapByCondition(selectMapList, hashMapOf("sortIndex" to nextSortIndex))?.get("id") as Int
                    helper.updateByCondition(tableName, hashMapOf("id" to hereId), hashMapOf("sortIndex" to nextSortIndex))
                    helper.updateByCondition(tableName, hashMapOf("id" to nextId), hashMapOf("sortIndex" to hereSortIndex))
                    refreshHolder()
                }
            }

            holderLayout.addView(holder)
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    fun getMapByCondition(
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

    fun deepCopy(obj: Any): Any {
        return when (obj) {
            is String -> obj
            is Int -> obj
            is Double -> obj
            is Boolean -> obj
            is Map<*, *> -> obj
                .mapKeys { it.key}
                .mapValues {it.value?.let {deepCopy(it)}}
            is List<*> -> obj.map {
                it?.let{deepCopy(it)}
            }
            else -> obj
        }
    }

    fun setDialogSize(dialog: Dialog, parent:View, widthRatio:Float?, heightRaito:Float?){
        val layoutParams = dialog.window?.attributes
        widthRatio?.let{
            layoutParams?.width = (parent.width * widthRatio).toInt()
        }
        heightRaito?.let{
            layoutParams?.height = (parent.height * heightRaito).toInt()
        }
        dialog.window?.attributes = layoutParams
    }



}