package com.monitor.traffic.demyati.seddik.trafficmonitor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    TextView info;
    Bundle bundle;
    Intent connect;
    Set<BluetoothDevice> PairedDevices;
    Button Connect,pair;
    ToggleButton toggleButton;
    ArrayList<BluetoothDevice> DevicesList;
    ListView listView;
    ArrayAdapter<String> ListAdapter;
    IntentFilter intentFilter;
    BroadcastReceiver broadcastReceiver;
    static List<BroadcastReceiver> receivers;
    static public int BTstatus=0;
    static public String DeviceName=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //runtime permissions for android 6 and above
        int PermissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        PermissionCheck += ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        PermissionCheck += ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA);
        PermissionCheck += ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(PermissionCheck==PackageManager.PERMISSION_DENIED)
            PermissionRequest();
        init();

        //create listener event to make action when item from listview is clicked
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                info.setText("Selected: "+listView.getItemAtPosition(position).toString());
                String[] paired=listView.getItemAtPosition(position).toString().split("~");
                DeviceName=paired[0];
                //what is the pairing state
                if(paired[1].equals("Paired"))
                    pair.setText("UNPAIR");
                else if(paired[1].equals("Unpaired"))
                    pair.setText("PAIR");
                Connect.setEnabled(true);
                pair.setEnabled(true);
            }
        });
    }

    public void init() {



        //Process root = Runtime.getRuntime().exec("su");
        bundle=new Bundle();
        connect = new Intent(MainActivity.this, ConnectActivity.class);
        pair=(Button)findViewById(R.id.Pair);
        Connect=(Button)findViewById(R.id.Connect);
        DevicesList= new ArrayList<>();
        info=(TextView)findViewById(R.id.info);
        toggleButton=(ToggleButton) findViewById(R.id.Discovery);
        listView=(ListView)findViewById(R.id.ListView);
        intentFilter= new IntentFilter();
        receivers = new ArrayList<BroadcastReceiver>();
        ListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        listView.setAdapter(ListAdapter);
        //check if mobile device has bluetooth chip
        if(bluetoothAdapter==null){
            Toast.makeText(this,"No bluetooth device",Toast.LENGTH_LONG).show();
            AlertDialog.Builder alertdialog=new AlertDialog.Builder(this);
            alertdialog.setMessage("No bluetooth device was detected")
                    .setTitle("Sorry!!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
        else
            EnableBluetooth();





    }

    public void PermissionRequest(){
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA},
                1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(MainActivity.this,"Permission Denied !!",Toast.LENGTH_LONG).show();
                    AlertDialog.Builder alertdialog=new AlertDialog.Builder(this);
                    alertdialog.setMessage("App won't work without this permission")
                            .setTitle("Are you sure?")
                            .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PermissionRequest();
                                }
                            })
                            .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setCancelable(false)
                            .show();
                }
            }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void EnableBluetooth(){
        if(!bluetoothAdapter.isEnabled()) {
            //ask to enable bluetooth
            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //set the dialog to give a bluetooth enable result back
            startActivityForResult(intent,1);
        }
    }

    //receive the bluetooth enable result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode==RESULT_CANCELED&&requestCode==1){
            Toast.makeText(MainActivity.this,"Bluetooth must be enabled",Toast.LENGTH_LONG).show();
            AlertDialog.Builder alertdialog=new AlertDialog.Builder(this);
            alertdialog.setMessage("App won't work without bluetooth")
                    .setTitle("Are you sure?")
                    .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EnableBluetooth();
                        }
                    })
                    .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
        else if(resultCode==RESULT_OK&&requestCode==1)
            BTstatus=0;
    }

    public void GetDevicesList(){
        //get all previously paired devices
        PairedDevices = bluetoothAdapter.getBondedDevices();
        //if there is data in ListView delete it
        DevicesList.clear();
        ListAdapter.clear();
        //filter actions that we want to listen
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //give it the highest priority
        intentFilter.setPriority(1000);
        //listen to actions and act when they happens
        broadcastReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action= intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice bluetoothDevice=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String PairedStatus=isPairedDevice(bluetoothDevice);
                    //check if device is already in list dont added
                    if(!isItemexist(DevicesList,bluetoothDevice))
                    {
                        //if bluetooth discovered a device added to list
                        DevicesList.add(bluetoothDevice);
                        ListAdapter.add(bluetoothDevice.getName()+PairedStatus);
                        info.setText("Bluetooth Devices: ");
                    }
                }
                //if user turned off bluetooth assk to enable
                else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    if (state==BluetoothAdapter.STATE_TURNING_OFF&&BTstatus==0) {
                        BTstatus=1;
                        EnableBluetooth();
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                //if user paired to device reload list view
                else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    final int state=intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    final int prevState=intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                        toggleButton.setChecked(false);
                        Toggled(findViewById(android.R.id.content));
                        toggleButton.setChecked(true);
                        Toggled(findViewById(android.R.id.content));
                    }
                    //if user unpaired to device reload list view
                    else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                        toggleButton.setChecked(false);
                        Toggled(findViewById(android.R.id.content));
                        toggleButton.setChecked(true);
                        Toggled(findViewById(android.R.id.content));
                    }
                }
            }
        };
    }

    //check if receive is registered in system
    public boolean isReceiverRegistered(BroadcastReceiver receiver){
        boolean registered = receivers.contains(receiver);
        return registered;
    }

    //check if device exists in PairedDevices lists
    public String isPairedDevice(BluetoothDevice bluetoothDevice){
        boolean isPaired=PairedDevices.contains(bluetoothDevice);
        if(isPaired)
            return "~Paired";
        else
            return "~Unpaired";
    }

    public boolean isItemexist(ArrayList arrayList, BluetoothDevice bluetoothDevice){
        boolean existed = arrayList.contains(bluetoothDevice);
        return existed;
    }

    //Discovery ON/OFF
    public void Toggled(View view) {
        if(toggleButton.isChecked()) {
            EnableBluetooth();
            if (bluetoothAdapter.isDiscovering()) {
                // Bluetooth is already in mode discovery mode, we cancel to restart it again
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
            GetDevicesList();
            if (!isReceiverRegistered(broadcastReceiver)){
                registerReceiver(broadcastReceiver,intentFilter);
                receivers.add(broadcastReceiver);
            }
            Toast.makeText(MainActivity.this,"Discovery Darin Resources !!",Toast.LENGTH_LONG).show();
        }
        else {
            if (bluetoothAdapter.isDiscovering()) {
                // Bluetooth is already in discovery mode, we cancel to restart it again
                bluetoothAdapter.cancelDiscovery();
                Connect.setEnabled(false);
                pair.setEnabled(false);
            }
            info.setText("No Bluetooth Devices");
            DeviceName=null;
            pair.setText("PAIR");
            DevicesList.clear();
            ListAdapter.clear();
        }
    }

    //Start dealing with selected device
    public void Connect(View view) {
        if(DeviceName!=null&&pair.getText().toString().equals("UNPAIR")) {
            for(BluetoothDevice device:DevicesList)
                if(device.getName().equals(DeviceName)&&device.getName()!=null){
                    bundle.putParcelable("SentDevice",device);
                    connect.putExtras(bundle);
                }
            startActivity(connect);
        }
        else
            Toast.makeText(MainActivity.this,"Pair to a device first !!",Toast.LENGTH_LONG).show();
    }

    public void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this,"Something went wrong +" +e,Toast.LENGTH_LONG).show();
        }
    }

    public void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this,"Something went wrong",Toast.LENGTH_LONG).show();
        }
    }

    public void Pair(View view) {
        if(DeviceName!=null) {
            if(pair.getText().equals("PAIR")){
                for(BluetoothDevice device:DevicesList)
                    if(device.getName().equals(DeviceName)){
                        pairDevice(device);
                    }
            }
            else if(pair.getText().equals("UNPAIR")){
                for(BluetoothDevice device:DevicesList)
                    if(device.getName().equals(DeviceName)) {
                        unpairDevice(device);
                    }
            }
        }
        else
            Toast.makeText(MainActivity.this,"No device was selected !!",Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bluetoothAdapter.isDiscovering())
            // Bluetooth Discovery drain resources so must be cancelled
            bluetoothAdapter.cancelDiscovery();
        toggleButton.setChecked(false);
        Toggled(findViewById(android.R.id.content));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothAdapter.isDiscovering()) {
            // Bluetooth is already in mode discovery mode, we cancel to restart it again
            bluetoothAdapter.cancelDiscovery();
        }
        toggleButton.setChecked(false);
        Toggled(findViewById(android.R.id.content));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isReceiverRegistered(broadcastReceiver)) {
            receivers.remove(broadcastReceiver);
            MainActivity.this.unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter.isDiscovering())
            // Bluetooth Discovery drain resources so must be cancelled
            bluetoothAdapter.cancelDiscovery();
    }
}
