package com.kubgu.moskovka.pedometer;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.kubgu.moskovka.pedometer.data.DBAdapter;

public class StatisticsActivity extends AppCompatActivity {

    private ListView statistics;
    DBAdapter dbAdapter;
    SQLiteDatabase db;
    Cursor cursor;
    SimpleCursorAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        statistics = findViewById(R.id.lv_total_stats);

        dbAdapter = new DBAdapter(getApplicationContext());
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        db.close();
        cursor.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Open connection
        db = dbAdapter.getReadableDatabase();

        // Get data from db
        cursor =  db.rawQuery("select * from "+ DBAdapter.TABLE, null);
        // Select columns to display
        String[] headers = new String[] {DBAdapter.COLUMN_DATE, DBAdapter.COLUMN_STEPS,
                DBAdapter.COLUMN_CALORIES, DBAdapter.COLUMN_METERS, DBAdapter.COLUMN_SECONDS};
        // Create adapter
        userAdapter = new SimpleCursorAdapter(this, R.layout.db_data_output_layout,
                cursor, headers,
                new int[]{R.id.field1, R.id.field2, R.id.field3, R.id.field4, R.id.field5}, 0);
        statistics.setAdapter(userAdapter);
    }
}