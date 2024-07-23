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

class SqliteHelper(context:Context?, name:String, version: Int):SQLiteOpenHelper(context, name, null, version) {

	private val mkWorkTable = """
		CREATE TABLE IF NOT EXISTS WorkTable
			(
				id INTEGER PRIMARY KEY AUTOINCREMENT,
				workName TEXT,
				typeList BLOB,
				shiftList BLOB,
				sortIndex INTEGER
			)
	""".trimIndent()

	private val mkMemberTable = """
		CREATE TABLE IF NOT EXISTS MemberTable
			(
				id INTEGER PRIMARY KEY AUTOINCREMENT,
				memberName TEXT,
				sortIndex INTEGER
			)
	""".trimIndent()

	private val mkLogTable = """
		CREATE TABLE IF NOT EXISTS LogTable
			(
				id INTEGER PRIMARY KEY AUTOINCREMENT,
				logDate TEXT,
				logMapList BLOB,
				typeMapList BLOB,
				shiftMapList BLOB,
				mainMemberList BLOB,
				subMemberList BLOB,
				workName TEXT
			)
	""".trimIndent()

	private val mkSizeTable = """
		CREATE TABLE IF NOT EXISTS SizeTable
		(
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			sizeName TEXT,
			tableWidth INTEGER,
			tableHeight INTEGER,
			typeWidth REAL,
			tableTextSize INTEGER,
			dateTextSize INTEGER
		)
	""".trimIndent()

	// 테이블 생성
	override fun onCreate(p0: SQLiteDatabase) {
		try{
			Log.d("test", "helper OnCreate 실행")
			listOf(mkWorkTable, mkMemberTable, mkLogTable, mkSizeTable).forEach {
				p0.execSQL(it)
			}
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


	fun select(
		tableName:String,
		toSortColumn:String? = null,
		where:HashMap<String, Any>? = null,
	):ArrayList<HashMap<String,Any>>{
		val resultMapArrayList = arrayListOf<HashMap<String,Any>>()
		val orderBy = if(toSortColumn == null) "" else "ORDER BY $toSortColumn"
		val whereClause = if(where == null) "" else "WHERE" + where.map {" ${it.key} = '${it.value}' "}.joinToString("AND")
		val sql = "SELECT * FROM $tableName $whereClause $orderBy"
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
		return resultMapArrayList
	}

	// 조건에 맞는 데이터 삭제
	fun deleteByCondition(tableName:String, where:HashMap<String,*>){
		val condition = where.map{
			"${it.key}='${it.value}'"
		}.joinToString (" AND ")
		val sql = "DELETE FROM $tableName WHERE $condition"
		val wd = writableDatabase
		wd.execSQL(sql)
		wd.close()
	}

	fun updateByCondition(
		tableName: String,
		where: HashMap<String, Any>? = null,
		updateMap: HashMap<String, Any>
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

		val whereClause = where?.map { "${it.key} = ?" }?.joinToString(" AND ")
		val whereArgs = where?.values?.map { it.toString() }?.toTypedArray()


		val rowsUpdated = wd.update(tableName, values, whereClause, whereArgs)
		wd.close()
		return rowsUpdated
	}


	fun dropTable(tableName:String){
		val wd = writableDatabase
		wd.execSQL("DROP TABLE IF EXISTS $tableName")
		onCreate(wd)
		wd.close()
	}


}