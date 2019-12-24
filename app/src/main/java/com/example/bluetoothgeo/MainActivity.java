package com.example.bluetoothgeo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.example.android.common.logger.Log;

public class MainActivity extends AppCompatActivity {
    Charset mCharSet = StandardCharsets.UTF_8;

    BluetoothAdapter mBluetoothAdapter;
    TextView mConnectionState;
    TextView DeviceName;
    TextView Distance;
    //TextView Angle;
    Toolbar toolbar;
    TextView mDataField;
    TextView Address;
    int tx_power = -50;
    int mDeviceRssi = 999;
    float[] valuesAccel = new float[3];
    float[] valuesGyro = new float[3];
    float[] valuesMag = new float[3];
    float[] orientation = new float[3];
    float[] last_orientation = new float[3];
    private boolean isAccel = false;
    private boolean isGyro = false;
    private boolean isMag = false;
    private double mInitTime;
    private double sensTime;
    Timer timer;
    private boolean isSensing = false;

    //FloatingActionButton pairdSearch;
    private Button mAdvertiseButton;
    private Button mDiscoverButton;
    private Button mSendButton;
    private Button mSensorButton;
    private Button mClearButton;
    private Button mCalibrateButton;
    private Button mConnectButton;
    private String mDeviceAddress;
    FloatingActionButton searchButton;

    private ScanCallback mScanCallback;
    private AdvertiseCallback mAdvertiseCallback;

    ArrayList<BluetoothDevice> Devices = new ArrayList<>();
    ArrayList<String> Addresses = new ArrayList<>();
    ArrayList<String> DeviceNames = new ArrayList<>();
    ArrayList<Short> Rssis = new ArrayList<>();
    private final BroadcastReceiver broadcastReceiver;
    private BluetoothLeService mBluetoothLeService;
    Intent mgattServiceIntent;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;

    private String mConnectedDeviceName = "null";
    private String mOutString = " ";
    ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    private BluetoothChatService mChatService = null;

