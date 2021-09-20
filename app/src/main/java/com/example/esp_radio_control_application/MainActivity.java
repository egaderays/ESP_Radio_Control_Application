
package com.example.esp_radio_control_application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;



public class MainActivity extends AppCompatActivity {
   // protected void onCreate() {

        private static final int REQ_ENABLE_BLUETOOTH = 1001;
        public final String TAG = getClass().getSimpleName();
        private Button On_Button; // 1
        private Button Off_Button;// 2
        private Button Increase_volume_Button; // 3
        private Button Next_preset_Button; // 4
        private Button Previous_preset_Button; // 5
        private Button Decrease_volume_Button; // 6

        private boolean isEnabledOn_Button = false;
        private boolean isEnabledOff_Button = false;

        private BluetoothAdapter mBluetoothAdapter;
        private ProgressDialog mProgressDialog;
        private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();//Final_Searching Device
        private Devices_List_Adapter mDeviceListAdapter;

        private BluetoothSocket mBluetoothSocket;
        private OutputStream mOutputStream;

        private ListView listDevices;

        private int isEnabledIncrease_volume_Button = 0;
        private int isEnabledNext_preset_Button = 0;
        private int isEnabledPrevious_preset_Button = 0;
        private int isEnabledDecrease_volume_Button = 0;

