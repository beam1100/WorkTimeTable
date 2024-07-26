package com.worktimetable

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
//import android.media.tv.AdRequest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.textfield.TextInputLayout
import com.worktimetable.databinding.ActivityMainBinding
import java.io.FileOutputStream

class MainActivity : FragmentActivity() {

    private lateinit var vBinding:ActivityMainBinding

    //프래그먼트
    private val tableFragment = TableFragment()
    private val databaseFragment = DatabaseFragment()
    val memberFragment = MemberFragment()
    val workFragment = WorkFragment()

    //db
    val helper = SqliteHelper(this, "WorkTable.db", 1)
    val resultLauncher =  registerForActivityResult( ActivityResultContracts.StartActivityForResult() ){ result ->
        if (result.resultCode == FragmentActivity.RESULT_OK){
            val uri = result.data?.data
            val dbFile = getDatabasePath("WorkTable.db").readBytes()
            writeData(uri, dbFile)
            Toast.makeText(this@MainActivity, "DB파일 백업됨!!!!!", Toast.LENGTH_SHORT).show()
        }
    }
    val readResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == FragmentActivity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                val fileData = readData(it)
                getDatabasePath("WorkTable.db").writeBytes(fileData)
                Toast.makeText(this@MainActivity, "DB파일 가져옴!!!!!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //SharedPreferences
    val preferences: SharedPreferences by lazy {getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)}
    val editor: SharedPreferences.Editor by lazy {preferences.edit()}

    //adMob
    var rewardedAd: RewardedAd? = null
    private var adRequest = AdRequest.Builder().build()