    private static UUID UUID_SERVER = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb");

    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler1 = new Handler();
    private int messageCount = 0;
	@SuppressLint("UseSparseArrays")
    //public Map<Integer, ArrayList<String>> messageMap = new HashMap<>();
    public Map<String, Map> deviceMap = new HashMap<>();
	int BYTES_IN_CONTINUE_PACKET;
    BluetoothLeAdvertiser mbluetoothLeAdvertiser;
    SensorManager sensorManager;
    Sensor sensorAccel;
    Sensor sensorGyro;
    Sensor sensorMag;
    int sensorAccuracy = -3;
    boolean isRotate = true;

    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);

    public MainActivity() {
        broadcastReceiver = new BroadcastReceiver() {

            public void onReceive(final Context context, Intent intent) {
                String action = intent.getAction();

                Log.i("Action ", action);
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Log.i("DISCOVERY_STARTED", "1");
                }
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) && !searchButton.isEnabled()) {
                    Log.i("DISCOVERY_FINISHED", "1");
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Choose a device");
                    builder.setItems(DeviceNames.toArray(new String[0]), builder1click);
                    AlertDialog dialog = builder.create();
                    dialog.show();

                    searchButton.setEnabled(true);
                }
                else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    Log.i("Found", "Found");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String name = "null";
                    Short rssi = null;
                    if (device != null) {
                        rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                        Devices.add(device);

                        if (device.getName() == null){
                            name = device.getAddress();}
                        else{
                            name = device.getName();
                        }
                        DeviceNames.add(name);
                        Rssis.add(rssi);
                    }
                    Toast.makeText(MainActivity.this, "Found Device "+ name + " with rssi " + rssi
                            , Toast.LENGTH_SHORT).show();

                    Log.i("Found", "Finished");
                    //Snackbar.make(this, "start searching", Snackbar.LENGTH_LONG).show();

                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    Log.i("Gatt", "5");
                    String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                    if (data != null) {
                        mDataField.setText(data);
                    }
                }
                if (BluetoothLeService.ACTION_RSSI_AVAILABLE.equals(action)){
                    Log.i("Gatt", "6");
                    readRssiBle();
                }
                else{
                    Log.i("Gatt", action);
                }

            }

        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("onCreate", "begin");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mConversationView = findViewById(R.id.in);
        mOutEditText = findViewById(R.id.edit_text_out);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DeviceName = findViewById(R.id.device);
        Distance = findViewById(R.id.distance);
        //Angle = findViewById(R.id.angel);
        searchButton = findViewById(R.id.fab);
        //pairdSearch = findViewById(R.id.fab1);
        Address = findViewById(R.id.device_address);
        mConnectionState = findViewById(R.id.connection_state);
        mDataField = findViewById(R.id.data_value);

        mDiscoverButton = findViewById( R.id.bleScan );
        mAdvertiseButton = findViewById( R.id.advertise );
        mClearButton = findViewById(R.id.clear);
        mCalibrateButton = findViewById(R.id.calibrate);
        mConnectButton = findViewById(R.id.connect);
        mSendButton = findViewById(R.id.button_send);
        mSensorButton = findViewById(R.id.button_sensor);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mbluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeService = new BluetoothLeService();
        clearUI();
        checkBlueTooth();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Log", "start searching");
                searchButton.setEnabled(false);
                Devices.clear();
                mBluetoothAdapter.startDiscovery();
                Snackbar.make(view, "start searching", Snackbar.LENGTH_LONG).show();
            }
        });

        mAdvertiseButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                TextView textView = findViewById(R.id.edit_text_out);
                String message = textView.getText().toString();
                if(!message.equals("")){
                    mOutString = message;
                    mOutStringBuffer.setLength(0);
                    mOutEditText.setText(mOutStringBuffer);
                }
                stopAdvertising();
				messageCount += 1;
				//construtHeadPacket();
                sendByAdvertise(mOutString);
                showText("Me: " + mOutString);
            }
        });

        mClearButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                mConversationArrayAdapter.clear();
                stopAdvertising();
                stopScanning();
            }
        });

        mConnectButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                connectDevice();
            }
        });

        mCalibrateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                tx_power = mDeviceRssi;
                float dis = (float) Math.pow(10, (tx_power - mDeviceRssi) / (10 * 2.5));
                String distanceS = new DecimalFormat("0.00").format(dis) + "m";
                Distance.setText(distanceS);
            }
        });

        mDiscoverButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                blescan();
            }
        });

        mSensorButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (!isSensing) {
                    mSensorButton.setText(R.string.sensor1);
                    mInitTime = System.currentTimeMillis();
                    isSensing = true;

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            mInitTime = System.currentTimeMillis();
                        }
                    }, 5000);

                } else {
                    mSensorButton.setText(R.string.sensor);
                    isSensing = false;
                }

            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_RSSI_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_RESPONSE_TO_CLIENT);

        mgattServiceIntent = new Intent(MainActivity.this , BluetoothLeService.class);
        if (!mBluetoothLeService.initialize(mBluetoothAdapter, MainActivity.this, mBluetoothManager)) {
            Log.e("service error:", "Unable to initialize Bluetooth");
        }

        registerReceiver(broadcastReceiver, intentFilter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(SensorListener, sensorAccel, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(SensorListener, sensorGyro, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(SensorListener, sensorMag, SensorManager.SENSOR_DELAY_FASTEST);

        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isSensing) {
                            sensTime = (System.currentTimeMillis() - mInitTime) / 1000;
                            //if (isRotate){
                            //    showInfo();
                            //}

                        }
                    }
                });
            }
        };
        timer.schedule(task, 0, 50);
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    private void checkBlueTooth(){

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_SHORT).show();
        }
        else{
            if(!mBluetoothAdapter.isEnabled()){
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
                Toast.makeText(getApplicationContext(),"Bluetooth Turned ON",Toast.LENGTH_SHORT).show();
            }
            else if (mChatService == null) {
                setupChat();
            }
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble no supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        if( !mBluetoothAdapter.isMultipleAdvertisementSupported() ) {
            Toast.makeText( this, "Multiple advertisement not supported", Toast.LENGTH_SHORT ).show();
            mAdvertiseButton.setEnabled( false );
            mAdvertiseButton.setText(R.string.advertise3);
        }
        /*
        if (!mBluetoothAdapter.isLe2MPhySupported()) {
            Log.e("check", "2M PHY not supported!");

        }
        if (!mBluetoothAdapter.isLeExtendedAdvertisingSupported()) {
            Log.e("check", "LE Extended Advertising not supported!");

        }

        int maxDataLength = mBluetoothAdapter.getLeMaximumAdvertisingDataLength();
        */

    }

    private void connectDevice() {
        String address = mDeviceAddress;
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mConversationArrayAdapter.clear();
        if (mBluetoothLeService != null && address !=null) {
            final boolean result = mBluetoothLeService.connect(address);
            Toast.makeText(MainActivity.this, "Connect request result=" + result, Toast.LENGTH_SHORT).show();
            Log.d("connectDevice", "Connect request result=" + result);
            if (result){
                mConnectionState.setText(R.string.connected);
                tx_power =0;
            }
            else{
                mConnectionState.setText(R.string.disconnected);
            }
        }
        else{
            Toast.makeText(MainActivity.this, "Ble no initialize!!!", Toast.LENGTH_SHORT).show();
        }
        mChatService.connect(device, true);
        if (mChatService.getState() == 3){
            mConnectionState.setText(R.string.connected);
        }
        else{
            mConnectionState.setText(R.string.disconnected);
        }
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        mOutString = message;
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
            mSendButton.setEnabled(false);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);

        }
        mSendButton.setEnabled(true);
    }


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage( Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            mConnectionState.setText(R.string.connected);
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            mConnectionState.setText(R.string.connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            mConnectionState.setText(R.string.disconnected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);

                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                    break;
                case Constants.MESSAGE_TOAST:

                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();

                    break;
            }
        }
    };


    private void setupChat() {
        android.util.Log.d("SetupChat", "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the send button with a listener that for click events
        mSendButton.setEnabled(true);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

                Log.i("sendButton", "1");
                TextView textView = findViewById(R.id.edit_text_out);
                String message = textView.getText().toString();
                if(!message.equals("")){
                    sendMessage(message);
                }

            }
        });


        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getApplicationContext(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer();
    }

    private void clearUI() {

        mDataField.setText(R.string.no_data);
    }

    private DialogInterface.OnClickListener builder1click =
            new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Log.i("DISCOVERY_FINISHED", "5");
            BluetoothDevice device = Devices.get(which);
            ///*
            String address = device.getAddress();
            Log.i("DISCOVERY_FINISHED", "6");
            String name;
            if (device.getName() == null){
                name = "null";}
            else{
                name = device.getName();
            }
            Log.i("DISCOVERY_FINISHED", "7");
            Address.setText(address);
            DeviceName.setText(name);
            Short rssi = Rssis.get(which);
            mDataField.setText(rssi);
            mDeviceRssi = rssi.intValue();
            Log.i("DISCOVERY_FINISHED", "8");
            float dis = (float) Math.pow(10, (tx_power - mDeviceRssi) / (10 * 2.5));
            String distanceS = new DecimalFormat("0.00").format(dis) + "m";
            Log.i("DISCOVERY_FINISHED", "9");
            Distance.setText(distanceS);
            mDeviceAddress =address;
            Log.i("DISCOVERY_FINISHED", "16");

        }
    };

    public void readRssiBle (){
        if (mBluetoothLeService.rssiStatus){
            int rssi = mBluetoothLeService.mrssi;
            mDeviceRssi = rssi;
            float dis = (float) Math.pow(10, (tx_power - mDeviceRssi) / (10 * 2.5));
            String distanceS = new DecimalFormat("0.00000000").format(dis) + "m";
            //float dis2 = (float) (-0.00903 * (rssi)*rssi - 2.171*rssi - 94);
            String distanceS2 = new DecimalFormat("0").format(rssi) + "db";
            mDataField.setText(distanceS2);
            Distance.setText(distanceS);
            mBluetoothLeService.rssiStatus = false;
            Toast.makeText(MainActivity.this, "rssi updated to " + distanceS, Toast.LENGTH_SHORT).show();
        }
        else{

            Toast.makeText(MainActivity.this, "requesting rssi... wait to get rssi", Toast.LENGTH_SHORT).show();

        }
    }

    SensorEventListener SensorListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            sensorAccuracy = accuracy;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if(sensorAccuracy != event.accuracy ){
                sensorAccuracy = event.accuracy;
            }

            if (isAccel  && isMag ) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, valuesAccel, valuesMag);
                if (success) {


                    SensorManager.getOrientation(R, orientation);
                    for (int i=0; i<3; i++){
                        orientation[i] = orientation[i]/(float)Math.PI * 180;
                        if (Math.abs(last_orientation[i]-orientation[i])>15){
                            System.arraycopy(orientation, 0, last_orientation,0, orientation.length);
                            isRotate = true;
                            //showInfo();
                        }
                    }

                }
            }
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    for (int i = 0; i < 3; i++) {
                        valuesAccel[i] = event.values[i];
                        isAccel = true;
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    for (int i = 0; i < 3; i++) {
                        valuesGyro[i] = event.values[i];
                        isGyro = true;
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    for (int i = 0; i < 3; i++) {
                        valuesMag[i] = event.values[i];
                        isMag = true;
                    }
                    break;
            }
        }
    };

    String format(float[] values) {
        return String.format("X: %2$.8f\tY: %3$.8f\tZ: %4$.8f", sensTime, values[0], values[1],
                values[2]);
    }
    private void blescan(){
        stopScanning();
        if (mScanCallback == null) {
            Log.d("blescan", "Starting Scanning");
            // Will stop the scanning after a set time.
            mHandler1.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, TIMEOUT);

            mScanCallback = getmScanCallback();
            List<ScanFilter> filters = new ArrayList<>();
            ScanFilter.Builder filter = new ScanFilter.Builder();
            filter.setServiceUuid( new ParcelUuid(UUID_SERVER ) );
            filters.add(filter.build());

            ScanSettings.Builder settings = new ScanSettings.Builder();
            settings.setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY );

            mBluetoothLeScanner.startScan(filters, settings.build(), mScanCallback);

            String toastText = "scan Started!!" + " "
                    + TimeUnit.SECONDS.convert(TIMEOUT, TimeUnit.MILLISECONDS) + " "
                    + "sec";
            Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_LONG).show();
            mDiscoverButton.setText(R.string.scan5);
        } else {
            mDiscoverButton.setText(R.string.scan1);
            Toast.makeText(MainActivity.this, "already scanning", Toast.LENGTH_SHORT).show();
        }
    }

    private void showText(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConversationArrayAdapter.add( msg);
                mConversationArrayAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showInfo() {
        String info = "";
        info += "sensTime: " + sensTime +" accuracy: "+ sensorAccuracy + " isRotate: "+ isRotate +"\n";
        info += "valuesAccel: " + format(valuesAccel)+"\n";
        info += "valuesGyro: " + format(valuesGyro)+"\n";
        info += "valuesMag: " + format(valuesMag)+"\n";
        info += "orientation: " + format(orientation);

        isRotate = false;
        showText(info);
    }

    private void stopAdvertising() {

        Log.d("stopAdvertising", "Service: Stopping Advertising");
        if (mbluetoothLeAdvertiser != null && mAdvertiseCallback != null) {
            mbluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertiseCallback = null;
        }
        mAdvertiseButton.setText(R.string.advertise);
    }

    private void setTimeout(){
        Handler mHandler2 = new Handler();
        Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("time_out", "AdvertiserService has reached timeout of "+TIMEOUT+" milliseconds, stopping advertising.");
                stopAdvertising();
            }
        };
        mHandler2.postDelayed(timeoutRunnable, TIMEOUT);
        mAdvertiseButton.setText(R.string.advertise);
    }

    private AdvertiseCallback getmAdvertiseCallback(){
        AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
            String TAG = "initGATT";

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "BLE advertisement ");
                mAdvertiseButton.setText(R.string.advertise4);
                Toast.makeText(MainActivity.this, "advertise success", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "Failed to add BLE advertisement, reason: " + errorCode);
                mAdvertiseButton.setText(R.string.advertise2);
                Toast.makeText(MainActivity.this, "advertise failure, reason: " + errorCode, Toast.LENGTH_SHORT).show();
            }
        };
        return mAdvertiseCallback;
    }
	
    public void construtHeadPacket(){
        //Head packet for device information:
        //  include device name, tx_power, uuid, device_address
        byte[] initial_packet = new byte[31];
        BYTES_IN_CONTINUE_PACKET = 31 - 16 - mBluetoothAdapter.getName().length();
        initial_packet[0] = (byte) -1; // indicate head packet
        
		String myName =  "Jason Yao";
		showText("headPacket: myName-" + myName + " deviceName-" + mBluetoothAdapter.getName());
		sendingContinuePacket(initial_packet, myName);
		
        return;
    }

    private void sendByAdvertise(String outString) {

        byte[] initial_packet = new byte[3];
        initial_packet[0] = (byte) messageCount;
        sendingContinuePacket(initial_packet, outString);
    }

    private void sendingContinuePacket(byte[] initial_packet, String CHARACTERS){

        int INITIAL_MESSAGE_PACKET_LENGTH = initial_packet.length;
		BYTES_IN_CONTINUE_PACKET = 31 - 16 - mBluetoothAdapter.getName().length();
        String TAG = "sendingContinuePacket";
        // Check the data length is large how many times with Default Data (BLE)
        int times = CHARACTERS.length() / BYTES_IN_CONTINUE_PACKET;

        Log.i(TAG, "CHARACTERS.length() " + CHARACTERS.length());
        Log.i(TAG, "times " + times);

        byte[] sending_continue_hex = new byte[BYTES_IN_CONTINUE_PACKET];
        for (int time = 0; time <= times; time++) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (time == times) {
                Log.i(TAG, "LAST PACKET ");

                int character_length = CHARACTERS.length()
                        - BYTES_IN_CONTINUE_PACKET*times;

                initial_packet[1] = (byte) time;
                initial_packet[2] = (byte) times;

                Log.i(TAG, "character_length " + character_length);
                byte[] sending_last_hex = new byte[character_length];
                for (int i = 0; i < sending_last_hex.length; i++) {
                    sending_last_hex[i] =
                            CHARACTERS.getBytes(mCharSet)[sending_continue_hex.length*time + i];
                }

                byte[] last_packet =
                        new byte[character_length + INITIAL_MESSAGE_PACKET_LENGTH];
                System.arraycopy(initial_packet, 0, last_packet,
                        0, initial_packet.length);
                System.arraycopy(sending_last_hex, 0, last_packet,
                        initial_packet.length, sending_last_hex.length);

                if (initial_packet[0] == (byte) -1){
					sendHeadPacket(last_packet);
					}
				else{
					sendOnePacket(last_packet);
				}
					
            } else {
                Log.i(TAG, "CONTINUE PACKET ");

                int character_length = sending_continue_hex.length;
                initial_packet[1] = (byte) time;
                initial_packet[2] = (byte) times;

                for (int i = 0; i < sending_continue_hex.length; i++) {
                    Log.i(TAG, "Send stt : "
                            + (sending_continue_hex.length*time + i));
                    sending_continue_hex[i] =
                            CHARACTERS.getBytes()[sending_continue_hex.length*time + i];
                }
                byte[] sending_continue_packet =
                        new byte[character_length + INITIAL_MESSAGE_PACKET_LENGTH];
                System.arraycopy(initial_packet, 0, sending_continue_packet,
                        0, initial_packet.length);
                System.arraycopy(sending_continue_hex, 0, sending_continue_packet,
                        initial_packet.length, sending_continue_hex.length);
                if (initial_packet[0] == (byte) -1){
					sendHeadPacket(sending_continue_packet);
					}
				else{
					sendOnePacket(sending_continue_packet);
				}
            }
        }
    }

    public void sendOnePacket(byte[] data){
        AdvertiseSettings.Builder settings = new AdvertiseSettings.Builder();

        settings.setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY );
        settings.setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH );
        settings.setConnectable(true);

        final AdvertiseData.Builder advertiseData = new AdvertiseData.Builder();
        advertiseData.setIncludeDeviceName(true);
        advertiseData.setIncludeTxPowerLevel(false);

        advertiseData.addServiceUuid(new ParcelUuid(UUID_SERVER));
        advertiseData.addServiceData(new ParcelUuid(UUID_SERVER), data );

        mAdvertiseCallback = getmAdvertiseCallback();
        mbluetoothLeAdvertiser.startAdvertising(settings.build(), advertiseData.build(), mAdvertiseCallback);
        mAdvertiseButton.setText(R.string.advertise1);
        setTimeout();
    }



    public void sendHeadPacket(byte[] data){

        AdvertiseSettings.Builder settings = new AdvertiseSettings.Builder();

        settings.setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY );
        settings.setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH );
        settings.setConnectable(true);

        final AdvertiseData.Builder advertiseData = new AdvertiseData.Builder();
        advertiseData.setIncludeDeviceName(true);
        advertiseData.setIncludeTxPowerLevel(true);

        advertiseData.addServiceUuid(new ParcelUuid(UUID_SERVER));
        advertiseData.addServiceData(new ParcelUuid(UUID_SERVER), data );
		String outString = new String(data, mCharSet);
        mAdvertiseCallback = getmAdvertiseCallback();
        mbluetoothLeAdvertiser.startAdvertising(settings.build(), advertiseData.build(), mAdvertiseCallback);
        mAdvertiseButton.setText(R.string.advertise1);
        setTimeout();
    }



    public void packetDecode(byte[] packet, Map<Integer, ArrayList<String>> theMessageMap){
        int messageId = packet[0] & 0xFF;
        int index = packet[1] & 0xFF;
        int indexes = packet[2] & 0xFF;
        if (theMessageMap == null){
            showText("asdsadasd");
            return;
        }

        byte[] dataPacket = new byte[packet.length - 3];
        System.arraycopy(packet, 3, dataPacket, 0, packet.length-3);
        String data = new String(dataPacket, mCharSet);

        if (theMessageMap.get(messageId) == null) {
            ArrayList<String> kk = new ArrayList<>();
            for (int i = 0; i < indexes + 2; i++) {
                kk.add("");
                System.out.println(i);
            }
            kk.set(index, data);
            int num_index = 1;
            kk.set(indexes+1, String.valueOf(num_index));
            theMessageMap.put(messageId, kk);
        } 
        else {
            ArrayList<String> kk = theMessageMap.get(messageId);
            int num_index = Integer.valueOf(kk.get(indexes+1));
            if (num_index == indexes+1) {
                StringBuilder InString = new StringBuilder("");
                String mInString = "";
                for (int i = 0; i < indexes+1; i++) {
                    InString.append(kk.get(i));
                }
                mInString = InString.toString();
                showText(DeviceName.getText().toString() + ": " + mInString);
                kk.set(indexes+1, String.valueOf(num_index + 1));
            }
            else if(!kk.get(index).equals(data) || kk.get(index).equals("")){
                kk.set(index, data);
                kk.set(indexes+1, String.valueOf(num_index + 1));
                theMessageMap.put(messageId, kk);
            }
        }
    }

    public ScanCallback getmScanCallback() {
        ScanCallback callback = new ScanCallback() {

            @Override
            public void onScanResult(int callbackType, ScanResult result) {

                super.onScanResult(callbackType, result);
                if (result == null
                        || result.getDevice() == null
                        || TextUtils.isEmpty(result.getDevice().getName())){

                    return;}
                BluetoothDevice device = result.getDevice();
                ///*
                String address = device.getAddress();
                if (!Addresses.contains(address)){
                    Addresses.add(address);
                    //showText("address: " + address);
                }

                String name;
                if (device.getName() == null){
                    name = result.getScanRecord().getDeviceName();}
                else{
                    name = device.getName();
                }

                mDiscoverButton.setText(R.string.scan3);
                if (!DeviceNames.contains(name)) {
                    DeviceNames.add(name);
                    //showText(name + ":  " + result.toString());
                }
                DeviceName.setText(name);
                String key = name;
                if (deviceMap.get(key) == null) {
                    Map<Integer, ArrayList<String>> theMessageMap = new HashMap<>();
                    deviceMap.put(key, theMessageMap);
                }

                if (mDeviceRssi==999){
                    mDeviceRssi =  result.getRssi();
                }
                else{
                    mDeviceRssi = (mDeviceRssi + result.getRssi())/2;
                }
                mDataField.setText(String.valueOf(mDeviceRssi));

                float dis = (float) Math.pow(10, (tx_power - mDeviceRssi) / (10 * 2.5));
                String distanceS = new DecimalFormat("0.00").format(dis) + "m";
                Distance.setText(distanceS);

                if (!Addresses.contains(address)){
                    Address.setText(address);
                    Addresses.add(address);
                }

                byte[] packet = result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0));
                if (packet != null){

                    packetDecode(packet, deviceMap.get(key));
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {

                super.onBatchScanResults(results);
                mDiscoverButton.setText(R.string.scan2);
                for (ScanResult result : results) {
                    StringBuilder builder = new StringBuilder(result.getDevice().getName());

                    builder.append("\n").append(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));
                    //showText(result.getDevice().getName() + ":  " + result.toString());
                    Toast.makeText(MainActivity.this, "Batch Scan: " + result.toString() + " " + builder.toString(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("BLE", "Discovery onScanFailed: " + errorCode);

                mBluetoothLeScanner.stopScan(mScanCallback);
                mDiscoverButton.setText(R.string.scan4);
                Toast.makeText(MainActivity.this, "Discovery onScanFailed errorCode: " + errorCode, Toast.LENGTH_SHORT).show();

                super.onScanFailed(errorCode);
            }
        };
        return callback;
    }

    public void stopScanning() {
        Log.d("stop scanning", "Stopping Scanning");
        // Stop the scan, wipe the callback.
        mDiscoverButton.setText(R.string.scan1);
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
        mConversationArrayAdapter.notifyDataSetChanged();

    }
}
