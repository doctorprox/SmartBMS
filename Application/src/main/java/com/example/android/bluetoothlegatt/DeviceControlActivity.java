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

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;


    private HorizontalBarChart cellvchart;



    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private EditText editTextMessage;
    private Button buttonSend;
    private Button buttonON, buttonOFF, buttonEXIT;

    private String receiveBuffer = "";
    private byte[] rxdata;  // used for message parsing
    private byte[] rxdatatemp;  // used for incomplete transmissions
    private  byte soc = 0;
    private  float voltage = 0;
    private  int power = 0;
    private  float remcap = 0;
    private  float temp1 = 0;
    private  float current = 0;
    private  float[] cellv;

    private Timer mTimer1;
    private TimerTask mTt1;

    bms_data adapter;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            rxdata  = new byte[0];
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void messageHandler() {
        //switch (receiveBuffer[])
        if (receiveBuffer != null) {

            String hexarray = "";
            for(int i=0;i<rxdata.length;i++)
            {
                int a = ((int)rxdata[i] & 0xFF);
                hexarray += String.format("%x",a) + " ";
            }

            Log.d(TAG,hexarray);
            if(rxdata[2] != 0){
                return;
            }
            switch (rxdata[1]){
                // add 4 to xls stuff
                case 3:
                    soc = (byte)(rxdata[23] & 0xFF);
                    voltage = (rxdata[4] & 0xFF) * 256 + (rxdata[5] & 0xFF);
                    voltage/= 100;
                    current = (rxdata[6] & 0xFF) * 256 + (rxdata[7] & 0xFF);
                    current/= 100;
                    remcap = (rxdata[8] & 0xFF) * 256 + (rxdata[9] & 0xFF);
                    remcap/= 100;
                    temp1 = (rxdata[27] & 0xFF) * 256 + (rxdata[28] & 0xFF);
                    temp1 -= 2731;
                    temp1 /= 10;
                    power = (int) (voltage * current);
                    /*
                    Log.d(TAG,"SoC = " + Byte.toString(soc));
                    Log.d(TAG,"V = " + Float.toString(voltage));
                    Log.d(TAG,"I = " + Float.toString(current));
                    Log.d(TAG,"P = " + Integer.toString(power));
                    Log.d(TAG,"Ah = " + Float.toString(remcap));
                    Log.d(TAG,"T = " + Float.toString(temp1));

                     */
                    adapter.setRemCap(remcap);
                    adapter.setPower(power);
                    adapter.setSoC(soc);
                    adapter.setCurrent(current);
                    adapter.setTemp1(temp1);
                    adapter.setV(voltage);
                    adapter.notifyDataSetChanged();
                    break;
                case 4:
                    int numcells = rxdata[3] / 2;
                    cellv = new float[numcells];
                    List<BarEntry> entries = new ArrayList<>();
                    for(int i=0; i<numcells; i++){
                        float tempv;
                        tempv = rxdata[4+(i*2)] * 256;
                        tempv +=  rxdata[5+(i*2)];
                        cellv[i] = tempv;
                        Log.d(TAG,"Cell  " + Integer.toString(i+1) + " : " + Float.toString(cellv[i]));
                        entries.add(new BarEntry(i, cellv[i]/1000));
                    }

                    BarDataSet set = new BarDataSet(entries, "BarDataSet");
                    BarData data = new BarData(set);
                    data.setBarWidth(0.9f); // set custom bar width
                    data.setHighlightEnabled(false);
                    
                    final String[] quarters = new String[] { "C1", "C2", "C3", "C4","C5","C6", "C7", "C8", "C9", "C10","C11","C12","C13", "C14"};
                    ValueFormatter formatter = new ValueFormatter() {
                        @Override
                        public String getAxisLabel(float value, AxisBase axis) {
                            return "C" + String.format("%.0f",value+1);
                        }
                    };
                    XAxis xAxis = cellvchart.getXAxis();
                    xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setValueFormatter(formatter);
                    xAxis.setLabelCount(entries.size());
                    cellvchart.getDescription().setEnabled(false);
                    cellvchart.setScaleYEnabled(false);
                    cellvchart.getLegend().setEnabled(false);


                    YAxis yl = cellvchart.getAxisRight();
                    yl.setDrawAxisLine(true);
                    yl.setDrawGridLines(false);
                    yl.setAxisMinimum(3.0f); // this replaces setStartAtZero(true)
                    yl.setAxisMaximum(4.25f); // this replaces setStartAtZero(true)

                    YAxis y2 = cellvchart.getAxisLeft();
                    y2.setDrawAxisLine(true);
                    y2.setDrawGridLines(true);
                    y2.setAxisMinimum(3.0f); // this replaces setStartAtZero(true)
                    y2.setAxisMaximum(4.25f); // this replaces setStartAtZero(true)

                    cellvchart.setData(data);
                   // cellvchart.setFitBars(true); // make the x-axis fit exactly all bars

                    cellvchart.invalidate(); // refresh
                    break;
            }
            rxdata = new byte[0];
        }
    }

    private void stopTimer(){
        if(mTimer1 != null){
            mTimer1.cancel();
            mTimer1.purge();
        }
    }

    private void startTimer(){
        mTimer1 = new Timer();
        mTt1 = new TimerTask() {
            public void run() {
                if(mConnected) {
                    mBluetoothLeService.request03();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    mBluetoothLeService.request04();
                } else{
                    stopTimer();
                }
            };

        };

        mTimer1.schedule(mTt1, 0, 2000);
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();

                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                Toast.makeText(getApplicationContext(),"Disconnected",Toast.LENGTH_SHORT).show();
                clearUI();
                stopTimer();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

                mBluetoothLeService.request03();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.request03();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.request03();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mBluetoothLeService.request03();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startTimer();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
               // Log.d(TAG,"RX\n");

                receiveBuffer += intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                rxdatatemp = intent.getByteArrayExtra(BluetoothLeService.DATA_BYTES);
                //Log.e(TAG,receiveBuffer);
                if(rxdatatemp[0] == 0xDD) {
                    rxdata = rxdatatemp;
                } else{
                    if(rxdatatemp.length > 1) {
                        rxdata = concatenateByteArrays(rxdata, rxdatatemp);
                    }
                }

                if(rxdatatemp[rxdatatemp.length-1] ==0x77) {
                    //receiveBuffer = receiveBuffer.substring(0, receiveBuffer.length() - 1);
                    messageHandler();
                    receiveBuffer = "";
                }
            }
        }
    };

    byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
       // Log.d(TAG,"LENGTHS : " + Integer.toString(a.length) + " " + Integer.toString(b.length));
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };

    private void clearUI() {
        //mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);

        cellvchart = (HorizontalBarChart) findViewById(R.id.cellvchart);


        ArrayList<String> animalNames = new ArrayList<>();
        animalNames.add("Voltage");
        animalNames.add("SoC");
        animalNames.add("Remaining");
        animalNames.add("Current");
        animalNames.add("Power");
        animalNames.add("Temp");

        ArrayList<String> animaldata = new ArrayList<>();
        animaldata.add("-");
        animaldata.add("-");
        animaldata.add("-");
        animaldata.add("-");
        animaldata.add("-");
        animaldata.add("-");


        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.view_stats);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        adapter = new bms_data(this, animalNames,animaldata);
        recyclerView.setAdapter(adapter);





        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Onresume Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        stopTimer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                item.setActionView(R.layout.actionbar_indeterminate_progress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                //custom code
                if(uuid.equals("0000ff01-0000-1000-8000-00805f9b34fb") && mNotifyCharacteristic == null) {
                    mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                    mNotifyCharacteristic = gattCharacteristic;
                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
