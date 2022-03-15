package com.kubgu.moskovka.pedometer.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBAdapter extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "pedometer_statistics.db";
    // DB version
    private static final int SCHEMA = 1;
    public static final String TABLE = "sessions";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_STEPS = "stepsCount";
    public static final String COLUMN_CALORIES = "caloriesBurned";
    public static final String COLUMN_METERS = "metersCovered";
    public static final String COLUMN_SECONDS = "secondsPassed";

    public DBAdapter(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA);
    }

    public Map<String, Object> getLatestRow(SQLiteDatabase db)
    {
        Map<String,Object> row = new HashMap<>();

        String sql = "SELECT * "
                + "FROM " + TABLE
                + " WHERE " + COLUMN_DATE + " = "
                + "(SELECT MAX(" + COLUMN_DATE + ") FROM " + TABLE + ")";
        Cursor cursor = db.rawQuery(sql, new String[]{});
       try{
           cursor.moveToFirst();
           row.put(COLUMN_ID, cursor.getInt(0));
           row.put(COLUMN_DATE, cursor.getString(1));
           row.put(COLUMN_STEPS, cursor.getInt(2));
           row.put(COLUMN_CALORIES, cursor.getInt(3));
           row.put(COLUMN_METERS, cursor.getInt(4));
           row.put(COLUMN_SECONDS, cursor.getInt(5));
           return row;
       }
       catch(RuntimeException ex)
       {
           System.out.println(ex.getMessage());
           return null;
       }
       finally
       {
           cursor.close();
       }
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_DATE + " DATE,"
                + COLUMN_STEPS + " INTEGER,"
                + COLUMN_CALORIES + " INTEGER,"
                + COLUMN_METERS + " INTEGER,"
                + COLUMN_SECONDS + " INTEGER);");

        // Insert initial data
        db.execSQL("INSERT INTO " + TABLE + " ("
                + COLUMN_DATE + ", "
                + COLUMN_STEPS + ", " + COLUMN_CALORIES + ","
                + COLUMN_METERS + ", " + COLUMN_SECONDS
                + ") VALUES ("
                + "'2021-11-04', "
                + "1500, 300, "
                + "789, 800"
                + ");");

        db.execSQL("INSERT INTO " + TABLE + " ("
                + COLUMN_DATE + ", "
                + COLUMN_STEPS + ", " + COLUMN_CALORIES + ","
                + COLUMN_METERS + ", " + COLUMN_SECONDS
                + ") VALUES ("
                + "'2021-12-04', "
                + "3000, 600, "
                + "1670, 1400"
                + ");");

        db.execSQL("INSERT INTO " + TABLE + " ("
                + COLUMN_DATE + ", "
                + COLUMN_STEPS + ", " + COLUMN_CALORIES + ","
                + COLUMN_METERS + ", " + COLUMN_SECONDS
                + ") VALUES ("
                + "'2021-12-13', "
                + "3000, 600, "
                + "1670, 1400"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE);
        onCreate(db);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void update(Map<String, Object> entryToUpdate, SQLiteDatabase db)
    {
        // Remove id
        int id = (Integer)entryToUpdate.get(COLUMN_ID);
        List<Map.Entry<String, Object>> keyValuePairs = new ArrayList<>(entryToUpdate.entrySet());

        // Form the request
        String sql = "UPDATE " + TABLE + " SET ";
        for (int i = 0; i < keyValuePairs.size(); ++i)
        {
            // Do not write the id
            if (keyValuePairs.get(i).getKey() == COLUMN_ID
                    || keyValuePairs.get(i).getKey() == COLUMN_DATE)
                continue;
            sql += keyValuePairs.get(i).getKey() + " = "
                    + keyValuePairs.get(i).getValue().toString();
            if (i < keyValuePairs.size() - 1)
                sql += ", ";
        }
        sql += " WHERE " + COLUMN_ID + " = " + id + ";";
        db.execSQL(sql);
    }
}
