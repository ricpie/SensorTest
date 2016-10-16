package com.example.sensortest.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cnbuff410 on 11/8/14.
 */
public class ScanManager {
    /**
     * Member variables
     */
    private static List<ScanListener> sListeners = new ArrayList<ScanListener>();
    private static BluetoothAdapter sBluetoothAdapter;
    private static boolean sScanning = false;

    private static final int BLE_SCAN_REST_PERIOD_MS = 10;
    private static final int BLE_SCAN_SIGNAL_COLLECTION_PERIOD_MS = 500;

    private static BluetoothAdapter.LeScanCallback sLeScanCallback = new BluetoothAdapter
            .LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            for (ScanListener l : sListeners) {
                l.onDevicesScanned(device, rssi, scanRecord);
            }
        }
    };

    private static Runnable endSignalCollection = new Runnable() {
        @Override
        public void run() {
            ScanManager.stopScan();
            new Handler().postDelayed(restartScanRunnable, BLE_SCAN_REST_PERIOD_MS);
        }
    };

    private static  Runnable restartScanRunnable = new Runnable() {
        @Override
        public void run() {
            ScanManager.startScan();
        }
    };

    public interface ScanListener {
        public void onScanStarted();

        public void onScanStopped();

        /**
         * The callback is fired each time the valid temperature data was obtained from remote BLE sensor
         */
        public void onDevicesScanned(final BluetoothDevice device, final int rssi,
                                     final byte[] scanRecord);
    }

    public static void setAdapter(BluetoothAdapter adapter) {
        sBluetoothAdapter = adapter;
    }

    public static void addListener(final ScanListener l) {
        if (!sListeners.contains(l)) {
            sListeners.add(l);
        }
    }

    public static void removeListener(final ScanListener l) {
        sListeners.remove(l);
    }

    /**
     * Starts scanning for temperature data. Call {@link #stopScan()} when done to save the power.
     */
    public static void startScan() {

        constructScanRestartRunnable();

        // Stops scanning after a pre-defined scan period.
        if (sBluetoothAdapter == null) return;
        if (sScanning) return;

        sBluetoothAdapter.startLeScan(
                sLeScanCallback
        );
        sScanning = true;
        for (ScanListener l : sListeners)
            l.onScanStarted();

        new Handler().postDelayed(endSignalCollection, BLE_SCAN_SIGNAL_COLLECTION_PERIOD_MS);
    }

    /**
     * Stops scanning for temperature data from BLE sensors.
     */
    public static void stopScan() {
        if (sBluetoothAdapter == null) {
            return;
        }
        sBluetoothAdapter.stopLeScan(sLeScanCallback);
        for (ScanListener l : sListeners)
            l.onScanStopped();
        sScanning = false;
    }

    public static void fullStopScan(){
        restartScanRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("SCAN_MNGR", "BLE Scanning has stopped");
            }
        };
    }

    private static void constructScanRestartRunnable(){
        restartScanRunnable = new Runnable() {
            @Override
            public void run() {
                ScanManager.startScan();
            }
        };
    }
}
