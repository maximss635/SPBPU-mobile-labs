package com.example.wifiscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class WifiScannerActivity extends AppCompatActivity {

    private final List<String> availableWifiNetworks = new ArrayList<>();
    private final Map<String, ScanResult> availableWifiNetworksInfo = new TreeMap<>();

    private WifiManager wifiManager;
    private ArrayAdapter<String> adapter;

    private final int PERMISSION_REQ_CODE = 123;

    private void onScanSuccess() {
        Log.d("wifi-scan", "success");

        availableWifiNetworks.clear();
        List<ScanResult> scanResults = wifiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
            Log.d("detected network", scanResult.SSID);

            availableWifiNetworks.add(scanResult.SSID);
            availableWifiNetworksInfo.put(scanResult.SSID, scanResult);
        }

        adapter.notifyDataSetChanged();
    }

    private void onScanFailure() {
        Log.w("wifi-scan", "failure");
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            Log.d("debug", "onReceive");

            boolean success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);

            if (success) {
                onScanSuccess();
            } else {
                onScanFailure();
            }

            unregisterReceiver(this);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanWifi();
            } else {
                Log.w("Permission", "denied");
            }
        }
    }

    private void getPermission() {
        List<String> permissionsList = new ArrayList<>(Arrays.asList(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ));

        permissionsList.removeIf(permission ->
                ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED);

        if (permissionsList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[0]),
                    PERMISSION_REQ_CODE);
        }
    }

    private void scanWifi() {
        registerReceiver(broadcastReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        boolean success = wifiManager.startScan();
        Toast.makeText(this, "scanning...", Toast.LENGTH_LONG).show();

        if (success) {
            onScanSuccess();
        } else {
            onScanFailure();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();

        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        adapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, availableWifiNetworks);

        ListView listView = findViewById(R.id.main_list_view);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener( (adapterView, view, i, l) -> {
            String networkName = availableWifiNetworks.get(i);
            ScanResult networkInfo = availableWifiNetworksInfo.get(networkName);

            Log.d("listener", "onItemClick " + networkInfo);

            new AlertDialog.Builder(this)
                    .setMessage("SSID: " + networkInfo.SSID + "\n" +
                                "BSSID: " + networkInfo.BSSID + "\n" +
                                "Level: " + networkInfo.level + "\n" +
                                "Frequency: " + networkInfo.frequency)
                    .create().show();

        });

        findViewById(R.id.button_scan).setOnClickListener( view -> scanWifi() );

        scanWifi();
    }
}