        private AdapterView.OnItemClickListener ItemOnCLickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                BluetoothDevice device = mDevices.get(position);
                startConnection(device);

            }
        };


        private void setMessage (String command){
            byte[] buffer = command.getBytes();
            if (mOutputStream != null) {
                try {
                    mOutputStream.write(buffer);
                    mOutputStream.flush();

                } catch (IOException e) {
                    showToastMessage("Command sending error!");
                    e.printStackTrace();
                }
            }
        }

        private void startConnection (BluetoothDevice device){
            if (device != null) {
                try {
                    Method method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    mBluetoothSocket = (BluetoothSocket) method.invoke(device, 1);
                    mBluetoothSocket.connect();
                    mOutputStream = mBluetoothSocket.getOutputStream();
                    showToastMessage("Connection successful!");
                } catch (Exception e) {
                    showToastMessage("Connection error!");
                    e.printStackTrace();
                }

            }
        }

        @Override
        protected void onCreate (Bundle savedInstanceState){
            Log.d(TAG, "onCreate()");
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            On_Button = findViewById(R.id.On_Button);
            Off_Button = findViewById(R.id.Off_Button);
            Increase_volume_Button = findViewById(R.id.Increase_volume_Button);
            Next_preset_Button = findViewById(R.id.Next_preset_Button);
            Previous_preset_Button = findViewById(R.id.Previous_preset_Button);
            Decrease_volume_Button = findViewById(R.id.Decrease_volume_Button);

            On_Button.setOnClickListener(clickListener);
            Off_Button.setOnClickListener(clickListener);
            Increase_volume_Button.setOnClickListener(clickListener);
            Next_preset_Button.setOnClickListener(clickListener);
            Previous_preset_Button.setOnClickListener(clickListener);
            Decrease_volume_Button.setOnClickListener(clickListener);

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Log.d(TAG, "onCreate: The device does not support Bluetooth technology");
                finish();

            }

            mDeviceListAdapter = new Devices_List_Adapter(this, R.layout.device_item, mDevices);
            enableBluetooth();
        }

        private void enableBluetooth () {
            Log.d(TAG, "enableBluetooth: EnableBluetooth()");
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d(TAG, "enableBluetooth: Bluetooth off, turn on bluetooth?");
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQ_ENABLE_BLUETOOTH);
            }
        }

        private void showToastMessage (String message){
            //Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onActivityResult ( int requestCode, int resultCode, @Nullable Intent data){
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQ_ENABLE_BLUETOOTH) {
                if (mBluetoothAdapter.isEnabled()) {
                    Log.d(TAG, "onActivityResult: Trying to turn on Bluetooth again...");
                    enableBluetooth();

                }
            }
        }

        @Override
        public boolean onCreateOptionsMenu (Menu menu){
            getMenuInflater().inflate(R.menu.main_program_menu, menu);
            return super.onCreateOptionsMenu(menu);
        }

        @Override
        public boolean onOptionsItemSelected (@NonNull MenuItem item){
            switch (item.getItemId()) {
                case R.id.item_search:
                    searchDevices(); //searchUsersDevice
                    break;

                case R.id.Exit:
                    finish();
                    break;
            }

            return super.onOptionsItemSelected(item);
        }

        private void searchDevices () {
            Log.d(TAG, "searchDevices: ");
            enableBluetooth();
            checkPermissionLocation();
            if (!mBluetoothAdapter.isDiscovering()) {
                Log.d(TAG, "searchUsersDevice: Searching for devices...");
                mBluetoothAdapter.startDiscovery();
            }
            if (mBluetoothAdapter.isDiscovering()) {
                Log.d(TAG, "searchUsersDevice: Search has already been started...restarting device search...");
                mBluetoothAdapter.cancelDiscovery();
                mBluetoothAdapter.startDiscovery();
            }
            IntentFilter SearchStatus = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            SearchStatus.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            SearchStatus.addAction(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, SearchStatus);

        }

        private void showListDevices () {
            Log.d(TAG, "showDevicesList: ");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Found devices:");

            View view = getLayoutInflater().inflate(R.layout.device_view_list, null);
            listDevices = view.findViewById(R.id.list_devices);
            listDevices.setAdapter(mDeviceListAdapter);
            listDevices.setOnItemClickListener(ItemOnCLickListener);

            builder.setView(view);
            builder.setNegativeButton("OK", null);
            builder.create();
            builder.show();
        }

        private void checkPermissionLocation () {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                int check = checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
                check += checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
                if (check != 0) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1002);
                }
            }
        }

        private View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = null;
                if (view.equals(On_Button)) {
                    isEnabledOn_Button = !isEnabledOn_Button;
                    command = "77";
                    Log.d(TAG, "onClick: isEnabledOn_Button =  " + isEnabledOn_Button);

                }
                if (view.equals(Off_Button)) {
                    isEnabledOff_Button = !isEnabledOff_Button;
                    command = "77";
                    Log.d(TAG, "onClick: isEnabledOff_Button =  " + isEnabledOff_Button);

                }
                if (view.equals(Increase_volume_Button)) {
                    isEnabledIncrease_volume_Button = isEnabledIncrease_volume_Button + 1;
                    command = "22";
                    Log.d(TAG, "onClick: isEnabled_Increase_volume_Button =  " + isEnabledIncrease_volume_Button);
                }
                if (view.equals(Next_preset_Button)) {
                    isEnabledNext_preset_Button = isEnabledNext_preset_Button + 1;
                    command = "44";
                    Log.d(TAG, "onClick: isEnabledNext_preset_Button =  " + isEnabledNext_preset_Button);

                }
                if (view.equals(Previous_preset_Button)) {
                    isEnabledPrevious_preset_Button = isEnabledPrevious_preset_Button + 1;
                    command = "55";
                    Log.d(TAG, "onClick: isEnabledPrevious_preset_Button =  " + isEnabledPrevious_preset_Button);

                }
                if (view.equals(Decrease_volume_Button)) {
                    isEnabledDecrease_volume_Button = isEnabledDecrease_volume_Button + 1;
                    command = "33";
                    Log.d(TAG, "onClick: isEnabledDecrease_volume_Button =  " + isEnabledDecrease_volume_Button);

                }
                setMessage(command);
            }

        };

        protected void onDestroy () {
            super.onDestroy();
            try {
                if (mBluetoothSocket != null) {
                    mBluetoothSocket.close();
                }
                if (mOutputStream != null) {
                    mOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                    Log.d(TAG, "onReceive: ACTION_DISCOVERY_STARTED");
                    showToastMessage("Start searching for devices...");
                    mProgressDialog = ProgressDialog.show(MainActivity.this, "Search for devices", "Wait please...");
                }
                if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                    Log.d(TAG, "onReceive: ACTION_DISCOVERY_FINISHED");
                    showToastMessage("The search for devices is complete.");
                    mProgressDialog.dismiss();
                    showListDevices();
                }
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    Log.d(TAG, "onReceive: ACTION_FOUND");
                    //showToastMessage("Start searching for devices...");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        if (!mDevices.contains(device))
                            mDeviceListAdapter.add(device);

                    }
                }
            }
        };
    }
//}
