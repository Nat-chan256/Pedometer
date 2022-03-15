package com.kubgu.moskovka.pedometer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kubgu.moskovka.pedometer.data.UserInfo;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    private TextView tvSensitivity;
    private SeekBar seekBar;
    private final int DEFAULT_SENSITIVITY = 8;
    private String sensitivityStr = "sensitivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(
                getResources().getString(R.string.setting_prefs_name),
                Context.MODE_PRIVATE
        );

        setUpSeekBar();
        setUpGrowthParameter();
        setUpWeightParameter();
        setUpStepLengthParameter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.contains(sensitivityStr))
            seekBar.setProgress(prefs.getInt(sensitivityStr, DEFAULT_SENSITIVITY));
    }

    @Override
    protected void onStop() {
        // Save user's settings
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(sensitivityStr, seekBar.getProgress()).apply();

        super.onStop();
    }


    //==========================Set-up methods========================

    private void setSeekBarColor()
    {
        int color =  getResources().getColor(R.color.lime);
        seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    private void setUpGrowthParameter()
    {
        TextView tvGrowth = findViewById(R.id.tv_growth);
        tvGrowth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog(
                        1,
                        250,
                        getResources().getString(R.string.growth_key),
                        getResources().getString(R.string.santimeters)
                );
            }
        });
    }

    private void setUpSeekBar()
    {
        seekBar = findViewById(R.id.sbSensetivity);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSensitivity.setText(getResources().getString(R.string.sensetivity));
                tvSensitivity.append(": ");
                tvSensitivity.append(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Intent intent = new Intent();
                intent.putExtra(sensitivityStr, seekBar.getProgress());
                setResult(RESULT_OK, intent);
            }
        });
        tvSensitivity = findViewById(R.id.tv_sensitivity);
        tvSensitivity.append(": ");
        tvSensitivity.append(String.valueOf(prefs.getInt(
                getResources().getString(R.string.sensitivity_key),
                getResources().getInteger(R.integer.default_sensitivity)))
        );
        setSeekBarColor();
    }

    private void setUpStepLengthParameter()
    {
        TextView tvStepLength = findViewById(R.id.tv_step_length);
        tvStepLength.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog(
                        1,
                        150,
                        getResources().getString(R.string.step_length_key),
                        getResources().getString(R.string.santimeters)
                );
            }
        });
    }

    private void setUpWeightParameter()
    {
        TextView tvWeight = findViewById(R.id.tv_weight);
        tvWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog(
                        1,
                        300,
                        getResources().getString(R.string.weight_key),
                        getResources().getString(R.string.kilograms)
                );
            }
        });
    }

    //================================Utility methods===============================


    private int getDefaultValue(String key)
    {
        if (key == getResources().getString(R.string.growth_key))
            return getResources().getInteger(R.integer.default_growth);
        else if (key == getResources().getString(R.string.weight_key))
            return getResources().getInteger(R.integer.default_weight);
        else if (key == getResources().getString(R.string.step_length_key))
            return getResources().getInteger(R.integer.default_step_length);
        return 0;
    }

    private void openDialog(int minBound, int maxBound, String key, String unit)
    {
        final LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(
                R.layout.number_picker_dialog, null
        );
        NumberPicker numberPicker = linearLayout.findViewById(R.id.numberPicker);
        numberPicker.setMinValue(minBound);
        numberPicker.setMaxValue(maxBound);
        numberPicker.setValue(prefs.getInt(key, getDefaultValue(key)));


        TextView tvUnit = linearLayout.findViewById(R.id.tv_unit);
        tvUnit.setText(unit);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setPositiveButton(getResources().getString(R.string.submit), null)
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .setView(linearLayout)
                .setCancelable(false)
                .create();
        dialog.show();

        // Set onClick listeners
        dialog
                .getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int value = numberPicker.getValue();

                // Create intent
                Intent intent = new Intent();
                intent.putExtra(key, value);
                setResult(RESULT_OK, intent);
                // Save preference
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(key, value).apply();

                dialog.dismiss();
            }
        });
    }
}