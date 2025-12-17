package com.stellar.runner;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class tileSetting extends Activity {

    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_NO) == Configuration.UI_MODE_NIGHT_NO)
            setTheme(android.R.style.Theme_DeviceDefault_Light_Dialog);
        setContentView(R.layout.pop);
        setTitle("设置磁帖点击命令");
        EditText e = findViewById(R.id.e);
        e.setHint("点击磁帖后会后台执行此命令");
        sp = getSharedPreferences("tile", 0);
        e.setText(sp.getString("content", null));
        e.requestFocus();
        e.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(e, 0);
            }
        }, 400);

        ImageButton imageButton = findViewById(R.id.ib);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sp.edit().putString("content", e.getText().toString()).apply();
                Toast.makeText(tileSetting.this, "已保存命令，点击磁帖即可执行此命令。", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout linearLayout = findViewById(R.id.ll);
        linearLayout.setVisibility(sp.getBoolean("switch", false) ? View.VISIBLE : View.GONE);
        CheckBox checkBox = findViewById(R.id.ch);
        checkBox.setVisibility(View.VISIBLE);
        checkBox.setChecked(sp.getBoolean("switch", false));
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                linearLayout.setVisibility(b ? View.VISIBLE : View.GONE);
                sp.edit().putBoolean("switch", b).apply();
            }
        });
        EditText e1 = findViewById(R.id.et);
        e1.setHint("磁帖关闭时会后台执行此命令");
        e1.setText(sp.getString("content1",null));
        ImageButton imageButton1 = findViewById(R.id.ib1);
        imageButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sp.edit().putString("content1", e1.getText().toString()).apply();
                Toast.makeText(tileSetting.this, "已保存命令，关闭磁帖即可执行此命令。", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
