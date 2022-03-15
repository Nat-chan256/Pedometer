package com.kubgu.moskovka.pedometer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.kubgu.moskovka.pedometer.adapter.StatsDataAdapter;
import com.kubgu.moskovka.pedometer.adapter.StatsItem;
import com.kubgu.moskovka.pedometer.data.DBAdapter;
import com.kubgu.moskovka.pedometer.data.UserInfo;
import com.kubgu.moskovka.pedometer.logic.StepDetector;
import com.kubgu.moskovka.pedometer.logic.StepListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener, StepListener {
    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private int numSteps;

    private TextView tvSteps;
    private MaterialButton btnStart;
    private boolean onPause = true;
    private boolean justAfterPause = true;
    private Button btnStop;

    private ImageView ivSteps;
    private AnimationDrawable animSteps;

    // Statistics
    private StatsDataAdapter adapter;
    private ListView lvStats;
    private List<StatsItem> statsItems = new ArrayList<>();
    private long lastTimeStamp;

    private UserInfo userInfo;
    private SharedPreferences settingsPrefs;
    private SharedPreferences statsPrefs;

    // Work with database
    private DBAdapter dbAdapter;
    private SQLiteDatabase db;
    private boolean wasReset = false;
    private Map<String, Object> lastSavedValues;

    private final String STEPS_NUMBER_STR = "steps";
    private final String CALORIES_STR = "calories";
    private final String METERS_STR = "meters";
    private final String SECONDS_STR = "seconds";
    private final String PREFS_STATS_NAME = "currentStatistics";
    private final String WAS_RESET_DATE_STR = "wasResetDate";
    private final String WAS_RESET_STR = "wasReset";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }

        // Set sensitivity
        int sensitivity = data.getIntExtra(
                getResources().getString(R.string.sensitivity_key),
                getResources().getInteger(R.integer.default_sensitivity)
        );
        simpleStepDetector.setSensitivity(sensitivity/10.0);

        userInfo.setGrowth(data.getIntExtra(
                getResources().getString(R.string.growth_key),
                getResources().getInteger(R.integer.default_growth)
        ));

        userInfo.setWeight(data.getIntExtra(
                getResources().getString(R.string.weight_key),
                getResources().getInteger(R.integer.default_weight)
        ));

        userInfo.setStepLength(data.getIntExtra(
                        getResources().getString(R.string.step_length_key),
                        getResources().getInteger(R.integer.default_step_length)
        ));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statsPrefs = getSharedPreferences(PREFS_STATS_NAME, Context.MODE_PRIVATE);
        setUpWasResetFlag();

        setUpStepsNumber();
        setUpStepsDetector();

        // Buttons initialization
        setUpBtnStart();
        setUpBtnStop();
        setUpAnimation();

        // Statistics output
        setUpStatistics();
        setUpUserInfo();

        openConnection();
        //clearDatabase();

        lastTimeStamp = System.currentTimeMillis();
    }

    private void clearDatabase()
    {
        db.delete(dbAdapter.TABLE, dbAdapter.COLUMN_ID + " > 3", null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id){
            case R.id.item_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, 1);
                return true;
            case R.id.item_statistics:
                saveToDB();
                Intent intentStats = new Intent(this, StatisticsActivity.class);
                startActivity(intentStats);
                return true;
            case R.id.item_share:
                Intent intentShare = new Intent();
                intentShare.setAction(Intent.ACTION_SEND);
                intentShare.putExtra(Intent.EXTRA_TEXT, collectTextToShare());
                intentShare.setType("text/plain");
                startActivity(
                        Intent.createChooser(intentShare, getResources().getString(R.string.share))
                );
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStop() {
        saveToDB();
        savePrefs();
        super.onStop();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        tvSteps.setText(R.string.stepsNumber);
        tvSteps.append(": ");
        tvSteps.append(Integer.toString(numSteps));
        updateStatistics(1);
    }


    //========================Auxillary methods========================

    private String getCurrentDateStr()
    {
        return new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
    }

    private int getItemValue(String itemName)
    {
        for (StatsItem item : statsItems)
            if (item.getItemName() == itemName)
                return item.getItemValue();
        return 0;
    }

    private void calculateNewStatistics(int stepsCount)
    {
        List<StatsItem> newStatistics = new ArrayList<>();
        for (StatsItem curItem : statsItems)
        {
            if (curItem.getItemName() == getResources().getString(R.string.calories_burned)) {
                int weight = userInfo.getWeight();
                int timePassed = getItemValue(getResources().getString(R.string.time_passed));
                int metersCovered = getItemValue(getResources().getString(R.string.meters_covered));
                double speed = 0.0;
                if (timePassed != 0)
                    speed = metersCovered / timePassed;
                curItem.setItemValue(
                        (0.0035 * weight
                                + Math.pow(speed, 2) / userInfo.getGrowth() * 0.029 * weight)
                        * metersCovered
                );
            }
            else if (curItem.getItemName() == getResources().getString(R.string.meters_covered)) {
                double stepLength = userInfo.getStepLength() / 100.0;
                curItem.setItemValue(curItem.getAccurateItemValue() + stepsCount * stepLength);
            }
            else if (curItem.getItemName() == getResources().getString(R.string.time_passed)) {
                long currentTimeStamp = System.currentTimeMillis();
                if (justAfterPause)
                {
                    lastTimeStamp -= curItem.getItemValue() * 1000;
                    justAfterPause = false;
                }
                curItem.setItemValue((currentTimeStamp - lastTimeStamp)/1000);
            }
            newStatistics.add(curItem.clone());
        }
        statsItems.clear();
        statsItems.addAll(newStatistics);
    }

    private void clearStatistics()
    {
        List<StatsItem> newStatistics = new ArrayList<>();
        for (StatsItem curItem : statsItems)
        {
            curItem.setItemValue(0.0);
            newStatistics.add(curItem.clone());
        }
        statsItems.clear();
        statsItems.addAll(newStatistics);
    }

    private String collectTextToShare()
    {
        String text = getResources().getString(R.string.stepsNumber);
        text += ": " + numSteps + "\n";
        for (StatsItem item : statsItems)
            text += item.getItemName() + ": " + item.getItemValue() + "\n";
        return text;
    }

    private void insertNewEntry()
    {
        ContentValues cv = new ContentValues();
        cv.put(
                DBAdapter.COLUMN_DATE,
                getCurrentDateStr()
        );
        cv.put(
                DBAdapter.COLUMN_STEPS,
                numSteps
        );
        cv.put(
                DBAdapter.COLUMN_CALORIES,
                getItemValue(getResources().getString(R.string.calories_burned))
        );
        cv.put(
                DBAdapter.COLUMN_METERS,
                getItemValue(getResources().getString(R.string.meters_covered))
        );
        cv.put(
                DBAdapter.COLUMN_SECONDS,
                getItemValue(getResources().getString(R.string.time_passed))
        );


        db.insert(DBAdapter.TABLE, null, cv);
        wasReset = false;
    }

    private void nullifyLastSavedValues()
    {
        lastSavedValues = new HashMap<>();
        lastSavedValues.put(dbAdapter.COLUMN_STEPS, 0);
        lastSavedValues.put(dbAdapter.COLUMN_CALORIES, 0);
        lastSavedValues.put(dbAdapter.COLUMN_METERS, 0);
        lastSavedValues.put(dbAdapter.COLUMN_METERS, 0);
    }

    private void openConnection()
    {
        dbAdapter = new DBAdapter(this);
        db = dbAdapter.getWritableDatabase();
    }

    private void resetStatistics()
    {
        clearStatistics();
        adapter.notifyDataSetChanged();
    }

    private void savePrefs()
    {
        SharedPreferences.Editor editor = statsPrefs.edit();
        editor.putInt(STEPS_NUMBER_STR, numSteps).apply();
        editor.putInt(
                CALORIES_STR,
                getItemValue(getResources().getString(R.string.calories_burned)))
                .apply();
        editor.putInt(
                METERS_STR,
                getItemValue(getResources().getString(R.string.meters_covered)))
                .apply();
        editor.putInt(
                SECONDS_STR,
                getItemValue(getResources().getString(R.string.time_passed)))
                .apply();
        editor.putBoolean(WAS_RESET_STR, wasReset).apply();
        editor.putString(WAS_RESET_DATE_STR, getCurrentDateStr());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void saveToDB(){
        Map<String, Object> lastRow = dbAdapter.getLatestRow(db);

        if (lastRow.get(DBAdapter.COLUMN_DATE).equals(getCurrentDateStr()))
            updateLastEntry(lastRow);
        else
            insertNewEntry();
    }

    private void setInitialStatistics()
    {
        statsItems.add(new StatsItem(
                getResources().getString(R.string.calories_burned),
                statsPrefs.getInt(CALORIES_STR, 0)
        ));
        statsItems.add(
                new StatsItem(getResources().getString(R.string.meters_covered),
                        statsPrefs.getInt(METERS_STR, 0)
                ));
        statsItems.add(new StatsItem(
                getResources().getString(R.string.time_passed),
                statsPrefs.getInt(SECONDS_STR, 0)
        ));
    }

    private void setUpAnimation()
    {
        ivSteps = findViewById(R.id.iv_steps);
        ivSteps.setImageResource(R.drawable.steps_animation);
        animSteps = (AnimationDrawable)ivSteps.getDrawable();
    }

    private void setUpBtnStart()
    {
        btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (onPause) {
                    sensorManager.registerListener(
                            MainActivity.this,
                            accel,
                            SensorManager.SENSOR_DELAY_FASTEST
                    );
                    animSteps.start();
                    lastTimeStamp = System.currentTimeMillis();
                    onPause = false;
                    btnStart.setIcon(getResources().getDrawable(R.drawable.pause));
                }
                else
                {
                    stopStepsDetection();
                    btnStart.setIcon(getResources().getDrawable(R.drawable.start));
                    justAfterPause = true;
                }
            }
        });


    }

    private void setUpBtnStop()
    {
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View arg0) {
                stopStepsDetection();
                saveToDB();
                wasReset = true;
                numSteps = 0;
                tvSteps.setText(getResources().getString(R.string.stepsNumber) + ": " + numSteps);
                resetStatistics();
                justAfterPause = false;
                nullifyLastSavedValues();

                onPause = true;
                btnStart.setIcon(getResources().getDrawable(R.drawable.start));
            }
        });

    }

    private void setUpStatistics()
    {
        setInitialStatistics();
        lvStats = findViewById(R.id.lv_stats);
        adapter = new StatsDataAdapter(
                this,
                R.layout.stats_item_layout,
                statsItems
        );
        lvStats.setAdapter(adapter);
    }


    private void setUpStepsDetector()
    {
        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        settingsPrefs = getSharedPreferences(
                getResources().getString(R.string.setting_prefs_name),
                Context.MODE_PRIVATE
        );
        String sensitivityKey = getResources().getString(R.string.sensitivity_key);
        int sensitivity = settingsPrefs.getInt(
                sensitivityKey,
                getResources().getInteger(R.integer.default_sensitivity)
        );
        simpleStepDetector = new StepDetector(
                sensitivity / 10.0
        );
        simpleStepDetector.registerListener(this);
    }

    private void setUpStepsNumber()
    {
        tvSteps = findViewById(R.id.tv_steps);
        numSteps = statsPrefs.getInt(STEPS_NUMBER_STR, 0);
        tvSteps.append(": " + numSteps);
    }

    private void setUpUserInfo()
    {
        int growth = settingsPrefs.getInt(
                getResources().getString(R.string.growth_key),
                getResources().getInteger(R.integer.default_growth)
        );
        int stepLength = settingsPrefs.getInt(
                getResources().getString(R.string.step_length_key),
                getResources().getInteger(R.integer.default_step_length)
        );
        int weight = settingsPrefs.getInt(
                getResources().getString(R.string.weight_key),
                getResources().getInteger(R.integer.default_weight)
        );
        userInfo = new UserInfo(growth, stepLength, weight);
    }

    private void setUpWasResetFlag()
    {
        if (statsPrefs.getString(WAS_RESET_DATE_STR, "").equals(getCurrentDateStr()))
            wasReset = statsPrefs.getBoolean(WAS_RESET_STR,false);
        else
            wasReset = false;
    }

    private void stopStepsDetection()
    {
        sensorManager.unregisterListener(MainActivity.this);
        animSteps.stop();
        onPause = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateLastEntry(Map<String, Object> entryToUpdate)
    {
        if (wasReset)
        {
            entryToUpdate.put(
                    dbAdapter.COLUMN_STEPS,
                    (Integer)entryToUpdate.get(dbAdapter.COLUMN_STEPS)
                            + numSteps
                            - (Integer)lastSavedValues
                            .getOrDefault(dbAdapter.COLUMN_STEPS, 0));
            entryToUpdate.put(
                    dbAdapter.COLUMN_CALORIES,
                    (Integer)entryToUpdate.get(dbAdapter.COLUMN_CALORIES)
                            + getItemValue(getResources().getString(R.string.calories_burned))
                            - (Integer)lastSavedValues
                            .getOrDefault(dbAdapter.COLUMN_CALORIES, 0));
            entryToUpdate.put(
                    dbAdapter.COLUMN_METERS,
                    (Integer)entryToUpdate.get(dbAdapter.COLUMN_METERS)
                            + getItemValue(getResources().getString(R.string.meters_covered))
                            - (Integer)lastSavedValues
                            .getOrDefault(dbAdapter.COLUMN_METERS, 0));
            entryToUpdate.put(
                    dbAdapter.COLUMN_SECONDS,
                    (Integer)entryToUpdate.get(dbAdapter.COLUMN_SECONDS)
                            + getItemValue(getResources().getString(R.string.time_passed))
                            - (Integer)lastSavedValues
                            .getOrDefault(dbAdapter.COLUMN_SECONDS, 0));
            lastSavedValues = entryToUpdate;
        }
        else
        {
            entryToUpdate.put(dbAdapter.COLUMN_STEPS, numSteps);
            entryToUpdate.put(
                    dbAdapter.COLUMN_CALORIES,
                    getItemValue(getResources().getString(R.string.calories_burned))
            );
            entryToUpdate.put(
                    dbAdapter.COLUMN_METERS,
                    getItemValue(getResources().getString(R.string.meters_covered))
            );
            entryToUpdate.put(
                    dbAdapter.COLUMN_SECONDS,
                    getItemValue(getResources().getString(R.string.time_passed))
            );
        }
        dbAdapter.update(entryToUpdate, db);
    }

    private void updateStatistics(int stepsCount)
    {
        calculateNewStatistics(stepsCount);
        adapter.notifyDataSetChanged();
    }
}