package com.example.bluchat;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import java.util.ArrayList;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

    // Constants
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_OBJECT = "device_name";
    private static final String CONNECTING = "Connecting...";
    private static final String CONNECTED_TO = "Connected to ";
    private static final String NOT_CONNECTED = "Not connected!";
    private static final String INPUT_TEXT_REQUEST = "Please enter some text!";
    private static final String DIALOG_TITLE = "Bluetooth Devices";
    private static final String NO_DEVICES_PAIRED = "No device have been paired!";
    private static final String TURN_OFF = "Bluetooth still disabled, turn off application!";
    private static final String CONNECTION_LOST = "Connection was lost!";
    private static final String NO_DEVICE_FOUND = "No device found!";
    private static final String BLUETOOTH_UNAVAILABLE = "Bluetooth is not available!";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int ANIMATION_TIME = 1000;


    // Member Variables
    private String authorName = "Me";
    private ImageView animation;
    private YoYo.AnimationComposer fadeOut;
    private YoYo.AnimationComposer bounce;
    private TextView status;
    private ListView chatList;
    private ImageButton btnSend;
    private Dialog dialog;
    private EditText messageInput;
    private ChatListAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;
    private BluetoothAdapter bluetoothAdapter;
    private ChatMessage chatMessage;
    private ChatSwitch chatSwitch;
    private BluetoothDevice connectingDevice;
    private ArrayAdapter<String> discoveredDevicesAdapter;

    // Methods
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(R.id.btn_connect == item.getItemId()) {
            showPrinterPickDialog();
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Customize ActionBar
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.actionbarlayout);

        // Cool Animations
        fadeOut = YoYo.with(Techniques.FadeOut)
                .duration(ANIMATION_TIME)
                .repeat(YoYo.INFINITE);

        bounce = YoYo.with(Techniques.Bounce)
                .duration(ANIMATION_TIME)
                .repeat(YoYo.INFINITE);

        // Linking layout to java code
        animation = findViewById(R.id.animationIV);
        status = (TextView) findViewById(R.id.status);
        chatList = (ListView) findViewById(R.id.list);
        messageInput = (EditText) findViewById(R.id.input_layout);
        messageInput.setVisibility(View.INVISIBLE);
        btnSend = (ImageButton) findViewById(R.id.btn_send);
        btnSend.setVisibility(View.INVISIBLE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        chatMessages = new ArrayList<>();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (messageInput.getText().toString().equals("")) {
                    Toast.makeText(ChatActivity.this, INPUT_TEXT_REQUEST, Toast.LENGTH_SHORT).show();
                } else {
                    sendMessage(messageInput.getText().toString());
                    messageInput.setText("");
                }
            }
        });

        if (bluetoothAdapter == null) {
            Toast.makeText(this, BLUETOOTH_UNAVAILABLE, Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatSwitch.STATE_CONNECTED:
                            fadeOut.playOn(animation).stop();
                            bounce.playOn(animation).stop();
                            getSupportActionBar().hide();
                            animation.setVisibility(View.INVISIBLE);
                            status.setVisibility(View.INVISIBLE);
                            messageInput.setVisibility(View.VISIBLE);
                            btnSend.setVisibility(View.VISIBLE);
                            break;
                        case ChatSwitch.STATE_CONNECTING:
                            fadeOut.playOn(animation).stop();
                            bounce.playOn(animation);
                            setStatus(CONNECTING);
                            break;
                        case ChatSwitch.STATE_LISTEN:
                        case ChatSwitch.STATE_NONE:
                            bounce.playOn(animation).stop();
                            fadeOut.playOn(animation);
                            setStatus(NOT_CONNECTED);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    authorName = "Me";
                    chatMessage = new ChatMessage(authorName, writeMessage);
                    chatMessages.add(chatMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    authorName = connectingDevice.getName();
                    chatMessage = new ChatMessage(authorName, readMessage);
                    chatMessages.add(chatMessage);
                    chatAdapter.notifyDataSetChanged();
                    break;
                case MESSAGE_DEVICE_OBJECT:
                    connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), CONNECTED_TO + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void showPrinterPickDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.devicelistview);
        dialog.setTitle(DIALOG_TITLE);

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        ListView listView = (ListView) dialog.findViewById(R.id.pairedDeviceList);
        ListView listView2 = (ListView) dialog.findViewById(R.id.discoveredDeviceList);
        listView.setAdapter(pairedDevicesAdapter);
        listView2.setAdapter(discoveredDevicesAdapter);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryFinishReceiver, filter);


        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryFinishReceiver, filter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesAdapter.add(NO_DEVICES_PAIRED);
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }

        });

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                connectToDevice(address);
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void setStatus(String s) {
        status.setText(s);
    }

    private void connectToDevice(String deviceAddress) {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        chatSwitch.connect(device);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_OK) {
                    chatSwitch = new ChatSwitch(this, handler);
                } else {
                    Toast.makeText(this, TURN_OFF, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void sendMessage(String message) {
        if (chatSwitch.getState() != ChatSwitch.STATE_CONNECTED) {
            Toast.makeText(this, CONNECTION_LOST, Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatSwitch.write(send);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            chatAdapter = new ChatListAdapter(this, authorName, chatMessages);
            chatList.setAdapter(chatAdapter);
            chatSwitch = new ChatSwitch(this, handler);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (chatSwitch != null) {
            if (chatSwitch.getState() == ChatSwitch.STATE_NONE) {
                chatSwitch.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatSwitch != null)
            chatSwitch.stop();
    }

    private final BroadcastReceiver discoveryFinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    discoveredDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (discoveredDevicesAdapter.getCount() == 0) {
                    discoveredDevicesAdapter.add(NO_DEVICE_FOUND);
                }
            }
        }
    };
}