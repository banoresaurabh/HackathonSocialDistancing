package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class BLService extends Service {
    private DeviceScanActivity.DeviceAddTask addTask;
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1740000 ;

    public static Activity globalContext = null;

    public Context context = this;
    public Handler handler = null;
    public static Runnable runnable = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "Service created!", Toast.LENGTH_LONG).show();


        mHandler = new Handler();
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();


        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }


        System.out.println("created");

        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                System.out.println("continue");

                Toast.makeText(context, "Service is still running", Toast.LENGTH_LONG).show();
                handler.postDelayed(runnable, 10000);
            }
        };

        handler.postDelayed(runnable
                , 15000);
    }
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            System.out.println("test");
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
    }
    public class DeviceHolder{
        private BluetoothDevice device;
        private int rssi;


        public DeviceHolder(BluetoothDevice device, int rssi){
            this. device = device;
            this.rssi = rssi;
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
            mLeHolders = new ArrayList<DeviceHolder>();
            mLeDevicesRssi = new HashMap<String, DeviceHolder>();
        }

        public void addDevice(DeviceHolder deviceHolder, int rssi) {
            String address = deviceHolder.device.getAddress();
            if(!mLeDevices.contains(deviceHolder.device)) {
                DeviceHolder cDeviceHolder = new DeviceHolder(deviceHolder.device, deviceHolder.rssi);
                cDeviceHolder.setRSSI(rssi);
                mLeDevicesRssi.put(address, cDeviceHolder);
                mLeDevices.add(cDeviceHolder.device);
                mLeHolders.add(cDeviceHolder);
            }
            else if(mLeDevices.contains(deviceHolder.device)) {
                mLeDevicesRssi.get(address).setRSSI(rssi);
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
            DeviceScanActivity.ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new DeviceScanActivity.ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRSSI = (TextView) view.findViewById(R.id.device_rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (DeviceScanActivity.ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();

            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceRSSI.setText(Integer.toString(mLeDevicesRssi.get(device.getAddress()).rssi));

            return view;
        }
    }
    class DeviceAddTask implements Runnable {
        DeviceHolder deviceHolder;

        public DeviceAddTask(DeviceHolder deviceHolder, int rssi ) {
            this.deviceHolder = deviceHolder;
            this.deviceHolder.rssi = rssi;
        }

        public void run() {

            if(deviceHolder.rssi > -50 ) {
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
            mLeDeviceListAdapter.addDevice(deviceHolder, deviceHolder.rssi);
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    }
    public BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    System.out.println("kuch"+device.getAddress()+device.getName()+" Rssi"+rssi);

                    DeviceHolder deviceHolder = new DeviceHolder(device, rssi);
                    final int new_rssi = rssi;
                    DeviceAddTask a=new DeviceAddTask( deviceHolder, new_rssi );
                    Thread t1 = new Thread(a);
                    t1.start();
                }
            };

    @Override
    public void onDestroy() {
        /* IF YOU WANT THIS SERVICE KILLED WITH THE APP THEN UNCOMMENT THE FOLLOWING LINE */
        //handler.removeCallbacks(runnable);
        scanLeDevice(false);
        //Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStart(Intent intent, int startid) {
        scanLeDevice(true);
        onTaskRemoved(intent);
        //Toast.makeText(this, "Service started in background.", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        onDestroy();
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }
}