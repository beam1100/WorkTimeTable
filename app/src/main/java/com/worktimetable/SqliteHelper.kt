package com.worktimetable

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.security.auth.callback.Callback

class SqliteHelper(context:Context?, name:String, version: Int):SQLiteOpenHelper(context, name, null, version) {

	private val mkTable = """
		CREATE TABLE IF NOT EXISTS WorkTable
			(
				id INTEGER PRIMARY KEY AUTOINCREMENT,
				work BLOB
			)
	""".trimIndent()

	// 테이블 생성
	override fun onCreate(p0: SQLiteDatabase) {
		try{
			Log.d("test", "helper OnCreate 실행")
			p0.execSQL(mkTable)
		}catch(err:Exception){
			Log.d("test", err.toString())
			Log.d("test", err.stackTraceToString())
		}
	}

	override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {}

	fun insert(tableName:String, columnAndValue: HashMap<String, Any>){
		try{
			val wd: SQLiteDatabase =writableDatabase
			val values = ContentValues()
			columnAndValue.forEach { (key, value) ->
				when(value){
					is String -> values.put(key, value)
					is Int -> values.put(key, value)
					is Collection<*> -> {
						val byteArrayOutPutStream = ByteArrayOutputStream()
						val objectOutputStream = ObjectOutputStream(byteArrayOutPutStream)
						objectOutputStream.writeObject(value)
						objectOutputStream.flush()
						val dateBytes = byteArrayOutPutStream.toByteArray()
						values.put(key, dateBytes)
						objectOutputStream.close()
						byteArrayOutPutStream.close()
					}
					is Map<*, *> -> {
						val byteArrayOutputStream = ByteArrayOutputStream()
						val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
						objectOutputStream.writeObject(value)
						objectOutputStream.flush()
						val dataBytes = byteArrayOutputStream.toByteArray()
						values.put(key, dataBytes)
						objectOutputStream.close()
						byteArrayOutputStream.close()
					}
					else -> throw IllegalArgumentException("Unsupported data type")
				}
			}
			wd.insert(tableName, null, values)
			wd.close()
		}catch (err:Exception){
			Log.d("test", err.toString())
			Log.d("test", err.stackTraceToString())
		}
	}


	fun selectAll(tableName:String, callback: (ArrayList<HashMap<String,Any>>)->Unit){
		val resultMapArrayList = arrayListOf<HashMap<String,Any>>()
		val sql = "SELECT * FROM $tableName"
		val rd = readableDatabase
		val cursor = rd.rawQuery(sql, null)
		while(cursor.moveToNext()){
			val tempHashMap = hashMapOf<String, Any>()
			cursor.columnNames.forEach { columnName ->
				val columnIndex = cursor.getColumnIndex(columnName)
				val value = when(cursor.getType(columnIndex)){
					Cursor.FIELD_TYPE_INTEGER -> cursor.getInt(columnIndex)
					Cursor.FIELD_TYPE_STRING -> cursor.getString(columnIndex)
					Cursor.FIELD_TYPE_BLOB -> {
						val dataBytes = cursor.getBlob(columnIndex)
						val byteArrayInputStream = ByteArrayInputStream(dataBytes)
						val objectInputStream = ObjectInputStream(byteArrayInputStream)
						objectInputStream.readObject()
					}
					else -> cursor.getString(columnIndex)
				}
				value?.let{
					tempHashMap[columnName] = value
				}
			}
			resultMapArrayList.add(tempHashMap)
		}
		rd.close()
		callback(resultMapArrayList)
	}

	// 조건에 맞는 데이터 삭제
	fun deleteByCondition(tableName:String, whereCondition:HashMap<String,*>){
		val condition = whereCondition.map{
			"${it.key}='${it.value}'"
		}.joinToString (" AND ")
		val sql = "DELETE FROM $tableName WHERE $condition"
		val wd = writableDatabase
		wd.execSQL(sql)
		wd.close()
	}

	fun dropTable(tableName:String){
		val wd = writableDatabase
		wd.execSQL("DROP TABLE IF EXISTS $tableName")
		onCreate(wd)
		wd.close()
	}


	fun updateByCondition(
		tableName: String,
		updateMap: HashMap<String, Any>,
		conditionMap: HashMap<String, Any>? = null
	): Int {
		val wd = writableDatabase
		val values = ContentValues()

		updateMap.forEach { (key, value) ->
			when (value) {
				is String -> values.put(key, value)
				is Int -> values.put(key, value)
				is Boolean -> values.put(key, if (value) 1 else 0)
				is ByteArray -> values.put(key, value)
				is Collection<*> -> {
					val byteArrayOutputStream = ByteArrayOutputStream()
					val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
					objectOutputStream.writeObject(value)
					objectOutputStream.flush()
					val dataBytes = byteArrayOutputStream.toByteArray()
					values.put(key, dataBytes)
					objectOutputStream.close()
					byteArrayOutputStream.close()
				}
				is Map<*, *> -> {
					val byteArrayOutputStream = ByteArrayOutputStream()
					val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
					objectOutputStream.writeObject(value)
					objectOutputStream.flush()
					val dataBytes = byteArrayOutputStream.toByteArray()
					values.put(key, dataBytes)
					objectOutputStream.close()
					byteArrayOutputStream.close()
				}
				else -> throw IllegalArgumentException("Unsupported value type")
			}
		}

		val whereClause = conditionMap?.map { "${it.key} = ?" }?.joinToString(" AND ")
		val whereArgs = conditionMap?.values?.map { it.toString() }?.toTypedArray()

		val rowsUpdated = wd.update(tableName, values, whereClause, whereArgs)
		wd.close()
		return rowsUpdated
	}



}

/*package com.jym.examplesqlite

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


class SqliteHelper(context: Context?, name:String, version:Int):
	SQLiteOpenHelper(context, name, null, version) {

	private val mkTable = """
		CREATE TABLE IF NOT EXISTS LogTable
			(
				id INTEGER PRIMARY KEY AUTOINCREMENT,
				name TEXT,
				memo TEXT,
				collection BLOB,
				time TEXT
			)
	""".trimIndent()

	// 테이블 생성
	override fun onCreate(p0: SQLiteDatabase) {
		try{
			p0.execSQL(mkTable)

		}catch(err:Exception){
			Log.d("test", err.toString())
			Log.d("test", err.stackTraceToString())
		}
	}

	// 업그래이드
	override fun onUpgrade(p0: SQLiteDatabase, p1: Int, p2: Int) {
		if(false){
			val sql = "ALTER TABLE LogTable ADD addedColumn INTEGER DEFAULT 0"
			p0.execSQL(sql)
		}
	}


	// 컬럼 목록 가져오기
	fun getColumnNames(tableName: String): List<String>? {
		val db = readableDatabase
		val cursor: Cursor = db.rawQuery("SELECT * FROM $tableName", null)
		val columnNames = arrayListOf<String>()
		cursor.use {
			for (columnName in cursor.columnNames) {
				columnNames.add(columnName)
			}
		}
		db.close()
		return columnNames
	}


}*/