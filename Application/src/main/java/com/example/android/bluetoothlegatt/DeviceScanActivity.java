/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Vibrator;


import java.util.ArrayList;
import java.util.HashMap;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private ArrayList<String> whiteListedDevices;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 29 minutes.
    private static final long SCAN_PERIOD = 1740000 ;

    public static Activity globalContext = null;
    DbUtility dbUtility;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        initDevicesList();
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
        if (result == 0){
            // Use this check to determine whether BLE is supported on the device.  Then you can
            // selectively disable BLE-related features.
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                finish();
            }

            // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
            // BluetoothAdapter through BluetoothManager.
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();

            // Checks if Bluetooth is supported on the device.
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            scanLeDevice(true);

        }

    }

    public void initDevicesList(){
        dbUtility = new DbUtility(this);
        Cursor res = dbUtility.getCred();
        whiteListedDevices = new ArrayList<String>();

        res.moveToFirst();
        while(!res.isAfterLast()) {
            whiteListedDevices.add(res.getString(res.getColumnIndex("device_mac_id")));
            res.moveToNext();
        }
        Log.d("DatabaseHandler", Integer.toString(whiteListedDevices.size()));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        scanLeDevice(false);
//        mLeDeviceListAdapter.clear();
//    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    scanLeDevice(true);
                    //invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    public class DeviceHolder{
        private BluetoothDevice device;
        private int rssi;
        private double deviceDistance;


        public DeviceHolder(BluetoothDevice device, int rssi, double deviceDistance){
            this. device = device;
            this.rssi = rssi;
            this.deviceDistance = deviceDistance;
        }
        public String getAddress() {
            return device.getAddress();
        }
        public String getName() {
            return device.getName();
        }
        public void setRSSI(int rssi){
            this.rssi = rssi;
        }
        public int getRSSI() {
            return rssi;
        }
        public void setDistance(double deviceDistance){
            this.deviceDistance = deviceDistance;
        }
        public double getDistance() {
            return deviceDistance;
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<DeviceHolder> mLeHolders;
        private HashMap<String, DeviceHolder> mLeDevicesRssi;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();

            mLeHolders = new ArrayList<DeviceHolder>();
            mLeDevicesRssi = new HashMap<String, DeviceHolder>();
        }

        public void addDevice(DeviceHolder deviceHolder, int rssi, double deviceDistance) {
            String address = deviceHolder.device.getAddress();
            if(!mLeDevices.contains(deviceHolder.device)) {
                DeviceHolder cDeviceHolder = new DeviceHolder(deviceHolder.device, deviceHolder.rssi, deviceHolder.deviceDistance);
                cDeviceHolder.setRSSI(rssi);
                cDeviceHolder.setDistance(deviceDistance);
                mLeDevicesRssi.put(address, cDeviceHolder);
                mLeDevices.add(cDeviceHolder.device);
                mLeHolders.add(cDeviceHolder);
            }
            else if(mLeDevices.contains(deviceHolder.device)) {
                mLeDevicesRssi.get(address).setRSSI(rssi);
                mLeDevicesRssi.get(address).setDistance(deviceDistance);
            }
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRSSI = (TextView) view.findViewById(R.id.device_rssi);
                viewHolder.deviceDistance = (TextView) view.findViewById(R.id.device_distance);

                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }



            final BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();

            final Button whiteListBtn = (Button) view.findViewById(R.id.whitelist_btn);
            whiteListBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(whiteListedDevices.contains(device.getAddress().toString())){
                        whiteListBtn.setText("Add to Whitelist");
                        dbUtility.delete(device.getAddress().toString());
                        Log.d("DatabaseHandler", "Deleted the device with mac_id" + device.getAddress().toString());
                    }else{
                        whiteListBtn.setText("Remove From Whitelist");
                        dbUtility.insert(device.getAddress().toString());
                        Log.d("DatabaseHandler", "Added the device with mac_id" + device.getAddress().toString());
                    }
                    initDevicesList();
                }

            });

            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceRSSI.setText(Integer.toString(mLeDevicesRssi.get(device.getAddress()).rssi));
            viewHolder.deviceDistance.setText(Double.toString(mLeDevicesRssi.get(device.getAddress()).deviceDistance));
            if (whiteListedDevices.contains(device.getAddress().toString())) {
                whiteListBtn.setText("Remove From Whitelist");
            } else {
                whiteListBtn.setText("Add to Whitelist");
            }

            return view;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    DeviceHolder deviceHolder = new DeviceHolder(device, rssi, getDistance(rssi, (int) scanRecord[29]));
                    final int new_rssi = rssi;
                    runOnUiThread(new DeviceAddTask( deviceHolder, new_rssi) );
                }
            };

    class DeviceAddTask implements Runnable {
        DeviceHolder deviceHolder;

        public DeviceAddTask( DeviceHolder deviceHolder, int rssi) {
            this.deviceHolder = deviceHolder;
            this.deviceHolder.rssi = rssi;
            this.deviceHolder.deviceDistance = deviceHolder.deviceDistance;
        }

        public void run() {

            if(deviceHolder.rssi > -50 && !whiteListedDevices.contains(deviceHolder.device.getAddress())) {
            //if(deviceHolder.deviceDistance < 300) {

//                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
//                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
//                toneGen1.release();
//                toneGen1 = null;
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mLeDeviceListAdapter.addDevice(deviceHolder, deviceHolder.rssi, deviceHolder.deviceDistance);
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    }

    double getDistance(int rssi, int txPower) {
        /*
         * RSSI = TxPower - 10 * n * lg(d)
         * n = 2 (in free space)
         *
         * d = 10 ^ ((TxPower - RSSI) / (10 * n))
         */

        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRSSI;
        TextView deviceDistance;
    }

}