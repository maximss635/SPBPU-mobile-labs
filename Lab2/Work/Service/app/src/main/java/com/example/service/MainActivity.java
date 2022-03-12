package com.example.service;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView textLabel;

    private ArrayAdapter<String> adapter;
    public static volatile List<String> lastContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lastContacts = new ArrayList<>(10);
        adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item,
                lastContacts);

        ((ListView)findViewById(R.id.list_view)).setAdapter(adapter);

        // Check permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) !=
                    PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, 10);
        }

        findViewById(R.id.button_start_service).setOnClickListener(this);
        findViewById(R.id.button_stop_service).setOnClickListener(this);

        textLabel = (TextView)findViewById(R.id.text_label);
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, BackupService.class);

        switch (v.getId()) {
            case R.id.button_start_service:
                if (!BackupService.isRunning()) {
                    startService(intent);
                    textLabel.setText("Service is running");
                }
                else {
                    textLabel.setText("Service is already running");
                }
                break;

            case R.id.button_stop_service:
                if (BackupService.isRunning()) {
                    stopService(intent);
                    textLabel.setText("Service is not running");
                }
                else {
                    textLabel.setText("Service is not running yet");
                }
                break;
        }

        adapter.notifyDataSetChanged();
    }
}
