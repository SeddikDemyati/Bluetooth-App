package com.monitor.traffic.demyati.seddik.trafficmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.provider.Telephony;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import java.util.regex.Pattern;

public class ConnectActivity extends Activity  {

    static String filename;
    EditText sent;
    TextView info;
    Button SendData;
    private Camera camera;
    static List<BroadcastReceiver> receivers;
    BluetoothDevice bluetoothDevice;
    BluetoothAdapter bluetoothAdapter;
    APNHelper APNhelper;
    String MMSnumber,MMStext="";
    Bitmap bitmap;
    BluetoothConnectionService mBluetoothConnection;
    byte[] bytes;
    Bundle bundle;
    private static final String TAG = "BluetoothConnectionServ";
    //The UUID is used for uniquely identifying information.
    // It identifies a particular service provided by a Bluetooth device
    //private static final UUID MY_UUID_INSECURE =
    //UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String[] APN_PROJECTION = {
            Telephony.Carriers.TYPE,
            Telephony.Carriers.MMSC,
            Telephony.Carriers.MMSPROXY,
            Telephony.Carriers.MMSPORT,
            Telephony.Carriers.NAME,
            Telephony.Carriers.APN,
            Telephony.Carriers.BEARER,
            Telephony.Carriers.PROTOCOL,
            Telephony.Carriers.ROAMING_PROTOCOL,
            Telephony.Carriers.AUTH_TYPE,
            Telephony.Carriers.MVNO_TYPE,
            Telephony.Carriers.MVNO_MATCH_DATA,
            Telephony.Carriers.PROXY,
            Telephony.Carriers.PORT,
            Telephony.Carriers.SERVER,
            Telephony.Carriers.USER,
            Telephony.Carriers.PASSWORD,
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Does the device have a Camera
        if (!getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "No camera on this device", Toast.LENGTH_LONG)
                    .show();
        }
        else {
            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
                //initialize Camera object
                camera = Camera.open();
        }
        //By creating this object will start waiting for bluetooth connections
        mBluetoothConnection=new BluetoothConnectionService(this);
        //add observer to observe the observable object
        mBluetoothConnection.addObserver(new UpdateData(ConnectActivity.this));
    }

    public void init(){
        receivers = new ArrayList<BroadcastReceiver>();
        if (!isReceiverRegistered(BitmapReceiver)) {
            LocalBroadcastManager.getInstance(this).registerReceiver(BitmapReceiver,
                    new IntentFilter("IncomingMessages"));
            receivers.add(BitmapReceiver);
        }
        SendData=findViewById(R.id.Senddata);
        SendData.setEnabled(false);
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isDiscovering())
            // Bluetooth Discovery drain resources so must be cancelled
            bluetoothAdapter.cancelDiscovery();
        info=findViewById(R.id.info);
        info.setText("Received data: ");
        sent= findViewById(R.id.sent);
        bundle = this.getIntent().getExtras();
        //receive object
        if (bundle != null) {
            bluetoothDevice = bundle.getParcelable("SentDevice");
        }
    }

    //check if receive is registered in system
    public boolean isReceiverRegistered(BroadcastReceiver receiver){
        boolean registered = receivers.contains(receiver);
        return registered;
    }

    public void Connect(View view) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
        //Start ConnectThread to start connection to other device
        mBluetoothConnection.startClient(bluetoothDevice,MY_UUID_INSECURE);
        SendData.setEnabled(true);
    }

    public void SendBluetooth(View view) {
        bytes=sent.getText().toString().getBytes(Charset.defaultCharset());
        mBluetoothConnection.write(bytes);
    }

    public void Camera(View view) {
        if (camera != null) {
            camera.startPreview();
            camera.takePicture(null, null,
                    new PhotoHandler(getApplicationContext()));
        }
        else
            Toast.makeText(this, "Device Not Supported",Toast.LENGTH_LONG).show();
    }


    public void SendMessage(View view) {
        //if android is KitKat settings will be set by SendMMS19 method
        Settings sendSettings=new Settings();
        //check if mobile system version is kitkat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            sendSettings=SendMMS19();
        SendMMS(this,sendSettings);
    }

    //receive picture path to send
    BroadcastReceiver BitmapReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            filename=intent.getStringExtra("IncomingMessage");
            System.out.println("ConnectActivity file found "+filename);
        }
    };

    public void SendMMS(Context context,Settings sendSettings){
        sendSettings.setUseSystemSending(true);
        //initialize message and send it
        Transaction sendTransaction = new Transaction(context, sendSettings);
        Message mMessage;
        //set number and body
        mMessage = new Message(MMStext, MMSnumber);
        //Attach taken photo to message
        System.out.println("InputStream file found "+filename);
        bitmap = BitmapFactory.decodeFile(filename);
        if (bitmap != null) {
            mMessage.addImage(bitmap);
        }
        else
            Toast.makeText(this,"null bitmap",Toast.LENGTH_LONG).show();
        //send message
        sendTransaction.sendNewMessage(mMessage, Transaction.NO_THREAD_ID);
    }

    public Settings SendMMS19(){
        //get the default APN settings to send MMS
        Settings sendSettings=new Settings();
        APNhelper=new APNHelper(this);
        List<APNHelper.APN> results = APNhelper.getMMSApns();
        for (APNHelper.APN apn:results) {
            sendSettings.setMmsc(apn.MMSCenterUrl);
            sendSettings.setProxy(apn.MMSProxy);
            sendSettings.setPort(apn.MMSPort);
        }
        return sendSettings;
    }

    @Override
    public void onPause() {
        super.onPause();
        // release camera object
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SendData.setEnabled(false);
        if(bitmap!=null)
            bitmap.recycle();
        if(mBluetoothConnection!=null) {
            mBluetoothConnection.CloseSocket();
            mBluetoothConnection = null;
        }
        if (isReceiverRegistered(BitmapReceiver)) {
            receivers.remove(BitmapReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(BitmapReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(mBluetoothConnection!=null) {
            mBluetoothConnection.CloseSocket();
            mBluetoothConnection = null;
        }
        if (isReceiverRegistered(BitmapReceiver)) {
            receivers.remove(BitmapReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(BitmapReceiver);
        }
    }

    //Observer Class
    public class UpdateData implements Observer{
        String IncomingData="";
        Handler handler;
        int indexOfEndData;

        public UpdateData(Context context){
            handler = new Handler(context.getMainLooper());
        }

        public void data(String data) {
            //Appending incoming data
            IncomingData=IncomingData+data;
            indexOfEndData=IncomingData.indexOf("\n");
            System.out.println("Incoming Data: "+IncomingData);
            //Incoming data end with character "+"
            if(IncomingData.contains("+")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String ReceivedData = IncomingData.substring(0,IncomingData.indexOf("+"));
                        System.out.println("Received Data: "+ReceivedData);
                        info.setText("Received data: "+ReceivedData);
                        if(ReceivedData.equals("cam"))
                            Camera(findViewById(android.R.id.content));
                        if(Pattern.matches("[0-9]+",ReceivedData)) {
                            MMSnumber=ReceivedData;
                            SendMessage(findViewById(android.R.id.content));
                        }
                        IncomingData="";
                    }
                });
            }
        }

        //post data to main thread
        private void runOnUiThread(Runnable r) {
            handler.post(r);
        }

        //receive the changed value whenever it is changed
        @Override
        public void update(Observable observable, Object o) {
            data(o.toString());
        }
    }

}