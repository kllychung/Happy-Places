package com.project.happyplaces.databases

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.project.happyplaces.models.HappyPlaceModel

class DatabaseHandler(
    context: Context
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "HappyPlace"
        private const val DATABASE_VERSION = 1
        private const val DATABASE_TABLE = "HappyPlaceTable"

        //All the columns name
        private const val COL_ID= "id"
        private const val COL_TITLE = "title"
        private const val COL_IMAGE_PATH = "imagePath"
        private const val COL_DESCRIPTION = "description"
        private const val COL_DATE = "date"
        private const val COL_LOCATION = "location"
        private const val COL_LATITUDE = "latitude"
        private const val COL_LONGITUDE = "longitude"

    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTblSqlStmt = "CREATE TABLE ${DATABASE_TABLE} (" +
                COL_ID + " INTEGER PRIMARY KEY, " +
                COL_TITLE + " TEXT ," +
                COL_IMAGE_PATH + " TEXT ," +
                COL_DESCRIPTION  + " TEXT ," +
                COL_DATE + " TEXT ," +
                COL_LOCATION + " TEXT ," +
                COL_LATITUDE + " TEXT ," +
                COL_LONGITUDE + " TEXT " +
                ")"

        Log.e("DATABASE QUERY", createTblSqlStmt);
        db?.execSQL(createTblSqlStmt)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS ${DATABASE_TABLE} ")
        onCreate(db)
    }

    fun insertHappyPlace(happyPlace: HappyPlaceModel): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_TITLE, happyPlace.title)
        contentValues.put(COL_IMAGE_PATH, happyPlace.imagePath)
        contentValues.put(COL_DESCRIPTION, happyPlace.description)
        contentValues.put(COL_DATE, happyPlace.date)
        contentValues.put(COL_LOCATION, happyPlace.location)
        contentValues.put(COL_LATITUDE, happyPlace.latitude)
        contentValues.put(COL_LONGITUDE, happyPlace.longitude)

        val result =  db.insert(DATABASE_TABLE, "", contentValues)
        db.close()
        return result
    }

    fun updateHappyPlace(happyPlace: HappyPlaceModel): Int{
        val db = this.writableDatabase

        val contentValues = ContentValues()
        contentValues.put(COL_TITLE, happyPlace.title)
        contentValues.put(COL_IMAGE_PATH, happyPlace.imagePath)
        contentValues.put(COL_DESCRIPTION, happyPlace.description)
        contentValues.put(COL_DATE, happyPlace.date)
        contentValues.put(COL_LOCATION, happyPlace.location)
        contentValues.put(COL_LATITUDE, happyPlace.latitude)
        contentValues.put(COL_LONGITUDE, happyPlace.longitude)

        val result = db.update(DATABASE_TABLE, contentValues,
            COL_ID + "= ${happyPlace.id}", null )
        db.close()
        return result;
    }

    fun deleteHappyPlace(happyPlace: HappyPlaceModel): Int {
        val db = this.writableDatabase
        val result = db.delete(DATABASE_TABLE, COL_ID + "= ${happyPlace.id}", null)
        db.close()
        return  result
    }

    @SuppressLint("Range")
    fun getHappyPlacesList(): ArrayList<HappyPlaceModel>{
        val happyPlaceList =  ArrayList<HappyPlaceModel>()
        val selectQuery = "SELECT * FROM $DATABASE_TABLE"
        val db = this.readableDatabase

        try{
            val cursor: Cursor = db.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()){
                do{
                    val place = HappyPlaceModel(
                        cursor.getInt(cursor.getColumnIndex(COL_ID)),
                        cursor.getString(cursor.getColumnIndex(COL_TITLE)),
                        cursor.getString(cursor.getColumnIndex(COL_IMAGE_PATH)),
                        cursor.getString(cursor.getColumnIndex(COL_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndex(COL_DATE)),
                        cursor.getString(cursor.getColumnIndex(COL_LOCATION)),
                        cursor.getDouble(cursor.getColumnIndex(COL_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(COL_LONGITUDE)),
                    )
                    happyPlaceList.add(place)
                }
                while (cursor.moveToNext())
            }
            cursor.close()
        }
        catch (e: SQLiteException){
            db.execSQL(selectQuery)
            return ArrayList()
        }
        return happyPlaceList;
    }
}