    override fun onCreate(savedInstanceState: Bundle?) {
        try{
            vBinding = ActivityMainBinding.inflate(layoutInflater)
            super.onCreate(savedInstanceState)
            setContentView(vBinding.root)

            supportFragmentManager
                .beginTransaction()
                .replace(vBinding.fragmentContainerView.id, tableFragment)
                .commit()

            vBinding.tableFragmentBtn.setOnClickListener {
                replaceFragment(tableFragment)
            }

            vBinding.memberFragmentBtn.setOnClickListener {
                handleFragmentChange(memberFragment)
            }

            vBinding.workFragmentBtn.setOnClickListener {
                handleFragmentChange(workFragment)
            }

            vBinding.dbFragmentBtn.setOnClickListener {
                handleFragmentChange(databaseFragment)
            }

            MobileAds.initialize(this)
            loadAd()

        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }


    private fun handleFragmentChange(fragment: Fragment) {
        if (supportFragmentManager.findFragmentById(vBinding.fragmentContainerView.id) == tableFragment) {
            if (tableFragment.isChanged()) {
                mkConfirmDialog(
                    "변경된 내용을 저장하시겠습니까?",
                    {
                        tableFragment.saveLog()
                        replaceFragment(fragment)
                    },
                    {replaceFragment(fragment) }
                )
            } else {replaceFragment(fragment)}
        } else {
            replaceFragment(fragment)
        }
    }

    fun replaceFragment(fragment: Fragment){
        supportFragmentManager
            .beginTransaction()
            .replace(vBinding.fragmentContainerView.id, fragment)
            .commit()
    }

    fun mkHolderFromMap(
        data:ArrayList<HashMap<String, Any>>,
        holderLayout: LinearLayout,
        holder: LinearLayout,
        mapItem:HashMap<String,Any>,
        toPrintKey:String,
        getBtnCallback: (HashMap<String,Any>) -> Unit,
        updateBtnCallback: (HashMap<String,Any>) -> Unit,
        delBtnCallback: (HashMap<String,Any>) -> Unit,
    ){
        try{
            //홀더 텍스트뷰
            holder.findViewById<TextView>(R.id.holderTV).text = mapItem[toPrintKey] as String

            //홀더 업데이트 버튼
            holder.findViewById<ImageButton>(R.id.holderUpdateBtn).setOnClickListener {
                updateBtnCallback(mapItem)
            }

            //홀더 겟 버튼
            holder.findViewById<ImageButton>(R.id.holderGetBtn).setOnClickListener {
                getBtnCallback(mapItem)
            }

            //홀더 삭제 버튼
            holder.findViewById<ImageButton>(R.id.holderDelBtn).setOnClickListener {
                delBtnCallback(mapItem)
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
        refreshCallback:()->Unit,
        getBtnCallback: (HashMap<String,Any>) -> Unit,
        updateBtnCallback: (HashMap<String,Any>) -> Unit,
        delBtnCallback: (HashMap<String,Any>) -> Unit
    ){
        try{

            //홀더 텍스트뷰
            holder.findViewById<TextView>(R.id.holderTV).text = mapItem[toPrintKey] as String

            //홀더 겟 버튼
            holder.findViewById<ImageButton>(R.id.holderGetBtn).setOnClickListener {
                getBtnCallback(mapItem)
            }

            //홀더 업데이트 버튼
            holder.findViewById<ImageButton>(R.id.holderUpdateBtn).setOnClickListener {
                updateBtnCallback(mapItem)
            }

            //홀더 삭제 버튼
            holder.findViewById<ImageButton>(R.id.holderDelBtn).setOnClickListener {
                delBtnCallback(mapItem)
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
                    refreshCallback()
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
                    refreshCallback()
                }
            }

            holderLayout.addView(holder)
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }


    fun getMapByCondition(
        mapList:List<HashMap<String, Any>>,
        conditionMap:HashMap<String, Any>
    ): HashMap<String, Any>? {
        var resultMap:HashMap<String, Any>? = null
        outer@ for(map in mapList){
            for((key,value) in conditionMap){
                if(map[key]!=value){
                    continue@outer
                }
            }
            resultMap = map
        }
        return resultMap
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

    private fun getCalendarFromString(dateStr: String): Calendar {
        val format = SimpleDateFormat("yyyy-MM-dd")
        val date = format.parse(dateStr)
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar
    }

    // 문자열로 입력된 두 날짜 사이의 일수를 정수로 리턴
    fun daysBetween(startDate: String, endDate: String): Int{
        val startCalendar = getCalendarFromString(startDate)
        val endCalendar = getCalendarFromString(endDate)
        val diffInMillis = endCalendar.timeInMillis - startCalendar.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }

    fun mkConfirmDialog(text:String, yesCallback: () -> Unit, noCallback:()->Unit){
        Dialog(this).apply {
            setContentView(R.layout.dialog_confirm)
            setDialogSize(this, vBinding.mainLayout, 0.9f, null)
            show()
            findViewById<TextView>(R.id.confirmTV).text = text
            findViewById<Button>(R.id.confirmYesBtn).setOnClickListener {
                yesCallback()
                dismiss()
            }
            findViewById<Button>(R.id.confirmNoBtn).setOnClickListener {
                noCallback()
                dismiss()
            }
        }
    }

    fun mkInputTextDialog(toHint:String, callback: (String) -> Unit){
        try{
            Dialog(this).apply dialog@{
                setContentView(R.layout.dialog_input_text)
                setDialogSize(this, vBinding.mainLayout, 0.9f, null)
                show()

                val et = findViewById<EditText>(R.id.inputTextET).apply {
                    hint = toHint
                }

                findViewById<Button>(R.id.inputTextBtn).setOnClickListener {
                    callback(et.text.toString())
                    dialog@dismiss()
                }

            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    fun validateTimeForm(textInputLayout: TextInputLayout, errCondition:Boolean, errMessage:String){
        try{
            if(errCondition){
                textInputLayout.isErrorEnabled = true
                textInputLayout.error = errMessage
            }else{
                textInputLayout.isErrorEnabled = false
            }
        }catch(err:Exception){
            Log.d("test", err.toString())
            Log.d("test", err.stackTraceToString())
        }
    }

    private fun readData(uri: Uri): ByteArray {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: byteArrayOf()
    }

    private fun writeData(uri: Uri?, fileData:ByteArray) {
        try {
            contentResolver.openFileDescriptor(uri!!, "w")?.use { txt ->
                FileOutputStream(txt.fileDescriptor).use { fos ->
                    fos.write(fileData)
                    fos.close()
                }
                txt.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAd() {
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                // 광고 로드가 실패했을 때 호출
                /*Log.d("test", adError.toString())*/
                rewardedAd = null
            }
            override fun onAdLoaded(ad: RewardedAd) {
                // 광고가 성공적으로 로드되었을 때 호출
                rewardedAd = ad
                setFullScreenContentCallback()
            }
        })
    }

    private fun setFullScreenContentCallback() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {}
            override fun onAdImpression() {
                // 광고에 대한 노출이 기록되었을 때 호출.
            }
            override fun onAdShowedFullScreenContent() {
                // 광고가 표시될 때 호출.
            }
            override fun onAdDismissedFullScreenContent() {
                // 광고가 닫혔을 때 호출. 광고 참조를 null로 설정하여 광고 두 번 출력 방지
                rewardedAd = null
                loadAd()
            }
        }
    }

} // class MainActivity : FragmentActivity() End