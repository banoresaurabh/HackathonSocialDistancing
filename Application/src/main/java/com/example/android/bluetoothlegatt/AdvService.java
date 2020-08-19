package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.*;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class AdvService extends Service {

    AdvertisingSet currentAdvertisingSet;

    String LOG_TAG = "SOCIAL_D_BLE_EMMITER";
    private AdvertiseData mAdvertiseData;
    private AdvertiseSettings mAdvertiseSettings;
    String currentUUID;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate() {
        Toast.makeText(this, "Service created!", Toast.LENGTH_LONG).show();
        startBLEEmmition();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void startBLEEmmition() {


        BluetoothLeAdvertiser advertiser =
                BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        setAdvertiseData();
        setAdvertiseSettings();

        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.i(LOG_TAG, "onAdvertisingStartSuccess() : currentUUID :" + currentUUID);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.i(LOG_TAG, "Advertising onStartFailure: " + errorCode);
                super.onStartFailure(errorCode);
            }
        };

        Toast.makeText(this, "adv started!", Toast.LENGTH_LONG).show();
        advertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, advertiseCallback);

    }


    public byte[] newUUID() {
        UUID uuid = UUID.fromString("7ff80eb3-9783-48ad-ad87-869a3df50e10");
        System.out.println("yesss");
        currentUUID = uuid.toString();
        long hi = uuid.getMostSignificantBits();
        long lo = uuid.getLeastSignificantBits();
        System.out.println(hi);
        System.out.println(lo);
        return ByteBuffer.allocate(16).putLong(hi).putLong(lo).array();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void setAdvertiseData() {
        AdvertiseData.Builder mBuilder = new AdvertiseData.Builder();
        ByteBuffer mManufacturerData = ByteBuffer.allocate(24);
        byte[] uuid = newUUID();
        mManufacturerData.put(0, (byte)0xBE); // Beacon Identifier
        mManufacturerData.put(1, (byte)0xAC); // Beacon Identifier
        for (int i=2; i<=17; i++) {
            mManufacturerData.put(i, uuid[i-2]); // adding the UUID
        }
        mManufacturerData.put(18, (byte)0x00); // first byte of Major
        mManufacturerData.put(19, (byte)0x09); // second byte of Major
        mManufacturerData.put(20, (byte)0x00); // first minor
        mManufacturerData.put(21, (byte)0x06); // second minor
        mManufacturerData.put(22, (byte)0xB5); // txPower
        mBuilder.addManufacturerData(224, mManufacturerData.array()); // using google's company ID
        mAdvertiseData = mBuilder.build();
    }

    public static byte[] getIdAsByte(java.util.UUID uuid)
    {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void setAdvertiseSettings() {
        AdvertiseSettings.Builder mBuilder = new AdvertiseSettings.Builder();
        mBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        mBuilder.setConnectable(false);
        mBuilder.setTimeout(0);
        mBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
        mAdvertiseSettings = mBuilder.build();
    }
}